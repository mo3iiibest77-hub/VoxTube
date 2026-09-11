package com.mo3ibest.voxtube.data.youtube

import com.mo3ibest.voxtube.data.model.TranscriptEntry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLDecoder
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the same captions you see with the YouTube CC button,
 * using the phone network (not the blocked server IP).
 */
@Singleton
class YouTubeCaptionFetcher @Inject constructor(
    private val client: OkHttpClient
) {

    fun fetch(videoId: String): List<TranscriptEntry> {
        val tracks = fetchCaptionTracks(videoId)
        if (tracks.isEmpty()) {
            throw IllegalStateException("برای این ویدیو زیرنویس (CC) پیدا نشد")
        }
        // Prefer Persian, then English, then first available
        val preferred = tracks.firstOrNull {
            it.lang.startsWith("fa") || it.lang.contains("persian")
        } ?: tracks.firstOrNull {
            it.lang.startsWith("en")
        } ?: tracks.first()

        val body = httpGet(preferred.url)
            ?: throw IllegalStateException("دانلود زیرنویس ناموفق بود")

        return when {
            preferred.url.contains("fmt=json3") || body.trimStart().startsWith("{") ->
                parseJson3(body)
            body.contains("WEBVTT") || preferred.url.contains("fmt=vtt") ->
                parseVtt(body)
            else ->
                parseTimedTextXml(body)
        }.ifEmpty {
            throw IllegalStateException("زیرنویس خالی بود")
        }
    }

    private data class Track(val lang: String, val url: String, val name: String)

    private fun fetchCaptionTracks(videoId: String): List<Track> {
        // 1) Innertube player (most reliable for track list + baseUrl)
        try {
            val tracks = fetchTracksViaInnertube(videoId)
            if (tracks.isNotEmpty()) return tracks
        } catch (_: Exception) {
        }

        // 2) Classic timedtext list
        try {
            val tracks = fetchTracksViaTimedTextList(videoId)
            if (tracks.isNotEmpty()) return tracks
        } catch (_: Exception) {
        }

        return emptyList()
    }

    private fun fetchTracksViaInnertube(videoId: String): List<Track> {
        val json = """
            {
              "context": {
                "client": {
                  "clientName": "ANDROID",
                  "clientVersion": "19.09.37",
                  "hl": "en",
                  "gl": "US"
                }
              },
              "videoId": "$videoId"
            }
        """.trimIndent()

        val req = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
            .header("User-Agent", ANDROID_UA)
            .header("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string().orEmpty()
            if (body.isEmpty()) return emptyList()
            return parseTracksFromPlayerJson(body)
        }
    }

    private fun parseTracksFromPlayerJson(body: String): List<Track> {
        val root = JSONObject(body)
        val trackList = root
            .optJSONObject("captions")
            ?.optJSONObject("playerCaptionsTracklistRenderer")
            ?.optJSONArray("captionTracks")
            ?: return emptyList()

        val out = mutableListOf<Track>()
        for (i in 0 until trackList.length()) {
            val t = trackList.getJSONObject(i)
            var baseUrl = t.optString("baseUrl")
            if (baseUrl.isEmpty()) continue
            // Prefer json3 for easier parse
            if (!baseUrl.contains("fmt=")) {
                baseUrl += if (baseUrl.contains("?")) "&fmt=json3" else "?fmt=json3"
            } else if (!baseUrl.contains("fmt=json3")) {
                baseUrl = baseUrl.replace(Regex("fmt=[^&]+"), "fmt=json3")
            }
            val lang = t.optString("languageCode", "und")
            val name = t.optJSONObject("name")?.optString("simpleText")
                ?: t.optString("languageCode")
            out += Track(lang = lang, url = baseUrl, name = name)
        }
        return out
    }

    private fun fetchTracksViaTimedTextList(videoId: String): List<Track> {
        val listXml = httpGet("https://www.youtube.com/api/timedtext?type=list&v=$videoId")
            ?: return emptyList()
        val out = mutableListOf<Track>()
        val trackPat = Pattern.compile(
            "<track[^>]*lang_code=\"([^\"]+)\"[^>]*?(?:name=\"([^\"]*)\")?[^>]*?/?>",
            Pattern.CASE_INSENSITIVE
        )
        val m = trackPat.matcher(listXml)
        while (m.find()) {
            val lang = m.group(1) ?: continue
            val name = m.group(2) ?: lang
            val url =
                "https://www.youtube.com/api/timedtext?v=$videoId&lang=$lang&fmt=json3"
            out += Track(lang, url, name)
        }
        return out
    }

    private fun httpGet(url: String): String? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", ANDROID_UA)
            .header("Accept-Language", "en-US,en;q=0.9,fa;q=0.8")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    private fun parseJson3(body: String): List<TranscriptEntry> {
        val root = JSONObject(body)
        val events = root.optJSONArray("events") ?: return emptyList()
        val out = mutableListOf<TranscriptEntry>()
        for (i in 0 until events.length()) {
            val ev = events.getJSONObject(i)
            val segs = ev.optJSONArray("segs") ?: continue
            val text = buildString {
                for (j in 0 until segs.length()) {
                    append(segs.getJSONObject(j).optString("utf8"))
                }
            }.replace("\n", " ").trim()
            if (text.isEmpty()) continue
            val start = ev.optLong("tStartMs", 0L) / 1000.0
            val dur = ev.optLong("dDurationMs", 0L) / 1000.0
            out += TranscriptEntry(text = text, start = start, duration = if (dur > 0) dur else 2.0)
        }
        return out
    }

    private fun parseTimedTextXml(body: String): List<TranscriptEntry> {
        val out = mutableListOf<TranscriptEntry>()
        val p = Pattern.compile(
            "<text start=\"([0-9.]+)\"[^>]*dur=\"([0-9.]+)\"[^>]*>([\\s\\S]*?)</text>",
            Pattern.CASE_INSENSITIVE
        )
        val m = p.matcher(body)
        while (m.find()) {
            val start = m.group(1)?.toDoubleOrNull() ?: continue
            val dur = m.group(2)?.toDoubleOrNull() ?: 2.0
            var text = m.group(3) ?: continue
            text = text.replace(Regex("<[^>]+>"), "")
            text = try {
                URLDecoder.decode(text.replace("+", "%2B"), "UTF-8")
            } catch (_: Exception) {
                text
            }
            text = text.replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"")
                .replace("\n", " ").trim()
            if (text.isNotEmpty()) {
                out += TranscriptEntry(text, start, dur)
            }
        }
        return out
    }

    private fun parseVtt(body: String): List<TranscriptEntry> {
        val out = mutableListOf<TranscriptEntry>()
        val blocks = body.split(Regex("\n\n+"))
        val timeRe = Pattern.compile(
            "(\d{2}):(\d{2}):(\d{2})\.(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})\.(\d{3})"
        )
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            var matcher: java.util.regex.Matcher? = null
            val textLines = mutableListOf<String>()
            for (ln in lines) {
                val m = timeRe.matcher(ln)
                if (m.find()) {
                    matcher = m
                    continue
                }
                if (ln.startsWith("WEBVTT") || ln.startsWith("NOTE") || ln.all { it.isDigit() }) continue
                val clean = ln.replace(Regex("<[^>]+>"), "").trim()
                if (clean.isNotEmpty()) textLines += clean
            }
            val m = matcher ?: continue
            if (textLines.isEmpty()) continue
            val start = m.group(1).toInt() * 3600 + m.group(2).toInt() * 60 +
                m.group(3).toInt() + m.group(4).toInt() / 1000.0
            val end = m.group(5).toInt() * 3600 + m.group(6).toInt() * 60 +
                m.group(7).toInt() + m.group(8).toInt() / 1000.0
            val text = textLines.joinToString(" ")
            if (out.isNotEmpty() && out.last().text == text) continue
            out += TranscriptEntry(text, start, maxOf(0.01, end - start))
        }
        return out
    }

    companion object {
        private const val ANDROID_UA =
            "com.google.android.youtube/19.09.37 (Linux; U; Android 14) gzip"

        fun extractVideoId(urlOrId: String): String? {
            val cleaned = urlOrId.trim()
            val patterns = listOf(
                Pattern.compile("(?:v=)([0-9A-Za-z_-]{11})"),
                Pattern.compile("(?:youtu\\.be/)([0-9A-Za-z_-]{11})"),
                Pattern.compile("(?:embed/)([0-9A-Za-z_-]{11})"),
                Pattern.compile("(?:shorts/)([0-9A-Za-z_-]{11})"),
                Pattern.compile("^([0-9A-Za-z_-]{11})$")
            )
            for (p in patterns) {
                val m = p.matcher(cleaned)
                if (m.find()) return m.group(1)
            }
            return null
        }
    }
}

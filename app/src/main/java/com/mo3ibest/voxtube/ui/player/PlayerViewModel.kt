package com.mo3ibest.voxtube.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mo3ibest.voxtube.data.api.VoxTubeApi
import com.mo3ibest.voxtube.data.model.TTSRequest
import com.mo3ibest.voxtube.data.model.TranscriptEntry
import com.mo3ibest.voxtube.data.youtube.YouTubeCaptionFetcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DubChunk(
    val text: String,
    val startSec: Double,
    val endSec: Double,
    val audioBase64: String?
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val voxTubeApi: VoxTubeApi,
    private val captionFetcher: YouTubeCaptionFetcher
) : ViewModel() {

    private val _dubbingStatus = MutableLiveData<String>()
    val dubbingStatus: LiveData<String> = _dubbingStatus

    private val _chunks = MutableLiveData<List<DubChunk>?>()
    val chunks: LiveData<List<DubChunk>?> = _chunks

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isPreparing = MutableLiveData(false)
    val isPreparing: LiveData<Boolean> = _isPreparing

    companion object {
        /** Test build: only first N windows to protect free-tier Gemini quota */
        private const val MAX_CHUNKS_FOR_TEST = 8
    }

    private fun mergeEntries(
        entries: List<TranscriptEntry>,
        maxWindowSec: Double = 12.0
    ): List<Pair<String, Pair<Double, Double>>> {
        if (entries.isEmpty()) return emptyList()
        val windows = mutableListOf<Pair<String, Pair<Double, Double>>>()
        val buf = StringBuilder()
        var windowStart = entries.first().start
        var windowEnd = entries.first().start + entries.first().duration

        fun flush() {
            val t = buf.toString().trim()
            if (t.isNotEmpty()) {
                windows += t to (windowStart to windowEnd)
            }
            buf.clear()
        }

        for (e in entries) {
            val eEnd = e.start + e.duration
            if (buf.isNotEmpty() && (eEnd - windowStart) > maxWindowSec) {
                flush()
                windowStart = e.start
            }
            if (buf.isEmpty()) windowStart = e.start
            if (buf.isNotEmpty()) buf.append(' ')
            buf.append(e.text.replace('\n', ' ').trim())
            windowEnd = eEnd
        }
        flush()
        return windows
    }

    fun startDubbing(videoUrl: String, voice: String = "Charon") {
        viewModelScope.launch {
            _isPreparing.value = true
            _error.value = null
            _chunks.value = null
            try {
                val videoId = YouTubeCaptionFetcher.extractVideoId(videoUrl)
                    ?: throw IllegalStateException("شناسه ویدیو نامعتبر است")

                _dubbingStatus.value = "دریافت زیرنویس از یوتیوب (گوشی)…"
                val entries = withContext(Dispatchers.IO) {
                    captionFetcher.fetch(videoId)
                }
                if (entries.isEmpty()) {
                    _error.value = "زیرنویس خالی است"
                    return@launch
                }

                var windows = mergeEntries(entries)
                if (windows.size > MAX_CHUNKS_FOR_TEST) {
                    windows = windows.take(MAX_CHUNKS_FOR_TEST)
                    _dubbingStatus.value =
                        "نسخه تست: فقط $MAX_CHUNKS_FOR_TEST بخش اول — سپس TTS…"
                } else {
                    _dubbingStatus.value = "ساخت صدای فارسی برای ${windows.size} بخش…"
                }

                val semaphore = Semaphore(1)
                val results = coroutineScope {
                    windows.mapIndexed { index, (text, range) ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                val truncated = if (text.length > 1200) text.take(1200) else text
                                val resp = voxTubeApi.textToSpeech(
                                    TTSRequest(text = truncated, voice = voice)
                                )
                                val audio =
                                    if (resp.isSuccessful) resp.body()?.audio_base64 else null
                                DubChunk(
                                    text = truncated,
                                    startSec = range.first,
                                    endSec = range.second,
                                    audioBase64 = audio
                                ).also {
                                    _dubbingStatus.postValue("TTS ${index + 1}/${windows.size}…")
                                }
                            }
                        }
                    }.awaitAll()
                }

                val ok = results.count { it.audioBase64 != null }
                if (ok == 0) {
                    _error.value =
                        "هیچ بخش صوتی ساخته نشد — سرور /tts یا GEMINI_API_KEY را چک کن"
                    return@launch
                }

                _chunks.value = results
                _dubbingStatus.value = "آماده — $ok بخش. پخش همگام شروع می‌شود."
            } catch (e: Exception) {
                _error.value = "خطا: ${e.message}"
            } finally {
                _isPreparing.value = false
            }
        }
    }

    fun clearDubbing() {
        _chunks.value = null
        _dubbingStatus.value = ""
        _error.value = null
    }
}

package com.mo3ibest.voxtube.data.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.lang.reflect.Type

interface YouTubeApi {
    @GET("videos")
    suspend fun getVideos(
        @Query("part") part: String = "snippet,statistics",
        @Query("chart") chart: String = "mostPopular",
        @Query("regionCode") regionCode: String = "IR",
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") apiKey: String
    ): Response<YouTubeResponse>

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") apiKey: String
    ): Response<YouTubeResponse>
}

data class YouTubeResponse(
    val items: List<YouTubeItem>
)

data class YouTubeItem(
    @JsonAdapter(YouTubeItemIdDeserializer::class)
    val id: YouTubeItemId,
    val snippet: YouTubeSnippet,
    val statistics: YouTubeStatistics? = null
)

/**
 * videos.list returns id as a plain string.
 * search.list returns id as { kind, videoId }.
 */
data class YouTubeItemId(
    val kind: String = "",
    val videoId: String? = null
) {
    fun resolvedId(): String = videoId?.takeIf { it.isNotBlank() } ?: kind
}

class YouTubeItemIdDeserializer : JsonDeserializer<YouTubeItemId> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): YouTubeItemId {
        return if (json.isJsonPrimitive) {
            // videos.list → "id": "dQw4w9WgXcQ"
            YouTubeItemId(kind = "youtube#video", videoId = json.asString)
        } else {
            val obj = json.asJsonObject
            YouTubeItemId(
                kind = obj.get("kind")?.asString ?: "",
                videoId = obj.get("videoId")?.asString
            )
        }
    }
}

data class YouTubeSnippet(
    val title: String,
    val description: String,
    val channelTitle: String,
    val publishedAt: String,
    val thumbnails: YouTubeThumbnails
)

data class YouTubeThumbnails(
    val medium: YouTubeThumbnail? = null,
    val high: YouTubeThumbnail? = null
)

data class YouTubeThumbnail(
    val url: String
)

data class YouTubeStatistics(
    val viewCount: String? = null
)

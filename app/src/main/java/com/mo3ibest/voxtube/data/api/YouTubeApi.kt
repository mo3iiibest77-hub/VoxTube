package com.mo3ibest.voxtube.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

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
    val id: YouTubeItemId,
    val snippet: YouTubeSnippet,
    val statistics: YouTubeStatistics? = null
)

data class YouTubeItemId(
    val kind: String,
    val videoId: String? = null
)

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

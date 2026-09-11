package com.mo3ibest.voxtube.data.model

data class Video(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelTitle: String,
    val publishedAt: String,
    val duration: String = "",
    val viewCount: String = ""
)

data class TranscriptEntry(
    val text: String,
    val start: Double,
    val duration: Double
)

data class TranscriptResponse(
    val video_id: String,
    val transcript: List<TranscriptEntry>
)

data class TTSRequest(
    val text: String,
    val voice: String = "Charon"
)

data class TTSResponse(
    val audio_base64: String,
    val mime_type: String
)

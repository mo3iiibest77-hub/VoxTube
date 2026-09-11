package com.mo3ibest.voxtube.data.api

import com.mo3ibest.voxtube.data.model.TranscriptResponse
import com.mo3ibest.voxtube.data.model.TTSRequest
import com.mo3ibest.voxtube.data.model.TTSResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface VoxTubeApi {
    @POST("transcript")
    suspend fun getTranscript(@Body body: Map<String, String>): Response<TranscriptResponse>

    @POST("tts")
    suspend fun textToSpeech(@Body body: TTSRequest): Response<TTSResponse>
}

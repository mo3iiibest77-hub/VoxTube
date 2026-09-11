package com.mo3ibest.voxtube.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mo3ibest.voxtube.data.api.VoxTubeApi
import com.mo3ibest.voxtube.data.model.TTSRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val voxTubeApi: VoxTubeApi
) : ViewModel() {

    private val _dubbingStatus = MutableLiveData<String>()
    val dubbingStatus: LiveData<String> = _dubbingStatus

    private val _audioBase64 = MutableLiveData<String?>()
    val audioBase64: LiveData<String?> = _audioBase64

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun startDubbing(videoUrl: String) {
        viewModelScope.launch {
            try {
                _dubbingStatus.value = "در حال دریافت متن ویدیو..."
                val transcriptResponse = voxTubeApi.getTranscript(mapOf("url" to videoUrl))

                if (!transcriptResponse.isSuccessful) {
                    _error.value = "متن ویدیو پیدا نشد. این ویدیو subtitle داره؟"
                    return@launch
                }

                val transcript = transcriptResponse.body()?.transcript ?: run {
                    _error.value = "متن خالی دریافت شد"
                    return@launch
                }

                val fullText = transcript.joinToString(" ") { it.text }
                val truncatedText = if (fullText.length > 2000) fullText.take(2000) else fullText

                _dubbingStatus.value = "در حال ساخت صدای فارسی..."

                val ttsResponse = voxTubeApi.textToSpeech(
                    TTSRequest(text = truncatedText, voice = "Charon")
                )

                if (ttsResponse.isSuccessful) {
                    _audioBase64.value = ttsResponse.body()?.audio_base64
                } else {
                    _error.value = "خطا در ساخت صدا"
                }

            } catch (e: Exception) {
                _error.value = "خطای شبکه: ${e.message}"
            }
        }
    }
}

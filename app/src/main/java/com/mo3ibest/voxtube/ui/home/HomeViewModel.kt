package com.mo3ibest.voxtube.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mo3ibest.voxtube.BuildConfig
import com.mo3ibest.voxtube.data.api.YouTubeApi
import com.mo3ibest.voxtube.data.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val youTubeApi: YouTubeApi
) : ViewModel() {

    private val _videos = MutableLiveData<List<Video>>()
    val videos: LiveData<List<Video>> = _videos

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val apiKey: String
        get() = BuildConfig.YOUTUBE_API_KEY

    fun loadTrendingVideos() {
        if (apiKey.isBlank()) {
            _error.value = "YOUTUBE_API_KEY تنظیم نشده. در CI یا gradle.properties مقدار بده."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = youTubeApi.getVideos(apiKey = apiKey)
                if (response.isSuccessful) {
                    val videos = response.body()?.items?.map { item ->
                        Video(
                            id = item.id.resolvedId(),
                            title = item.snippet.title,
                            description = item.snippet.description,
                            thumbnailUrl = item.snippet.thumbnails.high?.url
                                ?: item.snippet.thumbnails.medium?.url ?: "",
                            channelTitle = item.snippet.channelTitle,
                            publishedAt = item.snippet.publishedAt,
                            viewCount = item.statistics?.viewCount ?: "0"
                        )
                    } ?: emptyList()
                    _videos.value = videos
                } else {
                    _error.value = "خطا در دریافت ویدیوها (${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = "خطای شبکه: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchVideos(query: String) {
        if (apiKey.isBlank()) {
            _error.value = "YOUTUBE_API_KEY تنظیم نشده"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = youTubeApi.searchVideos(query = query, apiKey = apiKey)
                if (response.isSuccessful) {
                    val videos = response.body()?.items?.map { item ->
                        Video(
                            id = item.id.resolvedId(),
                            title = item.snippet.title,
                            description = item.snippet.description,
                            thumbnailUrl = item.snippet.thumbnails.high?.url
                                ?: item.snippet.thumbnails.medium?.url ?: "",
                            channelTitle = item.snippet.channelTitle,
                            publishedAt = item.snippet.publishedAt
                        )
                    } ?: emptyList()
                    _videos.value = videos
                } else {
                    _error.value = "خطا در جستجو (${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = "خطای شبکه: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

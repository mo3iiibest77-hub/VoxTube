package com.mo3ibest.voxtube.ui.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.mo3ibest.voxtube.databinding.ActivityPlayerBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()
    private var youTubePlayer: YouTubePlayer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isDubbing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoId = intent.getStringExtra("video_id") ?: return
        val videoTitle = intent.getStringExtra("video_title") ?: ""
        val videoUrl = intent.getStringExtra("video_url") ?: ""

        binding.tvVideoTitle.text = videoTitle

        setupYouTubePlayer(videoId)
        setupButtons(videoUrl)
        setupObservers()
    }

    private fun setupYouTubePlayer(videoId: String) {
        lifecycle.addObserver(binding.youtubePlayerView)

        binding.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                this@PlayerActivity.youTubePlayer = youTubePlayer
                youTubePlayer.loadVideo(videoId, 0f)
            }
        })
    }

    private fun setupButtons(videoUrl: String) {
        binding.btnDubbing.setOnClickListener {
            if (!isDubbing) {
                startDubbing(videoUrl)
            } else {
                stopDubbing()
            }
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun startDubbing(videoUrl: String) {
        isDubbing = true
        binding.btnDubbing.text = "⏹ متوقف کردن دوبله"
        binding.btnDubbing.isEnabled = false
        binding.progressDubbing.visibility = View.VISIBLE
        binding.tvDubbingStatus.text = "در حال دریافت متن ویدیو..."
        binding.tvDubbingStatus.visibility = View.VISIBLE

        viewModel.startDubbing(videoUrl)
    }

    private fun stopDubbing() {
        isDubbing = false
        binding.btnDubbing.text = "🎙 دوبله فارسی"
        binding.progressDubbing.visibility = View.GONE
        binding.tvDubbingStatus.visibility = View.GONE
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun setupObservers() {
        viewModel.dubbingStatus.observe(this) { status ->
            binding.tvDubbingStatus.text = status
        }

        viewModel.audioBase64.observe(this) { audioBase64 ->
            audioBase64?.let {
                binding.progressDubbing.visibility = View.GONE
                binding.btnDubbing.isEnabled = true
                playAudio(it)
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.progressDubbing.visibility = View.GONE
                binding.btnDubbing.isEnabled = true
                binding.tvDubbingStatus.visibility = View.GONE
                isDubbing = false
                binding.btnDubbing.text = "🎙 دوبله فارسی"
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun playAudio(base64Audio: String) {
        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            val tempFile = File.createTempFile("tts_audio", ".wav", cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    binding.tvDubbingStatus.text = "دوبله تموم شد ✓"
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در پخش صدا: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

package com.mo3ibest.voxtube.ui.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mo3ibest.voxtube.databinding.ActivityPlayerBinding
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()
    private var youTubePlayer: YouTubePlayer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isDubbingActive = false
    private var preparedChunks: List<DubChunk> = emptyList()
    private var nextChunkIndex = 0
    private var currentVideoSec = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isDubbingActive) {
                maybePlayChunkAt(currentVideoSec.toDouble())
                handler.postDelayed(this, 250)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoId = intent.getStringExtra("video_id") ?: return
        val videoTitle = intent.getStringExtra("video_title") ?: ""
        val videoUrl = intent.getStringExtra("video_url") ?: "https://www.youtube.com/watch?v=$videoId"

        binding.tvVideoTitle.text = videoTitle

        setupYouTubePlayer(videoId)
        setupButtons(videoUrl)
        setupObservers()
    }

    private fun setupYouTubePlayer(videoId: String) {
        lifecycle.addObserver(binding.youtubePlayerView)

        binding.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(player: YouTubePlayer) {
                youTubePlayer = player
                player.loadVideo(videoId, 0f)
            }

            override fun onCurrentSecond(player: YouTubePlayer, second: Float) {
                currentVideoSec = second
            }

            override fun onStateChange(player: YouTubePlayer, state: PlayerConstants.PlayerState) {
                if (state == PlayerConstants.PlayerState.PLAYING && isDubbingActive) {
                    // Best-effort mute of original audio while dubbed
                    try {
                        player.mute()
                    } catch (_: Exception) {
                    }
                }
            }
        })
    }

    private fun setupButtons(videoUrl: String) {
        binding.btnDubbing.setOnClickListener {
            if (!isDubbingActive) {
                startDubbing(videoUrl)
            } else {
                stopDubbing()
            }
        }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun startDubbing(videoUrl: String) {
        binding.btnDubbing.isEnabled = false
        binding.progressDubbing.visibility = View.VISIBLE
        binding.tvDubbingStatus.visibility = View.VISIBLE
        binding.tvDubbingStatus.text = "آماده‌سازی دوبله…"
        viewModel.startDubbing(videoUrl)
    }

    private fun stopDubbing() {
        isDubbingActive = false
        handler.removeCallbacks(tickRunnable)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        preparedChunks = emptyList()
        nextChunkIndex = 0
        viewModel.clearDubbing()
        try {
            youTubePlayer?.unMute()
        } catch (_: Exception) {
        }
        binding.btnDubbing.text = "دوبله فارسی"
        binding.btnDubbing.isEnabled = true
        binding.progressDubbing.visibility = View.GONE
        binding.tvDubbingStatus.visibility = View.GONE
    }

    private fun setupObservers() {
        viewModel.dubbingStatus.observe(this) { status ->
            binding.tvDubbingStatus.text = status
        }

        viewModel.isPreparing.observe(this) { preparing ->
            binding.progressDubbing.visibility = if (preparing) View.VISIBLE else View.GONE
            if (preparing) binding.btnDubbing.isEnabled = false
        }

        viewModel.chunks.observe(this) { list ->
            if (list == null) return@observe
            preparedChunks = list.filter { it.audioBase64 != null }
            nextChunkIndex = 0
            isDubbingActive = true
            binding.btnDubbing.text = "⏹ توقف دوبله"
            binding.btnDubbing.isEnabled = true
            binding.progressDubbing.visibility = View.GONE
            try {
                youTubePlayer?.mute()
            } catch (_: Exception) {
            }
            handler.removeCallbacks(tickRunnable)
            handler.post(tickRunnable)
            Toast.makeText(this, "دوبله فعال شد (${preparedChunks.size} بخش)", Toast.LENGTH_SHORT).show()
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.progressDubbing.visibility = View.GONE
                binding.btnDubbing.isEnabled = true
                binding.btnDubbing.text = "دوبله فارسی"
                isDubbingActive = false
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun maybePlayChunkAt(sec: Double) {
        if (nextChunkIndex >= preparedChunks.size) return
        val chunk = preparedChunks[nextChunkIndex]
        // Start a little early to compensate TTS lag
        if (sec + 0.15 >= chunk.startSec) {
            val audio = chunk.audioBase64 ?: run {
                nextChunkIndex++
                return
            }
            playAudioChunk(audio)
            nextChunkIndex++
        }
    }

    private fun playAudioChunk(base64Audio: String) {
        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            val tempFile = File.createTempFile("tts_chunk_", ".wav", cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer?.let {
                try {
                    it.stop()
                } catch (_: Exception) {
                }
                it.release()
            }
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
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در پخش بخش: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

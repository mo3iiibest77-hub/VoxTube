package com.mo3ibest.voxtube.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.mo3ibest.voxtube.data.model.Video
import com.mo3ibest.voxtube.databinding.ActivityMainBinding
import com.mo3ibest.voxtube.ui.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.regex.Pattern

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var videoAdapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupSearchView()
        setupUrlPaste()
        setupObservers()

        viewModel.loadTrendingVideos()
    }

    private fun setupUI() {
        val userName = intent.getStringExtra("user_name") ?: "کاربر"
        val userPhoto = intent.getStringExtra("user_photo")

        binding.tvWelcome.text = "سلام، $userName"

        if (!userPhoto.isNullOrEmpty()) {
            Glide.with(this)
                .load(userPhoto)
                .circleCrop()
                .into(binding.ivUserAvatar)
        }
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoAdapter { video -> openPlayer(video) }
        binding.rvVideos.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = videoAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val q = query?.trim().orEmpty()
                if (q.isEmpty()) return true
                // If user pasted a YouTube URL in search, open player directly
                val id = extractVideoId(q)
                if (id != null) {
                    openFromVideoId(id)
                    return true
                }
                viewModel.searchVideos(q)
                return true
            }
            override fun onQueryTextChange(newText: String?) = false
        })
    }

    private fun setupUrlPaste() {
        binding.btnOpenUrl.setOnClickListener {
            // Prefer paste field; fall back to search bar (common UX mistake)
            val fromPaste = binding.etYoutubeUrl.text?.toString()?.trim().orEmpty()
            val fromSearch = binding.searchView.query?.toString()?.trim().orEmpty()
            val raw = when {
                fromPaste.isNotEmpty() -> fromPaste
                fromSearch.isNotEmpty() -> fromSearch
                else -> ""
            }
            if (raw.isEmpty()) {
                Toast.makeText(this, "لینک را در کادر بالا بچسبان", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val id = extractVideoId(raw)
            if (id == null) {
                Toast.makeText(this, "لینک یوتیوب معتبر نیست", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Keep paste field in sync for next time
            if (fromPaste.isEmpty()) {
                binding.etYoutubeUrl.setText(raw)
            }
            openFromVideoId(id)
        }
    }

    private fun openFromVideoId(id: String) {
        openPlayer(
            Video(
                id = id,
                title = "ویدیو ($id)",
                description = "",
                thumbnailUrl = "",
                channelTitle = "لینک مستقیم",
                publishedAt = ""
            )
        )
    }

    private fun extractVideoId(url: String): String? {
        val cleaned = url.trim()
        val patterns = listOf(
            Pattern.compile("(?:youtube\.com/watch\\?v=)([0-9A-Za-z_-]{11})"),
            Pattern.compile("(?:youtube\.com/shorts/)([0-9A-Za-z_-]{11})"),
            Pattern.compile("(?:youtu\.be/)([0-9A-Za-z_-]{11})"),
            Pattern.compile("(?:v=)([0-9A-Za-z_-]{11})"),
            Pattern.compile("(?:embed/)([0-9A-Za-z_-]{11})"),
            Pattern.compile("^([0-9A-Za-z_-]{11})$")
        )
        for (p in patterns) {
            val m = p.matcher(cleaned)
            if (m.find()) return m.group(1)
        }
        return null
    }

    private fun setupObservers() {
        viewModel.videos.observe(this) { videos ->
            videoAdapter.submitList(videos)
            binding.progressBar.visibility = View.GONE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPlayer(video: Video) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("video_id", video.id)
            putExtra("video_title", video.title)
            putExtra("video_url", "https://www.youtube.com/watch?v=${video.id}")
        }
        startActivity(intent)
    }
}

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
import com.mo3ibest.voxtube.R
import com.mo3ibest.voxtube.data.model.Video
import com.mo3ibest.voxtube.databinding.ActivityMainBinding
import com.mo3ibest.voxtube.ui.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint

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
        videoAdapter = VideoAdapter { video ->
            openPlayer(video)
        }
        binding.rvVideos.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = videoAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchVideos(it) }
                return true
            }
            override fun onQueryTextChange(newText: String?) = false
        })
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

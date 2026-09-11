package com.mo3ibest.voxtube.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mo3ibest.voxtube.data.model.Video
import com.mo3ibest.voxtube.databinding.ItemVideoBinding

class VideoAdapter(
    private val onVideoClick: (Video) -> Unit
) : ListAdapter<Video, VideoAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(
        private val binding: ItemVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(video: Video) {
            binding.apply {
                tvTitle.text = video.title
                tvChannel.text = video.channelTitle
                tvViews.text = formatViewCount(video.viewCount)

                Glide.with(itemView.context)
                    .load(video.thumbnailUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_media_play)
                    .into(ivThumbnail)

                root.setOnClickListener { onVideoClick(video) }
            }
        }

        private fun formatViewCount(count: String): String {
            val num = count.toLongOrNull() ?: return ""
            return when {
                num >= 1_000_000 -> "${num / 1_000_000}M بازدید"
                num >= 1_000 -> "${num / 1_000}K بازدید"
                else -> "$num بازدید"
            }
        }
    }

    class VideoDiffCallback : DiffUtil.ItemCallback<Video>() {
        override fun areItemsTheSame(oldItem: Video, newItem: Video) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Video, newItem: Video) = oldItem == newItem
    }
}

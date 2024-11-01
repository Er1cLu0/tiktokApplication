package com.example.tiktokapplication

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tiktokapplication.databinding.ItemVideoBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem

// 数据类表示视频的 URL
data class Video(val videoUrl: String)

class VideoAdapter : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private val videos = mutableListOf<Video>()

    // 更新视频列表
    fun setVideos(videos: List<Video>) {
        this.videos.clear()
        this.videos.addAll(videos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount() = videos.size

    // ViewHolder 类用于管理视频播放
    inner class VideoViewHolder(private val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        internal var player: ExoPlayer? = null

        fun bind(video: Video) {
            // 初始化 ExoPlayer
            player = ExoPlayer.Builder(binding.root.context).build()
            binding.playerView.player = player
            val mediaItem = MediaItem.fromUri(Uri.parse(video.videoUrl))
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
        }

        // 释放播放器资源
        fun releasePlayer() {
            player?.release()
            player = null
        }
    }

    // 释放不再可见的 ViewHolder 中的 ExoPlayer
    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.releasePlayer()
    }
}

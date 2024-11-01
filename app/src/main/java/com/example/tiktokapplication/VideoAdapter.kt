package com.example.tiktokapplication

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 定义一个 Retrofit 接口来描述 API 请求
interface ApiService {
    @POST("api/watch_time")
    fun uploadWatchTime(@Body watchTimeRequest: WatchTimeRequest): Call<Void>
}

data class WatchTimeRequest(
    val user_id: String,
    val video_id: String,
    val duration: Long
)

// 创建一个 Retrofit 实例
object RetrofitClient {
    private const val BASE_URL = "http://teledesktop.amtlld.top:24763/"

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// 数据类表示视频的 URL 和 ID
data class Video(val url: String, val id: String)

class VideoAdapter(private val context: Context) :
    RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    val videos = mutableListOf<Video>()
    private var userId: String = ""

    // 更新视频列表
    fun setVideos(videos: List<Video>, userId: String) {
        this.videos.clear()
        this.videos.addAll(videos)
        this.userId = userId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.bind(video.url, video.id)
    }

    override fun getItemCount(): Int = videos.size

    // ViewHolder 类用于管理视频播放
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerView: PlayerView = itemView.findViewById(R.id.player_view)
        var player: ExoPlayer? = null
        private var startTime: Long = 0
        private var handler: Handler = Handler(Looper.getMainLooper())
        private var runnable: Runnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val duration = currentTime - startTime
                // 记录观看时长，这里可以先保存在一个变量中
                // 当视频切换时再上传到服务器
                Log.d("VideoDuration", "Watched for $duration ms")
                handler.postDelayed(this, 1000) // 每秒更新一次
            }
        }

        fun bind(videoUrl: String, videoId: String) {
            // 初始化 ExoPlayer
            player = ExoPlayer.Builder(context).build()
            playerView.player = player
            val mediaItem = MediaItem.fromUri(videoUrl)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true

            // 开始计时
            startTime = System.currentTimeMillis()
            handler.post(runnable)
        }

        // 释放播放器资源
        fun releasePlayer(videoId: String) {
            player?.release()
            player = null

            // 停止计时
            handler.removeCallbacks(runnable)
            // 这里可以调用一个方法来上传观看时长到服务器
            uploadWatchTime(videoId, System.currentTimeMillis() - startTime)
        }

        private fun uploadWatchTime(videoId: String, duration: Long) {
            // 构建请求体
            val watchTimeRequest = WatchTimeRequest(userId, videoId, duration)

            // 创建 Retrofit 客户端
            val call = RetrofitClient.apiService.uploadWatchTime(watchTimeRequest)

            // 发起请求
            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        // 成功上传
                        Log.d("UploadWatchTime", "Watch time uploaded successfully")
                    } else {
                        // 处理错误
                        Log.e("UploadWatchTime", "Failed to upload watch time: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    // 处理网络错误
                    Log.e("UploadWatchTime", "Network error: ${t.message}")
                }
            })
        }
    }

    // 释放不再可见的 ViewHolder 中的 ExoPlayer
    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.releasePlayer(videos[holder.bindingAdapterPosition].id) // 修改：传入视频 ID
    }
}
package com.example.tiktokapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var videoAdapter: VideoAdapter
    private var lastVisiblePosition = RecyclerView.NO_POSITION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 确保布局文件正确

        // 初始化 RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.mRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 初始化 VideoAdapter 并传递 Context
        videoAdapter = VideoAdapter(this)
        recyclerView.adapter = videoAdapter

        // 加载视频 URL 列表
        loadVideoUrls()

        // 监听 RecyclerView 滑动事件，实现自动播放
                recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (visiblePosition != RecyclerView.NO_POSITION) {
                        // 释放前一个视频的播放器资源
                        if (lastVisiblePosition != RecyclerView.NO_POSITION) {
                            val previousHolder = recyclerView.findViewHolderForAdapterPosition(lastVisiblePosition) as? VideoAdapter.VideoViewHolder
                            val previousVideoId = videoAdapter.videos[lastVisiblePosition].id
                            previousHolder?.releasePlayer(previousVideoId)
                        }
                        // 更新当前可见位置
                        lastVisiblePosition = visiblePosition
                        // 播放当前可见视频
                        val currentHolder = recyclerView.findViewHolderForAdapterPosition(visiblePosition) as? VideoAdapter.VideoViewHolder
                        currentHolder?.player?.playWhenReady = true
                    }
                }
            }
        })
    }

    private fun loadVideoUrls() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = Jsoup.connect("http://teledesktop.amtlld.top:24763/videos/").get()
                val elements = doc.select("a[href$=.mp4]")
                val videoList = elements.map {
                    // 提取视频 URL 和 ID
                    val url = "http://teledesktop.amtlld.top:24763/videos/${it.attr("href")}"
                    val id = it.attr("href").removeSuffix(".mp4") // 新增：提取视频 ID
                    Video(url, id)
                }
                val userId = "user123" // 替换为实际的用户 ID
                withContext(Dispatchers.Main) {
                    videoAdapter.setVideos(videoList, userId) // 修改：传入视频列表和用户 ID
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
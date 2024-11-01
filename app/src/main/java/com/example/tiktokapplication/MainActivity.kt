package com.example.tiktokapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var videoAdapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 确保布局文件正确

        // 初始化 RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        videoAdapter = VideoAdapter()
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
                        val holder = recyclerView.findViewHolderForAdapterPosition(visiblePosition) as? VideoAdapter.VideoViewHolder
                        holder?.player?.playWhenReady = true
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
                    Video("http://teledesktop.amtlld.top:24763/videos/${it.attr("href")}")
                }
                withContext(Dispatchers.Main) {
                    videoAdapter.setVideos(videoList)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

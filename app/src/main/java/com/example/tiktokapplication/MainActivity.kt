package com.example.tiktokapplication

import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import java.net.NetworkInterface

fun getMacAddress(context: Context): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val all = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in all) {
                if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                    val macBytes = networkInterface.hardwareAddress ?: return ""
                    val macAddress = macBytes.joinToString(":") { byte -> "%02X".format(byte) }
                    return macAddress
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    } else {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.connectionInfo.macAddress ?: ""
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var videoAdapter: VideoAdapter
    private var currentVideoId: Int? = null
    private var startTime: Long = 0L
    private val macAddress = getMacAddress(this) // 设备的MAC地址
    private val apiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://teledesktop.amtlld.top:37568/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.mRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        videoAdapter = VideoAdapter { video ->
            onVideoChanged(video.video_id)
        }
        recyclerView.adapter = videoAdapter

        // 加载视频 URL 列表
        loadVideoUrls()
    }

    private fun loadVideoUrls() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val videoList = apiService.getVideos()
                withContext(Dispatchers.Main) {
                    videoAdapter.setVideos(videoList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "获取视频失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onVideoChanged(newVideoId: Int) {
        // 如果当前有视频正在播放，计算观看时间并上传记录
        currentVideoId?.let { previousVideoId ->
            val watchDuration = (SystemClock.elapsedRealtime() - startTime) / 1000 // 秒
            val watchTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
            val record = WatchRecord(
                macAddress = macAddress,
                videoId = previousVideoId,
                watchDuration = watchDuration.toInt(),
                watchTime = watchTime
            )
            uploadWatchRecord(record)
        }

        // 更新为新视频的 ID，并重置开始时间
        currentVideoId = newVideoId
        startTime = SystemClock.elapsedRealtime()
    }

    private fun uploadWatchRecord(record: WatchRecord) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.uploadWatchRecord(record)
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "观看记录上传成功", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "观看记录上传失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "上传时出错", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

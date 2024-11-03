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
import android.util.Log
import android.view.MotionEvent
import retrofit2.Call
import java.net.NetworkInterface
import com.google.gson.Gson

fun getMacAddress(context: Context): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            NetworkInterface.getNetworkInterfaces().toList().find {
                it.name.equals("wlan0", ignoreCase = true)
            }?.hardwareAddress?.joinToString(":") { byte -> "%02X".format(byte) } ?: ""
        } catch (e: Exception) {
            ""
        }
    } else {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.connectionInfo.macAddress ?: ""
    }
}
fun getDeviceIdentifier(context: Context): String {
    return if (Build.FINGERPRINT.contains("generic")) {
        "00:11:22:33:44:55" // 模拟器的静态MAC地址
    } else {
        getMacAddress(context) // 实设备获取真实MAC地址
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var videoAdapter: VideoAdapter
    private var currentVideoId: Int? = null
    private var startTime: Long = 0L
    private val macAddress = "00:11:22:33:44:55" // 测试时使用的静态地址
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
            Log.d("MainActivity", "Video clicked: ${video.video_id}") // 添加日志
            onVideoChanged(video.video_id)
        }
        recyclerView.adapter = videoAdapter

        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent): Boolean {
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    val child = recyclerView.findChildViewUnder(motionEvent.x, motionEvent.y)
                    child?.let {
                        val position = recyclerView.getChildAdapterPosition(it)
                        val video = videoAdapter.videos[position] // 获取对应的视频对象
                        Log.d("MainActivity", "Video clicked: ${video.video_id}")
                        onVideoChanged(video.video_id) // 手动调用
                    }
                }
                return super.onInterceptTouchEvent(recyclerView, motionEvent)
            }
        })

        // 加载视频 URL 列表
        loadVideoUrls()
        // 测试上传代码
        //testUploadWatchRecord()
    }
    private fun testUploadWatchRecord() {
        // 使用当前时间和 MAC 地址模拟一条观看记录
        val testWatchRecord = WatchRecord(
            macAddress = "00:1B:44:11:3A:B7",
            videoId = 1, // 假设视频 ID 为 1
            watchDuration = 100, // 假设观看时长为 120 秒
            watchTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(System.currentTimeMillis())
        )

        // 调用 uploadWatchRecord 方法上传测试记录
        uploadWatchRecord(testWatchRecord)
    }

    private fun loadVideoUrls() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val videoList = apiService.getVideos()
                Log.d("LoadVideos", "Video list loaded: $videoList") // 添加日志
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

    private fun uploadWatchRecord(record: WatchRecord) {
        Log.d("UploadRecord", "Uploading watch record: $record") // 输出日志
        Log.d("UploadRecord", "Uploading JSON: ${Gson().toJson(record)}")

        val call = apiService.addWatchRecord(record)
        call.enqueue(object : retrofit2.Callback<Map<String, String>> {
            override fun onResponse(
                call: Call<Map<String, String>>,
                response: retrofit2.Response<Map<String, String>>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "观看记录上传成功", Toast.LENGTH_SHORT).show()
                } else {
                    // 记录响应的状态码和错误消息
                    Log.e("UploadRecord", "上传失败，状态码: ${response.code()}, 错误信息: ${response.errorBody()?.string()}")
                    Toast.makeText(this@MainActivity, "观看记录上传失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                t.printStackTrace()
                Log.e("UploadError", "上传时出错: ${t.message}")
                Toast.makeText(this@MainActivity, "上传时出错", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun onVideoChanged(newVideoId: Int) {
        Log.d("VideoChanged", "New video ID: $newVideoId, Previous video ID: $currentVideoId")

        if (currentVideoId == null) {
            // 如果这是第一次播放视频，直接设置当前视频 ID 和开始时间
            currentVideoId = newVideoId
            startTime = SystemClock.elapsedRealtime()

        }

        // 如果当前有视频正在播放，计算观看时间并上传记录
        currentVideoId?.let { previousVideoId ->
            val watchDuration = (SystemClock.elapsedRealtime() - startTime) / 1000 // 秒
            val watchTime = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            ).format(System.currentTimeMillis())
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


}

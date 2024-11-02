package com.example.tiktokapplication

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("api/videos")
    suspend fun getVideos(): List<Video>

    @POST("/api/watch_record")
    fun addWatchRecord(@Body watchRecord: WatchRecord): Call<Map<String, String>>
}


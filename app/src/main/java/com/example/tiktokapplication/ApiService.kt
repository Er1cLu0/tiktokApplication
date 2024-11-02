package com.example.tiktokapplication

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("api/videos")
    suspend fun getVideos(): List<Video>

    @POST("api/watch_record")
    suspend fun uploadWatchRecord(@Body record: WatchRecord): Response<Unit>
}


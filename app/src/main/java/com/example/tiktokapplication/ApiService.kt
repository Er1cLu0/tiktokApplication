package com.example.tiktokapplication

import retrofit2.http.GET

interface ApiService {
    @GET("api/videos")
    suspend fun getVideos(): List<Video>
}

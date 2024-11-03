package com.example.tiktokapplication

import com.google.gson.annotations.SerializedName

data class WatchRecord(
    @SerializedName("mac_address") val macAddress: String,
    @SerializedName("video_id") val videoId: Int,
    @SerializedName("watch_duration") val watchDuration: Int,
    @SerializedName("watch_time") val watchTime: String
)
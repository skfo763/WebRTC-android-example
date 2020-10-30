package com.skfo763.rtc.data

import com.google.gson.annotations.SerializedName

data class MatchModel(
        @SerializedName("offer") val isOffer: Boolean,
        @SerializedName("otherIdx") val otherIdx: Int,
        @SerializedName("duration") val duration: Int,
        @SerializedName("matchIdx") val matchIdx: Int,
        @SerializedName("previewText") val previewText: String?,
        @SerializedName("previewTime") val previewTime: Int?,
        @SerializedName("skin") val skinIdentifier: String
)
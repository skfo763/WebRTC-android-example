package com.skfo763.rtc.data

import com.google.gson.annotations.SerializedName

data class SignalServerInfo(
        @SerializedName("signal_server_host") val signalServerHost : String, // 소켓에 넣어줄 호스트 url
        @SerializedName("stun_and_turn") val stunAndTurn : List<StunAndTurn>, // 스턴 앤 턴 서버
        @SerializedName("password") val password : String // 인증시 부여되는 번
)

data class StunAndTurn(
        @SerializedName("urls") val urlList : List<String>,
        @SerializedName("username") val userName : String,
        @SerializedName("credential" )val credential : String
)
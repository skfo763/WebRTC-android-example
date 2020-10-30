package com.skfo763.rtc.data

import com.google.gson.annotations.SerializedName

data class SignalServerInfo(
        @SerializedName("signal_server_host") val signalServerHost : String = "http://54.180.123.99:9500", // 소켓에 넣어줄 호스트 url
        @SerializedName("stun_and_turn") val stunAndTurn : List<StunAndTurn> = listOf(StunAndTurn()), // 스턴 앤 턴 서버
        @SerializedName("password") val password : String = "123456"// 인증시 부여되는 번
)

data class StunAndTurn(
        @SerializedName("urls") val urlList : List<String> = listOf("stun:coturn.mozzet.com:443", "turn:coturn.mozzet.com:443"),
        @SerializedName("username") val userName : String = "pxUunWktnveMisn9WbEQ01Hd0iURP6ZD",
        @SerializedName("credential" )val credential : String = "nqcfx2ek8gQ56UFyv2huA6KWyQD9gPiT"
)
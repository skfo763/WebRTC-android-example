package com.skfo763.rtc.data

import com.google.gson.annotations.SerializedName

data class SignalServerInfo(
    val signalServerHost : String, // 소켓에 넣어줄 호스트 url
    val stunAndTurn : List<StunAndTurn>, // 스턴 앤 턴 서버
    val password : String // 인증시 부여되는 번
)

data class StunAndTurn(
    val urlList : List<String>,
    val userName : String,
    val credential : String
)
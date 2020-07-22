package com.skfo763.rtc.data

data class UserJoinInfo(
    val token: String,
    val password: String,
    val isBlindMode: Boolean = false
)
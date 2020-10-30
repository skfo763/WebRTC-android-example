package com.skfo763.rtcandroid_example.utils

import com.skfo763.rtcandroid_example.BuildConfig

class TokenManager {

    companion object {
        const val DEFAULT_PASSWORD = "123456"

        @JvmStatic
        fun getToken(isMan: Boolean) : String {
            return if(isMan) BuildConfig.TOKEN_MAN else BuildConfig.TOKEN_WOMAN
        }
    }
}
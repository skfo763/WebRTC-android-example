package com.skfo763.rtcandroid_example.utils

import com.skfo763.rtcandroid_example.TOKEN_MAN
import com.skfo763.rtcandroid_example.TOKEN_WOMEN

class TokenManager {

    companion object {

        @JvmStatic
        fun getToken(isMan: Boolean) : String {
            return if(isMan) TOKEN_MAN else TOKEN_WOMEN
        }
    }
}
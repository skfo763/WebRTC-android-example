package com.skfo763.rtcandroid_example.base

interface MainActivityUseCase {

    val token: String

    fun vibrate(milliSecond: Long)

    fun showTopBanner(stringResId: Int, colorResId: Int)

    fun showTopBanner(message: String, resId: Int)

    fun showToast(text: String)

    fun finishActivity()

}
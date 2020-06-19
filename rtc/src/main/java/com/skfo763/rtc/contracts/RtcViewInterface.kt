package com.skfo763.rtc.contracts

import com.skfo763.rtc.data.UserJoinInfo

interface RtcViewInterface {

    fun updateWaitInfo(text: String)

    fun sendFinishInfo(displayRating: Boolean, matchIdx: Int)

    fun stopCall()

    fun finishActivity()

    fun refundCandy()

    fun handleError(showMessage: Boolean, message: String?)

    fun sendTimerAndIdx(sendTimeToDisposable: Any, otherIdx: Int)

    fun startCall()

    fun getUserInfo(): UserJoinInfo

}
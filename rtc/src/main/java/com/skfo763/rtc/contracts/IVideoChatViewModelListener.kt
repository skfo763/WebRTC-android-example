package com.skfo763.rtc.contracts

import com.skfo763.rtc.data.MatchModel
import com.skfo763.rtc.data.UserJoinInfo

interface IVideoChatViewModelListener  {

    fun onMatched(data: MatchModel)

    fun updateWaitInfo(text: String)

    fun onUiEvent(uiEvent: RtcUiEvent, message: String? = null)

    fun onError(e: ErrorHandleData)

    fun getUserInfo(): UserJoinInfo

}
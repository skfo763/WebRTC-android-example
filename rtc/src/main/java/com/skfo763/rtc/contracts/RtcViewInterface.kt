package com.skfo763.rtc.contracts

import com.skfo763.rtc.data.UserJoinInfo

interface RtcViewInterface {

    fun updateWaitInfo(text:String)

    fun onUiEvent(uiEvent: VoiceChatUiEvent)

    fun onError(e:Any)

    fun callUserInfo() : UserJoinInfo

    fun sendTimerAndIdx(duration: Int, otherIdx: Int) // 통합: 같은 콜백에서 받는부분 이라서 , 따로하니까 에러남

    fun sendFinishInfo(displayRating:Boolean?=false, matchIdx:Int?=-9999) // displayRating = 통화종료화면 별점 메기는 부분 보여줄것인지의 여부 , matchIdx = 상대방에게 별점메길때 상대방 아이디값
}
package com.skfo763.rtc.manager

import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SocketManager {

    fun sendJoinToSocket(userInfo: JSONObject)

    fun sendOfferAnswerToSocket(sessionDescription: SessionDescription)

    fun sendIceCandidateToSocket(iceCandidate: IceCandidate)

    fun sendHangUpEventToSocket(data: Any)

    fun sendCommonEventToSocket(data: String)

}
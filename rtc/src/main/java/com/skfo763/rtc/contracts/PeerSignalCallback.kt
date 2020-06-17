package com.skfo763.rtc.contracts

import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface PeerSignalCallback {

    fun onConnected()

    fun createOffer()

    fun createAnswer()

    fun onOfferReceived(description: SessionDescription)

    fun onAnswerReceived(description: SessionDescription)

    fun onIceCandidateReceived(iceCandidate: IceCandidate)

    fun onMatched(obj: JSONObject)

    fun onHangUp(ackJson: JSONObject)

    fun onHangUpSuccess()

    fun onTerminate(terminateState: String)

    fun onError(isCritical: Boolean, showMessage: Boolean, message: String)
}
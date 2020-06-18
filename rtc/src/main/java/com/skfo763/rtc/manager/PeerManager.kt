package com.skfo763.rtc.manager

import org.webrtc.IceCandidate
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

interface PeerManager {

    fun callOffer(sdpObserver: SdpObserver)

    fun callAnswer(sdpObserver: SdpObserver)

    fun onRemoteSessionReceived(sdpObserver: SdpObserver, sessionDescription: SessionDescription)

    fun addIceCandidate(iceCandidate: IceCandidate)

    fun disconnect()
}
package com.skfo763.rtc.manager

import com.skfo763.rtc.data.SignalServerInfo
import org.webrtc.IceCandidate
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

interface PeerManager {

    fun initSurfaceView(surfaceViewRenderer: SurfaceViewRenderer)

    fun startLocalVideoCapture(localSurfaceView: SurfaceViewRenderer)

    fun setIceServer(signalServerInfo: SignalServerInfo)

    fun callOffer(sdpObserver: SdpObserver)

    fun callAnswer(sdpObserver: SdpObserver)

    fun onRemoteSessionReceived(sdpObserver: SdpObserver, sessionDescription: SessionDescription)

    fun addIceCandidate(iceCandidate: IceCandidate)

    fun disconnectPeer()
}
package com.skfo763.rtc.manager

import com.skfo763.rtc.data.SignalServerInfo
import org.webrtc.*

interface PeerManager {

    fun initSurfaceView(surfaceViewRenderer: SurfaceViewRenderer)

    fun startLocalVideoCapture(localSurfaceView: SurfaceViewRenderer)

    fun startRemoteVideoCapture(remoteSurfaceView: SurfaceViewRenderer, mediaStream: MediaStream?)

    fun setIceServer(signalServerInfo: SignalServerInfo)

    fun callOffer(sdpObserver: SdpObserver)

    fun callAnswer(sdpObserver: SdpObserver)

    fun onRemoteSessionReceived(sdpObserver: SdpObserver, sessionDescription: SessionDescription)

    fun addIceCandidate(iceCandidate: IceCandidate)

    fun disconnectPeer()
}
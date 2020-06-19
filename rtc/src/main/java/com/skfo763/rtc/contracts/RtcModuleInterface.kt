package com.skfo763.rtc.contracts

import com.skfo763.rtc.data.SignalServerInfo
import org.webrtc.SurfaceViewRenderer

interface RtcModuleInterface {

    fun setLocalSurfaceView(local: SurfaceViewRenderer)

    fun setRemoteSurfaceView(remote: SurfaceViewRenderer)

    /**
     * 이 메소드는 반드시 local, remote surface 뷰가 세팅되고 난 다음에 호출되어야 합니다.
     */
    fun setPeerInfo(peer : SignalServerInfo)

    fun startWaiting(peer: SignalServerInfo)

    fun stopCallSignFromClient(stoppedAt: StopCallType)

}
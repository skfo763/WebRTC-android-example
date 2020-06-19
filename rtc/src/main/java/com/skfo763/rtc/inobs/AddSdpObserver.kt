package com.skfo763.rtc.inobs

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class AppSdpObserver : SdpObserver {

    override fun onSetFailure(p0: String?) {
    }

    override fun onSetSuccess() {
    }

    override fun onCreateSuccess(desc: SessionDescription?) {
    }

    override fun onCreateFailure(p0: String?) {
    }
}
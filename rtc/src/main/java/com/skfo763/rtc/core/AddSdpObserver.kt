package com.skfo763.rtc.core

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class AddSdpObserver: SdpObserver {
    override fun onSetFailure(p0: String?) {
    }

    override fun onSetSuccess() {
    }

    override fun onCreateSuccess(p0: SessionDescription?) {
    }

    override fun onCreateFailure(p0: String?) {
    }
}
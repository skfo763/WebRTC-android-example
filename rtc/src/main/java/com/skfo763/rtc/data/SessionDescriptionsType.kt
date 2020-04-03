package com.skfo763.rtc.data

import org.webrtc.SessionDescription

data class SessionDescriptionsType (
    val type: String,
    val description: SessionDescription
)
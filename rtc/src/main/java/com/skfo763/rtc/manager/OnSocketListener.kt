package com.skfo763.rtc.manager

interface OnSocketListener {
    fun onSocketState(state: String, data: Array<Any>, onComplete: (() -> Unit)? = null)
}
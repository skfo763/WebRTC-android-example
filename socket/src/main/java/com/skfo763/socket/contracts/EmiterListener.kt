package com.skfo763.socket.contracts

interface EmitterListener {
    fun onMessageReceived(message: Any?)

    fun onConnected(connectData: Array<Any>)

    fun onReconnected()

    fun onMatched(match: Array<Any>)

    fun onDisconnected()

    fun onConnectError()

    fun onWaitingStatus(wait: Array<Any>)

    fun onTerminate(terminate: Array<Any>)
}
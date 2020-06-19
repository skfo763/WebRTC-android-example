package com.skfo763.socket.contracts

interface SocketEmitterListener {
    fun onMessageReceived(message: Any?)

    fun onConnected(connectData: Array<Any>)

    fun onReconnected()

    fun onMatched(match: Array<Any>)

    fun onDisconnected(disconnect: Any?)

    fun onConnectError(retryCount: Int)

    fun onWaitingStatus(wait: Array<Any>)

    fun onTerminate(terminate: Array<Any>)

    fun onErrorRetry(retryCount: Int)
}
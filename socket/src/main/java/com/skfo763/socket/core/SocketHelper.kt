package com.skfo763.socket.core

import com.skfo763.socket.*
import com.skfo763.socket.contracts.SocketEmitterListener
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import okhttp3.OkHttpClient
import java.lang.Exception
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SocketHelper(listener: SocketEmitterListener) {

    private var socket: Socket? = null
    private val retryConnectionCount = AtomicInteger(0)

    private val messageListener = Emitter.Listener {
        listener.onMessageReceived(it.getOrNull(0))
    }

    private val connectListener = Emitter.Listener {
        retryConnectionCount.set(0)
        listener.onConnected(it)
    }

    private val reconnectListener = Emitter.Listener {
        listener.onReconnected(it)
    }

    private val matchListener = Emitter.Listener {
        listener.onMatched(it)
    }

    private val disconnectListener = Emitter.Listener {
        listener.onDisconnected(it.getOrNull(0))
    }

    private val connectErrorListener = Emitter.Listener {
        (it.getOrNull(0) as Exception).printStackTrace()

        val connectCount = retryConnectionCount.get()
        if(connectCount >= RECONNECTION_ATTEMPTS - 1) {
            listener.onConnectError(connectCount)
            retryConnectionCount.set(0)
        } else {
            retryConnectionCount.set(connectCount + 1)
            listener.onErrorRetry(connectCount + 1)
        }
    }

    private val waitingStatusListener = Emitter.Listener {
        listener.onWaitingStatus(it)
    }

    private val terminatedListener = Emitter.Listener {
        listener.onTerminate(it)
    }

    // 로컬 테스트 시 ssl 인증 우회하기 위해 필요합니다. 자체 ssl 인증 서버가 있으면 안써도 됩니다.
    private fun getOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object: X509TrustManager {
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, null)
        }
        val trustManager = trustAllCerts[0] as X509TrustManager

        return OkHttpClient.Builder()
            .hostnameVerifier { _, _ -> true }
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    private fun getSocketOptions(okHttpClient: OkHttpClient? = null) = IO.Options().apply {
        okHttpClient?.let {
            callFactory = it
            webSocketFactory = it
        }
        reconnection = RECONNECTION
        randomizationFactor = RANDOMIZATION_FACTOR
        reconnectionAttempts = RECONNECTION_ATTEMPTS
        reconnectionDelay = RECONNECTION_DELAY
        reconnectionDelayMax = RECONNECTION_DELAY_MAX
    }

    fun initializeSocket(url: String) {
        if(socket == null) {
            val socketOptions = getSocketOptions()
            socket = IO.socket(url, socketOptions).apply {
                connectListener()
                connect()
            }
        }
    }

    fun sendSocket(event: String, vararg data: String, onCall: ((args: Array<Any?>) -> Unit)? = null) {
        onCall?.let { function ->
            socket?.emit(event, data) { function.invoke(it) }
        } ?: kotlin.run {
            socket?.emit(event, data)
        }
    }

    fun sendSocket(event: SocketListenerEvent, data: Any) {
        socket?.emit(event.value, data)
    }

    fun sendSocket(event: SocketListenerEvent, vararg data: Any, onCall: ((args: Array<Any?>) -> Unit)? = null) {
        onCall?.let { function ->
            socket?.emit(event.value, data) { function.invoke(it) }
        } ?: kotlin.run {
            socket?.emit(event.value, data)
        }
    }

    fun releaseSocket() {
        socket?.disconnectListener()
        socket?.disconnect()
        socket = null
    }

    private fun Socket.connectListener() {
        on(SocketListenerEvent.EVENT_MESSAGE.value, messageListener)
        on(SocketListenerEvent.EVENT_CONNECT.value, connectListener)
        on(SocketListenerEvent.EVENT_RECONNECT_ERROR.value, connectErrorListener)
        on(SocketListenerEvent.EVENT_DISCONNECT.value, disconnectListener)
        on(SocketListenerEvent.EVENT_RECONNECT.value, reconnectListener)
        on(SocketListenerEvent.EVENT_MATCHED.value, matchListener)
        on(SocketListenerEvent.EVENT_TERMINATED.value, terminatedListener)
        on(SocketListenerEvent.EVENT_WAITING_STATUS.value, waitingStatusListener)
    }

    private fun Socket.disconnectListener() {
        off(SocketListenerEvent.EVENT_MESSAGE.value, messageListener)
        off(SocketListenerEvent.EVENT_CONNECT.value, connectListener)
        off(SocketListenerEvent.EVENT_RECONNECT_ERROR.value, connectErrorListener)
        off(SocketListenerEvent.EVENT_DISCONNECT.value, disconnectListener)
        off(SocketListenerEvent.EVENT_RECONNECT.value, reconnectListener)
        off(SocketListenerEvent.EVENT_MATCHED.value, matchListener)
        off(SocketListenerEvent.EVENT_TERMINATED.value, terminatedListener)
        off(SocketListenerEvent.EVENT_WAITING_STATUS.value, waitingStatusListener)
    }
}
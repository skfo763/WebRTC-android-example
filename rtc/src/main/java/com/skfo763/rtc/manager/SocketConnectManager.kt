package com.skfo763.rtc.manager

import android.os.Build
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.Polling
import io.socket.engineio.client.transports.PollingXHR
import io.socket.engineio.client.transports.WebSocket
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

open class SocketConnectManager(private val onSocketListener: OnSocketListener) {

    protected var socket: Socket? = null
    private val retryConnectionCount = AtomicInteger(0)

    companion object {
        const val RECONNECTION = true                    // 재연결 시도 여부
        const val RANDOMIZATION_FACTOR = 1.0             // 지연시간 오차 범위
        const val RECONNECTION_ATTEMPTS = 10             // 재연결 시도 횟수
        const val RECONNECTION_DELAY = 1000L             // 재연결 시도 시간
        const val RECONNECTION_DELAY_MAX = 1000L         // 최대 재연결 시도 시간
    }

    // 로컬 테스트 시 ssl 인증 우회하기 위해 필요합니다. 자체 ssl 인증 서버가 있으면 안써도 됩니다.
    private fun getOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
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
            transports = arrayOf(Polling.NAME, PollingXHR.NAME, WebSocket.NAME)
            callFactory = it
            webSocketFactory = it
        }
        reconnection = RECONNECTION
        randomizationFactor = RANDOMIZATION_FACTOR
        reconnectionAttempts = RECONNECTION_ATTEMPTS
        reconnectionDelay = RECONNECTION_DELAY
        reconnectionDelayMax = RECONNECTION_DELAY_MAX
    }

    fun createSocket(url: String) {
        if (socket != null) return
        socket = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            IO.socket(url, getSocketOptions(getOkHttpClient()))
        } else {
            IO.socket(url, getSocketOptions())
        }
        IO.socket(url, getSocketOptions())
        connectListener()
        socket?.connect()
    }

    protected open fun connectListener() {
        socket?.run {
            on(Socket.EVENT_CONNECT, connectListener)
            on(Socket.EVENT_DISCONNECT, disconnectListener)
            on(Socket.EVENT_RECONNECT, reconnectListener)
            on(Socket.EVENT_RECONNECT_ERROR, reconnectErrorListener)
            on(Socket.EVENT_MESSAGE, messageListener)
        }
    }

    private val connectListener = Emitter.Listener {
        retryConnectionCount.set(0)
        onSocketListener.onSocketState("connect", it)
    }

    private val disconnectListener = Emitter.Listener {
        onSocketListener.onSocketState("disconnect", it)
    }

    private val reconnectListener = Emitter.Listener {
        onSocketListener.onSocketState("reconnect", it)
    }

    private val reconnectErrorListener = Emitter.Listener {
        val connectCount = retryConnectionCount.get()
        if (connectCount >= RECONNECTION_ATTEMPTS - 1) {
            onSocketListener.onSocketState("connectError", emptyArray())
            retryConnectionCount.set(0)
        } else if(connectCount % 5 == 0) {
            onSocketListener.onSocketState("connectRetryError", emptyArray())
        }
        Log.d("webrtcTAG", "socket retry event, retry count = $connectCount")
        retryConnectionCount.set(connectCount + 1)
    }

    private val messageListener = Emitter.Listener {
        onSocketListener.onSocketState("message", it)
    }

    fun disconnectSocket() {
        disconnectListener()
        socket?.disconnect()
        socket = null
    }

    protected open fun disconnectListener() {
        socket?.run {
            off(Socket.EVENT_CONNECT, connectListener)
            off(Socket.EVENT_DISCONNECT, disconnectListener)
            off(Socket.EVENT_RECONNECT, reconnectListener)
            off(Socket.EVENT_RECONNECT_ERROR, reconnectErrorListener)
            off(Socket.EVENT_MESSAGE, messageListener)
        }
    }

    fun emit(event: String, data: Any) {
        socket?.emit(event, data)
    }

    fun emit(event: String, vararg data: Any, onCall: ((args: Array<Any?>) -> Unit)? = null) {
        onCall?.let { function ->
            socket?.emit(event, data) { function.invoke(it) }
        } ?: run {
            socket?.emit(event, data)
        }
    }

}
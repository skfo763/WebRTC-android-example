package com.skfo763.rtc.core

import com.skfo763.rtc.RTCApp
import com.skfo763.rtc.data.BYE
import com.skfo763.rtc.utils.setLogDebug
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.*

open class SignalingClient {

    companion object {
        private const val TEST_ADDRESS = "https://192.168.0.16"
        private const val TEST_PORT = "8889"
        private const val TEST_ROOM_ID = "hi"
    }

    val channel = RTCApp.coChannel.channel
    var socket: Socket? = null
    var socketOnListener: SocketOnListener? = null

    init {
        connect(TEST_ADDRESS, TEST_PORT, TEST_ROOM_ID)
    }

    fun connect(address: String, portNumber: String, roomId: String) =
        CoroutineScope(Dispatchers.Main).launch {
            initSocket(address, portNumber, roomId)
        }

    private fun initSocket(address: String, port : String, roomId : String) {
        try {
            val socketUrl = "$address:$port"
            setLogDebug(socketUrl)

            val hostNameVerifier = HostnameVerifier { _, _ -> true}
            val trustAllCerts = arrayOf<TrustManager>(
                object: X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {}

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {}

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                })

            val trustManager = trustAllCerts[0] as X509TrustManager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, null)

            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
            val okHttpClient = OkHttpClient.Builder()
                .hostnameVerifier(hostNameVerifier)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .build()
            /** SSL SIGNED -> TRUST MANAGER & HOST_NAME_VERIFIER **/

            // socket initialize with options
            IO.Options().apply {
                callFactory = okHttpClient
                webSocketFactory = okHttpClient
            } .run {
                socket = IO.socket(socketUrl, this).run {
                    // create on then connect
                    socketOnListener = SocketOnListener(this, roomId)
                    socketOnListener?.run {
                        on(Socket.EVENT_CONNECT, onJoinToRoomListener)
                        on(Socket.EVENT_CONNECT_ERROR, onErrorReceivedListener)
                        on(Socket.EVENT_MESSAGE, onMessageReceivedListener)
                    } ?: run {
                        // 소켓 리스너 생성 에러
                    }
                    connect()
                }
            }


        } catch (e:Exception){
            // 소켓 연결중 문제가 생겼으니 sokcet 및 peerConnection 관련 전부 해제후
            // 에러 발생을 알린 후 , 되돌아간다.
            // TODO() = 연결 해제 및 뷰(or 상태) 롤백
        }
    }

    fun destroy() {
        socket?.run {
            send(BYE)
            socketOnListener?.run {
                off(Socket.EVENT_CONNECT, onJoinToRoomListener)
                off(Socket.EVENT_CONNECT_ERROR, onErrorReceivedListener)
                off(Socket.EVENT_MESSAGE, onMessageReceivedListener)
            }
            disconnect()
        }
    }
}
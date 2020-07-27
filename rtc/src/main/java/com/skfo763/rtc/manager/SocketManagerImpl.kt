package com.skfo763.rtc.manager

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.skfo763.rtc.contracts.PeerSignalCallback
import com.skfo763.rtc.contracts.StopCallType
import com.skfo763.rtc.data.*
import com.skfo763.socket.contracts.SocketEmitterListener
import com.skfo763.socket.core.SocketHelper
import com.skfo763.socket.core.SocketListenerEvent
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.SocketException
import java.util.*

class SocketManagerImpl(private val peerSignalCallback: PeerSignalCallback): SocketEmitterListener, SocketManager {

    companion object {
        private const val TYPE = "type"
    }

    private val helper = SocketHelper(this)
    private val gson = Gson()

    override fun initializeSocket(url: String) {
        Handler(Looper.getMainLooper()).post {
            helper.initializeSocket(url)
        }
    }

    override fun sendJoinToSocket(userInfo: JSONObject) {
        helper.sendSocket(SocketListenerEvent.EVENT_JOIN, userInfo) {
            try {
                val ackJson = JSONObject("${it[it.size - 1]}")
                if(ackJson.getBoolean("success")) {
                    if(ackJson.getString("status") == FINISHED) {
                        peerSignalCallback.onTerminate(FINISHED)
                    }
                } else {
                    peerSignalCallback.onError(true, message = ackJson.getString("msg"))
                }
            } catch (e: SocketException) {
                peerSignalCallback.onError(false, message = e.message)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    override fun sendOfferAnswerToSocket(sessionDescription: SessionDescription) {
        try {
            val jsonSessionDescription = gson.toJson(sessionDescription)
            val sendData = JSONObject()
            when {
                jsonSessionDescription.toLowerCase(Locale.ROOT).contains(OFFER) -> {
                    sendData.apply {
                        put(TYPE, OFFER)
                        put(SDP, sessionDescription.description)
                    }
                    helper.sendSocket(SocketListenerEvent.EVENT_MESSAGE, sendData)
                }
                jsonSessionDescription.toLowerCase(Locale.ROOT).contains(ANSWER) -> {
                    sendData.apply {
                        put(TYPE, ANSWER)
                        put(SDP, sessionDescription.description)
                    }
                    helper.sendSocket(SocketListenerEvent.EVENT_MESSAGE, sendData)
                }
                else -> {
                    peerSignalCallback.onError(true, message = "Invalid session description")
                }
            }
        } catch (e: Exception) {
            peerSignalCallback.onError(true, message = e.message)
        }
    }

    override fun sendIceCandidateToSocket(iceCandidate: IceCandidate) {
        try {
            val sendData = JSONObject().apply {
                put(TYPE, CANDIDATE)
                put(CANDIDATE, iceCandidate.sdp)
                put(ID, iceCandidate.sdpMid)
                put(LABEL, iceCandidate.sdpMLineIndex)
            }
            helper.sendSocket(SocketListenerEvent.EVENT_MESSAGE, sendData)
        } catch(e: Exception) {
            peerSignalCallback.onError(true, message = e.message)
        }
    }

    override fun sendHangUpEventToSocket(
        data: Any,
        stoppedAt: StopCallType
    ) {
        try {
            val jsonData = gson.toJson(data)
            if(data == HANGUP || jsonData == HANGUP) {
                helper.sendSocket(SocketListenerEvent.EVENT_HANGUP, JSONObject().put(HANGUP, HANGUP)) {
                    val ackJson = JSONObject("${it[it.size-1]}")
                    peerSignalCallback.onHangUp(ackJson)

                    if(ackJson.getBoolean("success")) {
                        peerSignalCallback.onHangUpSuccess(stoppedAt)
                    } else {
                        peerSignalCallback.onError(true, message = HANGUP_FALSE)
                    }
                }
            } else {
                peerSignalCallback.onError(false, message = "data should be hang-up : $data")
            }
        } catch (e: Exception) {
            peerSignalCallback.onError(true, message = e.message)
        }
    }

    override fun sendCommonEventToSocket(data: String) {
        helper.sendSocket(data)
    }

    override fun disconnectSocket() {
        helper.releaseSocket()
    }

    override fun onMessageReceived(message: Any?) {
        try {
            val data = JSONObject(message.toString())
            when(data[TYPE]) {
                OFFER -> {
                    peerSignalCallback.onOfferReceived(
                            SessionDescription(SessionDescription.Type.OFFER, "${data[SDP]}"))
                }
                ANSWER -> {
                    peerSignalCallback.onAnswerReceived(
                            SessionDescription(SessionDescription.Type.ANSWER, "${data[SDP]}"))
                }
                CANDIDATE -> {
                    peerSignalCallback.onIceCandidateReceived(
                            IceCandidate(
                                    data[ID].toString(),
                                    data.getInt(LABEL),
                                    data[CANDIDATE].toString()
                            )
                    )
                }
                else -> {
                    peerSignalCallback.onError(false, message = "unsupported socket message")
                }
            }
        } catch (e: java.lang.Exception){
            e.printStackTrace()
        }
    }

    override fun onConnected(connectData: Array<Any>) {
        peerSignalCallback.onConnected(connectData)
    }

    override fun onReconnected(reconnectData: Array<Any>) {
        peerSignalCallback.onError(false, showMessage = false, message = "${reconnectData[0]}")
    }

    override fun onMatched(match: Array<Any>) {
        match.forEach {
            try {
                val matchData = JSONObject("$it")
                peerSignalCallback.onMatched(matchData)
            } catch (e: java.lang.Exception) {
                peerSignalCallback.onError(true, message = e.message)
            }
        }
    }

    override fun onDisconnected(disconnect: Any?) {
        if(disconnect != null && disconnect == SERVER_DISCONNECT) {
            peerSignalCallback.onError(true, message = SERVER_DISCONNECT)
        }
    }

    override fun onConnectError(retryCount: Int) {
        peerSignalCallback.onError(true, message = RETRY_CONNECT_FAILED_COUNT_OVER)
    }

    override fun onErrorRetry(retryCount: Int) {
        peerSignalCallback.onError(false, message = "Retrying for $retryCount count")
    }

    override fun onWaitingStatus(wait: Array<Any>) {
        wait.forEachIndexed { index, data ->
            val data = JSONObject("$data")
            peerSignalCallback.onWaitingStatusReceived(data)
        }
    }

    override fun onTerminate(terminate: Array<Any>) {
        val data = JSONObject("${terminate[0]}")
        peerSignalCallback.onHangUp(data)
        (data[TERMINATED_CASE] as? String)?.let { peerSignalCallback.onTerminate(it) }
    }
}
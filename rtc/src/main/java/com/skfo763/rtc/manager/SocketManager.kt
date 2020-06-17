package com.skfo763.rtc.manager

import com.google.gson.Gson
import com.skfo763.rtc.contracts.PeerSignalCallback
import com.skfo763.rtc.data.*
import com.skfo763.socket.contracts.EmitterListener
import com.skfo763.socket.core.SocketHelper
import com.skfo763.socket.core.SocketListenerEvent
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.*

class SocketManager(private val peerSignalCallback: PeerSignalCallback): EmitterListener {

    companion object {
        private const val TYPE = "type"
        private const val DEFAULT_MESSAGE = "default_message"

    }

    private val helper = SocketHelper(this)
    private val gson = Gson()

    fun sendJoinToSocket(userInfo: JSONObject) {
        helper.sendSocket(SocketListenerEvent.EVENT_JOIN, userInfo) {
            val ackJson = JSONObject("${it[it.size - 1]}")
            val msg = ackJson.getString("msg")
            if(ackJson.getBoolean("success")) {
                try {
                    if(ackJson.getString("status") == FINISHED) {
                        peerSignalCallback.onTerminate()
                    }
                } catch (e: JSONException) {
                    peerSignalCallback.onError()
                }
            } else {
                peerSignalCallback.onError()
            }
        }
    }

    fun sendOfferAnswerToSocket(sessionDescription: SessionDescription) {
        try {
            val jsonSessionDescription = gson.toJson(sessionDescription)
            val sendData = JSONObject()
            when {
                jsonSessionDescription.toLowerCase(Locale.getDefault()).contains(OFFER) -> {
                    sendData.apply {
                        put(TYPE, OFFER)
                        put(SDP, sessionDescription.description)
                    }
                    helper.sendSocket(SocketListenerEvent.EVENT_MESSAGE, sendData)
                }
                jsonSessionDescription.toLowerCase(Locale.getDefault()).contains(ANSWER) -> {
                    sendData.apply {
                        put(TYPE, ANSWER)
                        put(SDP, sessionDescription.description)
                    }
                    helper.sendSocket(SocketListenerEvent.EVENT_MESSAGE, sendData)
                }
                else -> {
                    peerSignalCallback.onError()
                }
            }
        } catch (e: Exception) {
            peerSignalCallback.onError()
        }
    }

    fun sendIceCandidateToSocket(iceCandidate: IceCandidate) {
        try {
            val sendData = JSONObject().apply {
                put(TYPE, CANDIDATE)
                put(CANDIDATE, iceCandidate.sdp)
                put(ID, iceCandidate.sdpMid)
                put(LABEL, iceCandidate.sdpMLineIndex)
            }
            helper.sendSocket(SocketListenerEvent.EVENT_MESSAGE, sendData)
        } catch(e: Exception) {
            peerSignalCallback.onError()
        }
    }

    fun sendHangUpEventToSocket(data: Any) {
        try {
            val jsonData = gson.toJson(data)
            if(data == HANGUP || jsonData == HANGUP) {
                helper.sendSocket(SocketListenerEvent.EVENT_HANGUP, JSONObject().put(HANGUP, HANGUP)) {
                    val ackJson = JSONObject("${it[it.size-1]}")
                    peerSignalCallback.onHangUp(ackJson)

                    if(ackJson.getBoolean("success")) {
                        peerSignalCallback.onHangUpSuccess()
                    } else {
                        peerSignalCallback.onError()
                    }
                }
            } else {
                peerSignalCallback.onError()
            }
        } catch (e: Exception) {
            peerSignalCallback.onError()
        }
    }

    override fun onMessageReceived(message: Any?) {

    }

    override fun onConnected(connectData: Array<Any>) {
        peerSignalCallback.onConnected()
    }

    override fun onReconnected() {
        TODO("Not yet implemented")
    }

    override fun onMatched(match: Array<Any>) {
        match.forEach {
            try {
                val matchData = JSONObject("$it")
                peerSignalCallback.onMatched(matchData)

                if(matchData.getBoolean(OFFER)) {
                    peerSignalCallback.createOffer()
                } else {
                    peerSignalCallback.createAnswer()
                }
            } catch (e: java.lang.Exception) {
                peerSignalCallback.onError()
            }
        }
    }

    override fun onDisconnected() {
        TODO("Not yet implemented")
    }

    override fun onConnectError() {
        TODO("Not yet implemented")
    }

    override fun onWaitingStatus(wait: Array<Any>) {
        TODO("Not yet implemented")
    }

    override fun onTerminate(terminate: Array<Any>) {
        val data = JSONObject("${terminate[0]}")
        peerSignalCallback.onHangUp(data)
        (data[TERMINATED_CASE] as? String)?.let { peerSignalCallback.onTerminate(it) }
    }


    private fun handleRtcEvent(data: JSONObject) {
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
                peerSignalCallback.onError()
            }
        }
    }
}
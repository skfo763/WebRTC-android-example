package com.skfo763.rtc.core

import com.google.gson.Gson
import com.skfo763.rtc.RTCApp
import com.skfo763.rtc.data.*
import com.skfo763.rtc.utils.setLogDebug
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

open class SocketOnListener(val socket: Socket, val roomId: String) {

    val channel = RTCApp.coChannel
    private val gson = Gson()

    val onJoinToRoomListener = Emitter.Listener {
        socket.run {
            emit(CREATE_OR_JOIN, roomId)
            emit(Socket.EVENT_MESSAGE, GOT_USER_MEDIA)
        }
    }

    val onMessageReceivedListener = Emitter.Listener {
        it.forEach { msg -> processReceivedMessage(msg) }
    }

    val onErrorReceivedListener = Emitter.Listener {

    }

    fun sendRTCinfo(dataObject: Any?) = runBlocking {
        if(dataObject == null) { setLogDebug("data is null"); return@runBlocking }
        socket.run {
            try {
                val rtcJson = gson.toJson(dataObject).toLowerCase()

                when {
                    rtcJson.contains("offer") -> {
                        emitSessionDescription(dataObject, "offer")
                    }
                    rtcJson.contains("answer") -> {
                        emitSessionDescription(dataObject, "answer")

                    }
                    rtcJson.contains("candidate") -> {
                        val can = (dataObject as IceCandidate)
                        val jsonObject = JSONObject().apply {
                            put("type", "candidate")
                            put("candidate", can.sdp)
                            put("id", can.sdpMid)
                            put("label", can.sdpMLineIndex)
                        }
                        emit("message", jsonObject)
                    }
                    else -> {
                        setLogDebug("wrong type is $dataObject")
                    }
                }
            } catch (e: Exception) {
                setLogDebug("error is $e")
            }
        }
    }

    private fun Emitter.emitSessionDescription(dataObject: Any, type: String) {
        if(dataObject !is SessionDescription) return
        val jsonObject = JSONObject().apply {
            put("type", type)
            put("sdp", dataObject.description)
        }

        setLogDebug("data : $dataObject")
        emit(Socket.EVENT_MESSAGE, jsonObject)
    }

    private fun processReceivedMessage(data: Any) {
        setLogDebug("received data : $data")

        when(data) {
            is String -> {
                handleStringMessage(data)
            }
            else -> {
                castAndHandleMessage(data)
            }
        }

    }

    private fun handleStringMessage(data: String) {
        when(data) {
            GOT_USER_MEDIA -> channel.run { runMain { sendString(CREATE_OFFER) } }
            BYE -> channel.run { runMain { sendString(DESTROY) } }
        }
    }

    private fun castAndHandleMessage(data: Any) {
        try {
            val info = JSONObject("$data")
            when(val type = info["type"]) {
                OFFER -> {
                    var sdp = info["sdp"]
                    var sessionDescription = SessionDescription(SessionDescription.Type.OFFER, "$sdp")
                    channel.run {
                        setLogDebug("send offer")
                        runMain { sendSessionDescription(OFFER, sessionDescription) }
                    }
                }
                ANSWER -> {
                    var sdp = info["sdp"]
                    var sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, "$sdp")
                    channel.run {
                        setLogDebug("send offer")
                        runMain { sendSessionDescription(ANSWER, sessionDescription) }
                    }
                }
                CANDIDATE -> {
                    val iceCandidate = IceCandidate(
                        "${info["id"]}",
                        info.getInt("label"),
                        "${info["candidate"]}"
                    )
                    channel.run { runMain { sendCandidate(iceCandidate) } }
                }
            }
        } catch (e: Exception) {
            setLogDebug("$e")
        }
    }
}
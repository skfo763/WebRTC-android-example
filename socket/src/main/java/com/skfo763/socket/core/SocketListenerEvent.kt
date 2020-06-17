package com.skfo763.socket.core

import io.socket.client.Socket

enum class SocketListenerEvent(val value: String) {
    EVENT_MESSAGE(Socket.EVENT_MESSAGE),
    EVENT_CONNECT(Socket.EVENT_CONNECT),
    EVENT_DISCONNECT(Socket.EVENT_DISCONNECT),
    EVENT_RECONNECT(Socket.EVENT_RECONNECT),
    EVENT_RECONNECT_ERROR(Socket.EVENT_RECONNECT_ERROR),
    EVENT_MATCHED("matched"),
    EVENT_TERMINATED("terminated"),
    EVENT_WAITING_STATUS("waiting_status"),
    EVENT_HANGUP("hangup"),
    EVENT_JOIN("join")
}
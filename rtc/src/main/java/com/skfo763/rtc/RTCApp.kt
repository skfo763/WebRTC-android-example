package com.skfo763.rtc

import com.skfo763.rtc.data.SessionDescriptionsType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class RTCApp {
    companion object {
        @JvmStatic
        val coChannel = CoChannel()
    }
}

open class CoChannel {
    val channel: BroadcastChannel<Any> = ConflatedBroadcastChannel()
//  val channel: BroadcastChannel<String> = ConflatedBroadcastChannel()
//  val channelDescription: BroadcastChannel<SessionDescription> = ConflatedBroadcastChannel()
//  val channelCandidate: BroadcastChannel<IceCandidate> = ConflatedBroadcastChannel()

    fun runMain(work: suspend (() -> Unit)) = CoroutineScope(Dispatchers.Main).launch { work() }

    // 이벤트를 보낸다.
    fun sendString(o: String) = run {
        runMain { channel.send(o) }
    }

    fun sendSessionDescription(
        type : String,
        sDescription : SessionDescription
    ) = run {
        runMain { channel.send(
            SessionDescriptionsType(
                type,
                sDescription
            )
        ) }
    }

    fun sendCandidate(
        candidate: IceCandidate
    ) = run {
        runMain { channel.send(candidate) }
    }

    fun sendMediaStream(
        mStream: MediaStream?
    ) = run {
        runMain { mStream?.let { channel.send(it) } }
    }


//    fun send(o: List<GPSData>) {
//        Coroutines.main {
//            bus.send(o)
//        }
//    }
//
//    fun send(o: LatLng) {
//        Coroutines.main {
//            bus.send(o)
//        }
//    }
//
//    inline fun <reified T> asChannel(): ReceiveChannel<T> {
//        return bus.openSubscription().filter { it is T }.map { it as T }
//    }
}


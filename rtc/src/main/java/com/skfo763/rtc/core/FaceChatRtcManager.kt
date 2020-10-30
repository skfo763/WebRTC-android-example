package com.skfo763.rtc.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.skfo763.rtc.R
import com.skfo763.rtc.contracts.*
import com.skfo763.rtc.data.*
import com.skfo763.rtc.inobs.PeerConnectionObserver
import com.skfo763.rtc.manager.*
import com.skfo763.rtc.manager.audio.MAudioManager
import com.skfo763.rtc.manager.audio.MAudioManagerImpl
import org.json.JSONObject
import org.webrtc.*
import java.util.concurrent.atomic.AtomicBoolean

class FaceChatRtcManager private constructor(
        context: Context
) : PeerConnectionObserver(), OnSocketListener {

    companion object {
        fun createFaceChatRtcManager(context: Context): FaceChatRtcManager {
            return FaceChatRtcManager(context)
        }
    }

    lateinit var iFaceChatViewModelListener: IFaceChatViewModelListener

    private val isStart = AtomicBoolean(false)
    private val isReleased = AtomicBoolean(false)
    private var otherUserIdx: Int? = null
    private val gson = Gson()

    private val socketManager = RTCSocketManager(this)
    private val peerManager = VideoPeerManager(context, this)
    private val audioManager: MAudioManager = MAudioManagerImpl(context)
    private val retryErrorMsg = context.getString(R.string.face_chat_retry_connection_error_msg)

    var callingRemoteView: SurfaceViewRenderer? = null
        set(value) {
            value?.let {
                initSurfaceView(it, true)
            }
            field = value
        }

    // 액티비티 실행 시 최초 1회
    fun initializeVideoTrack() {
        peerManager.addTrackToStream()
    }

    // onStart 시점에서 호출
    fun startCameraCapturer() {
        peerManager.startCameraCapture()
    }

    // 각 프래그먼트 생성 시 최초 1회
    fun initSurfaceView(surfaceView: SurfaceViewRenderer, isRemote: Boolean = false) {
        surfaceView.setMirror(!isRemote)
        peerManager.initSurfaceView(surfaceView)
    }

    // 프래그먼트 트랜지션마다 호출 - 새로 띄워지는 프래그먼트의 서페이스뷰를 넣어준다.
    fun attachVideoTrackToLocalSurface(surfaceView: SurfaceViewRenderer) {
        peerManager.attachLocalTrackToSurface(surfaceView)
    }

    // 시그널링 서버 갈아끼워질때마다 (페이스챗 1 사이클 돌 때마다)
    fun setPeerInfo(peer: SignalServerInfo) {
        peerManager.setIceServer(peer)
        peerManager.addStreamToPeerConnection()
        socketManager.createSocket(peer.signalServerHost)
    }

    // 프래그먼트 트랜지션마다 호출 - 내려가는 프래그먼트의 서페이스뷰를 넣어준다.
    fun detachVideoTrackFromLocalSurface(surfaceView: SurfaceViewRenderer) {
        peerManager.detachLocalTrackFromSurface(surfaceView)
    }

    fun detachVideoTrackFromRemoteSurface(surfaceView: SurfaceViewRenderer) {
        peerManager.stopRemotePreviewRendering(surfaceView)
    }

    // 통화 종료 시마다 호출 - 여러 케이스 있을텐데 통합해서 이건 다 불러주도록 합니다.
    private fun releaseSocket(doAfterRelease: (() -> Unit)? = null) {
        Handler(Looper.getMainLooper()).post {
            isStart.set(false)
            isReleased.set(true)

            socketManager.run {
                otherUserIdx = null
                disconnectSocket()
            }
            peerManager.closePeer()
            doAfterRelease?.invoke()
        }
    }

    // 액티비티 종료 시 호출
    fun disposePeer() {
        peerManager.apply {
            removeStreamFromPeerConnection()
            removeTrackFromStream()
            stopCameraCapture()
            disconnectPeer()
        }
        audioManager.audioFocusLoss()
    }

    override fun onPeerCreate(desc: SessionDescription?) {
        socketManager.sendOfferAnswerToSocket(desc!!)
    }

    override fun onPeerError(isCritical: Boolean, showMessage: Boolean, message: String?) {
        this.onError(isCritical, showMessage, message)
    }

    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        super.onIceCandidate(iceCandidate)
        iceCandidate?.let {
            socketManager.sendIceCandidateToSocket(it)
            peerManager.addIceCandidate(it)
        } ?: kotlin.run {
            onPeerError(isCritical = false, showMessage = false, message = "Ice candidate data is null")
        }
    }

    override fun onAddStream(mediaStream: MediaStream?) {
        super.onAddStream(mediaStream)
        Log.d("webrtcTAG", "onAddStream")
        audioManager.audioFocusDucking()

        if (callingRemoteView != null && mediaStream != null) {
            peerManager.startRemoteVideoCapture(callingRemoteView!!, mediaStream)
        }
    }

    fun stopCallSignFromClient(stoppedAt: StopCallType) {
        when(stoppedAt) {
            StopCallType.STOP_WAITING -> releaseSocket { hangUpSuccess(stoppedAt) }
            StopCallType.GO_TO_INTRO -> releaseSocket {
                hangUpSuccess(stoppedAt)
            }
            StopCallType.QUIT_ACTIVITY -> releaseSocket {
                Log.d("webrtcTAG", "stop at destroy activity")
            }
            else -> socketManager.sendHangUpEventToSocket(HANGUP, { hangUp(it)} ) { hangUpSuccess(stoppedAt) }
        }
    }

    private fun hangUp(data: JSONObject) = Unit

    private fun hangUpSuccess(stoppedAt: StopCallType) {
        Log.d("webrtcTAG", "onHangUpSuccess")
        val uiEvent = when (stoppedAt) {
            StopCallType.QUIT_ACTIVITY -> RtcUiEvent.FINISH
            StopCallType.GO_TO_INTRO -> RtcUiEvent.FINISH
            StopCallType.STOP_WAITING -> RtcUiEvent.FINISH
            else -> RtcUiEvent.STOP_PROCESS_COMPLETE
        }
        releaseSocket {
            iFaceChatViewModelListener.onUiEvent(uiEvent)
        }
    }

    private fun onError(shouldClosePeer: Boolean, showMessage: Boolean, message: String?) {
        if (shouldClosePeer) {
            iFaceChatViewModelListener.onError(ErrorHandleData(true, message ?: "", showMessage))
        }
        Log.d("webrtcTAG", "message = $message, showMessage = $showMessage")
    }

    override fun onSocketState(state: String, data: Array<Any>, onComplete: (() -> Unit)?) {
        when (state) {
            "connect" -> onSocketConnected(data)
            "message" -> onSocketMessage(data)
            "matched" -> data.forEach { onSocketMatched(gson.fromJson("$it", MatchModel::class.java)) }
            "waiting_status" -> data.forEach { waitingStatusReceived(JSONObject("$it")) }
            "terminated" -> onTerminated(data)
            "disconnect" -> onError(false, false, message = SERVER_DISCONNECT)
            "reconnect" -> onError(false, showMessage = false, message = "${data[0]}")
            "connectRetryError" -> iFaceChatViewModelListener.onUiEvent(RtcUiEvent.RETRY)
            "connectError" -> onError(true, true, message = retryErrorMsg)
        }
    }

    private fun onSocketConnected(data: Array<Any>) {
        Log.d("webrtcTAG", "onSocketConnected")
        val userInfo = iFaceChatViewModelListener.getUserInfo()
        val authInfo = JSONObject().apply {
            put(TOKEN, userInfo.token)
            put(PASSWORD, userInfo.password)
            put(SKIN, "none")
            if (isStart.get()) {
                put(STATUS, MATCHED)
                put(OTHER, otherUserIdx)
            }
        }
        socketManager.socketJoin(authInfo) {
            terminate(it)
        }
    }

    private fun onSocketMessage(message: Array<Any>) {
        try {
            val data = JSONObject(message[0].toString())
            when (data["type"]) {
                OFFER -> onSocketOfferReceived(SessionDescription(SessionDescription.Type.OFFER, "${data[SDP]}"))
                ANSWER -> onSocketAnswerReceived(SessionDescription(SessionDescription.Type.ANSWER, "${data[SDP]}"))
                CANDIDATE -> onSocketIceCandidateReceived(IceCandidate(data[ID].toString(), data.getInt(LABEL), data[CANDIDATE].toString()))
                else -> onError(shouldClosePeer = false, showMessage = false, message = "unsupported socket message")
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun onSocketOfferReceived(description: SessionDescription) {
        Log.d("webrtcTAG", "onOfferReceived")
        peerManager.run {
            onRemoteSessionReceived(description)
            callAnswer()
        }
    }

    private fun onSocketAnswerReceived(description: SessionDescription) {
        Log.d("webrtcTAG", "onSocketAnswerReceived")
        peerManager.onRemoteSessionReceived(description)
    }

    private fun onSocketIceCandidateReceived(iceCandidate: IceCandidate) {
        Log.d("webrtcTAG", "onSocketIceCandidateReceived")
        peerManager.addIceCandidate(iceCandidate)
    }

    private fun onSocketMatched(data: MatchModel) {
        try {
            Log.d("webrtcTAG", "onSocketMatched")
            this.otherUserIdx = data.otherIdx
            if(!isStart.get()) {
                if (data.isOffer) createOffer()
                else createAnswer()

                iFaceChatViewModelListener.onUiEvent(RtcUiEvent.START_CALL)
                isStart.set(true)
                iFaceChatViewModelListener.onMatched(data)
            }
        } catch (e: Exception) {
            onPeerError(true, showMessage = true, message = e.message)
        }
    }

    private fun createOffer() {
        Log.d("webrtcTAG", "createOffer")
        peerManager.callOffer()
        socketManager.sendEventToSocket(CALL_STARTED)
    }

    private fun createAnswer() {
        Log.d("webrtcTAG", "createAnswer")
        peerManager.callAnswer()
        socketManager.sendEventToSocket(CALL_STARTED)
    }

    private fun waitingStatusReceived(data: JSONObject) {
        iFaceChatViewModelListener.updateWaitInfo(data.getString(WAITING_TEXT))
    }

    private fun onTerminated(data: Array<Any>) {
        var terminateCase = HANGUP
        var message: String? = null
        try {
            val jsonObject = JSONObject("${data[0]}")
            terminateCase = jsonObject.getString(TERMINATED_CASE) ?: HANGUP
            message = jsonObject.getString("msg")
            hangUp(jsonObject)
        } catch (e: Exception) {
            onError(true, showMessage = false, message = e.message)
        } finally {
            terminate(terminateCase, message)
        }
    }

    private fun terminate(state: String, message: String? = null) {
        when (state) {
            TIMEOUT, DISCONNECTION, HANGUP -> {
                releaseSocket {
                    iFaceChatViewModelListener.onUiEvent(RtcUiEvent.STOP_PROCESS_COMPLETE, message)
                }
            }
            else -> {
                iFaceChatViewModelListener.onError(ErrorHandleData(true, "", true))
            }
        }
    }

    fun changeCameraFacing(handler: CameraVideoCapturer.CameraSwitchHandler) {
        peerManager.changeCameraFacing(handler)
    }

}
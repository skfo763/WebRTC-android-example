package com.skfo763.rtc.manager.audio

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/**
 *  블루투스와 헤드셋(헤드폰) 이 하나의 이미지로 합쳐지면서 따로따로 구분 할 필요가 없어짐에 따라
 *  헤드폰이나 블루투스에 해당하면 바로 이어폰구분으로 하기로 변경
 *  --> 하나만 연결되어있는경우 , no headset 이 bluetooth 를 덮는 경우가 생겨 둘을 재분리 함.
 *
 *
 *
 *  @param getDeviceTypeNow() : detect now audio connect
 *  @param ConnectAudioDetectReceiver : recevier detect about audio connect
 */
class AudioListener(
    val context: Context,
    val audioStatusInterface: AudioStatusInterface
) {

    companion object {
        const val BLUETOOTH_CONNECTED = "android.bluetooth.device.action.ACL_CONNECTED"
        const val BLUETOOTH_DISCONNECTED = "android.bluetooth.device.action.ACL_DISCONNECTED"
        const val HEADSET_STATE = "state"
    }

    var audioM: AudioManager = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)

    var connectAudioDetectReceiver: BroadcastReceiver? = null
    var connectStatus: AudioConnect = AudioConnect.NONE

    /**
     *  분류가 기본 or 헤드셋(블루투스/헤드셋/헤드폰) 으로 변경됨에 따라 통일
     */
    enum class AudioConnect {
        NONE,
        EARPIECE,
        EARPIECE_WIRE_CONNECTED,
        EARPIECE_WIRE_DISCONNECTED,
        EARPIECE_NO_WIRE_CONNECTED,
        EARPIECE_NO_WIRE_DISCONNECTED,
    }

    fun unRegisterAudioListener() {
        audioM.mode = AudioManager.MODE_NORMAL

        // 종료시 스피커모드가 true 그대로면 추후에도 유지되는걸로 보임.
        if (audioM.isSpeakerphoneOn) audioM.isSpeakerphoneOn = false

        connectAudioDetectReceiver?.let { nullCheckReceiver ->
            try {
                context.unregisterReceiver(nullCheckReceiver)
                connectAudioDetectReceiver = null
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun registerAudioListener() {
        val receiverFilter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BLUETOOTH_CONNECTED)
            addAction(BLUETOOTH_DISCONNECTED)
        }

        connectAudioDetectReceiver?.let {
            context.registerReceiver(connectAudioDetectReceiver, receiverFilter)
        } ?: kotlin.run {
            connectAudioDetectReceiver = ConnectAudioDetectReceiver()
            context.registerReceiver(connectAudioDetectReceiver, receiverFilter)
        }

        // 처음 들어갔는데 스피커모드가 바로 되어있다면 스피커 모드를 해제해준다.
        if (audioM.isSpeakerphoneOn) audioM.isSpeakerphoneOn = false

        getDeviceTypeNow()
    }

    fun getDeviceTypeNow() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (audioM.isWiredHeadsetOn) {
                changeAudioConnectStatus(true, AudioConnect.EARPIECE_WIRE_CONNECTED)
            } else if (audioM.isBluetoothScoOn || audioM.isBluetoothA2dpOn) {
                changeAudioConnectStatus(true, AudioConnect.EARPIECE_NO_WIRE_CONNECTED)
            } else {
                changeAudioConnectStatus(true, AudioConnect.NONE)
            }
        } else {
            val devices: Array<AudioDeviceInfo> = audioM.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var tempAudioConnect = AudioConnect.NONE
            for (i in devices.indices) {
                val device: AudioDeviceInfo = devices[i]
                if (device.type === AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type === AudioDeviceInfo.TYPE_USB_HEADSET ||
                    device.type === AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    tempAudioConnect = AudioConnect.EARPIECE_WIRE_CONNECTED
                } else if (device.type === AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type === AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    tempAudioConnect = AudioConnect.EARPIECE_NO_WIRE_CONNECTED
                }
            }
            changeAudioConnectStatus(true, tempAudioConnect)
        }
    }

    fun ConnectAudioDetectReceiver() = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context?.let { con ->
                intent?.let { intent_ ->
                    when (intent_.action) {
                        Intent.ACTION_HEADSET_PLUG -> {
                            when (intent.getIntExtra(HEADSET_STATE, -1)) {
                                0 -> {
                                    if (connectStatus == AudioConnect.NONE) else
                                        changeAudioConnectStatusTerminal(AudioConnect.EARPIECE_WIRE_DISCONNECTED)
                                }
                                1 -> {
                                    changeAudioConnectStatusTerminal(AudioConnect.EARPIECE_WIRE_CONNECTED)
                                }
                            }
                        }
                        BLUETOOTH_CONNECTED -> {
                            changeAudioConnectStatusTerminal(AudioConnect.EARPIECE_NO_WIRE_CONNECTED)
                        }
                        BLUETOOTH_DISCONNECTED -> {
                            if (connectStatus == AudioConnect.NONE) else
                                changeAudioConnectStatusTerminal(AudioConnect.EARPIECE_NO_WIRE_DISCONNECTED)
                        }
                    }
                }
            }
        }
    }


    /**
     *  오디오 연결 상태 변경에 대한 구분을 지어주는 로직
     */
    fun changeAudioConnectStatusTerminal(status: AudioConnect) {
        var tempStatus = status
        var isToChange = false

        when (status) {
            AudioConnect.EARPIECE_NO_WIRE_DISCONNECTED -> {
                if (connectStatus == AudioConnect.EARPIECE_WIRE_CONNECTED) {
                    tempStatus = AudioConnect.EARPIECE
                } else {
                    isToChange = true
                    tempStatus = AudioConnect.NONE
                }
            }
            AudioConnect.EARPIECE_WIRE_DISCONNECTED -> {
                if (connectStatus == AudioConnect.EARPIECE_NO_WIRE_CONNECTED) {
                    tempStatus = AudioConnect.EARPIECE
                } else {
                    isToChange = true
                    tempStatus = AudioConnect.NONE
                }
            }
            AudioConnect.EARPIECE_NO_WIRE_CONNECTED -> {
                if (connectStatus == AudioConnect.EARPIECE_WIRE_CONNECTED) {
                    tempStatus = AudioConnect.EARPIECE_NO_WIRE_CONNECTED
                } else {
                    isToChange = true
                    tempStatus = AudioConnect.EARPIECE_NO_WIRE_CONNECTED
                }
            }
            AudioConnect.EARPIECE_WIRE_CONNECTED -> {
                if (connectStatus == AudioConnect.EARPIECE_NO_WIRE_CONNECTED) {
                    tempStatus = AudioConnect.EARPIECE_WIRE_CONNECTED
                } else {
                    isToChange = true
                    tempStatus = AudioConnect.EARPIECE_WIRE_CONNECTED
                }
            }
            else -> {
                isToChange = true
                tempStatus = AudioConnect.NONE
            }
        }

        changeAudioConnectStatus(isToChange, tempStatus)
    }

    /**
     *  오디오 연결 상태 변경에 대한 구분을 지어주는 최종 메소드
     */
    fun changeAudioConnectStatus(toChange: Boolean, status: AudioConnect) {
        audioM.isSpeakerphoneOn = false

        if (toChange) {
            audioStatusInterface.setStatusAudioStatus(status)
        }
        connectStatus = status
    }


}
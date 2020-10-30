package com.skfo763.rtcandroid_example.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.skfo763.rtc.contracts.StopCallType
import com.skfo763.rtc.manager.audio.AudioListener
import com.skfo763.rtc.manager.audio.AudioStatusInterface
import com.skfo763.rtcandroid_example.R
import com.skfo763.rtcandroid_example.base.BaseFragment
import com.skfo763.rtcandroid_example.base.FragmentType
import com.skfo763.rtcandroid_example.databinding.FragmentCallingBinding
import com.skfo763.rtcandroid_example.viewmodel.MainViewModel

class CallingFragment : BaseFragment(), AudioStatusInterface {

    companion object {
        fun newInstance(): CallingFragment {
            return CallingFragment()
        }
    }

    private val sharedViewModel: MainViewModel by activityViewModels()

    private val audioListener by lazy { AudioListener(requireActivity().applicationContext, this) }

    lateinit var binding: FragmentCallingBinding

    override fun onPageChanged() {
        sharedViewModel.rtcModule.attachVideoTrackToLocalSurface(binding.callingLocal)
        audioListener.audioM.isSpeakerphoneOn = true
    }

    override fun onPageHideStart(nextPage: FragmentType) {
        audioListener.audioM.isSpeakerphoneOn = false
        sharedViewModel.rtcModule.detachVideoTrackFromLocalSurface(binding.callingLocal)
        sharedViewModel.rtcModule.detachVideoTrackFromRemoteSurface(binding.callingRemote)
    }

    override fun goBackConfirmed() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.back_confirm_title)
            .setMessage(R.string.back_confirm_message)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                sharedViewModel.rtcModule.stopCallSignFromClient(StopCallType.GO_TO_INTRO)
                sharedViewModel.rtcModule.detachVideoTrackFromLocalSurface(binding.callingLocal)
                dialog.dismiss()
            }.setNegativeButton(R.string.deny) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onResume() {
        audioListener.registerAudioListener()
        super.onResume()
    }

    override fun onPause() {
        audioListener.unRegisterAudioListener()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.callingLocal.release()
        binding.callingRemote.release()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_calling, container, false)
        binding.lifecycleOwner = this
        binding.parentViewModel = sharedViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel.rtcModule.initSurfaceView(binding.callingLocal)
        sharedViewModel.rtcModule.callingRemoteView = binding.callingRemote
    }

    override fun setStatusAudioStatus(status: AudioListener.AudioConnect) {
        when(status) {
            AudioListener.AudioConnect.EARPIECE_WIRE_CONNECTED,
            AudioListener.AudioConnect.EARPIECE_NO_WIRE_CONNECTED -> {
                showToast(getString(R.string.earpiece_connected))
            }
            AudioListener.AudioConnect.EARPIECE_WIRE_DISCONNECTED,
            AudioListener.AudioConnect.EARPIECE_NO_WIRE_DISCONNECTED -> {
                showToast(getString(R.string.earpiece_disconnected))
            }
        }
    }

}
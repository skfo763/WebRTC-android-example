package com.skfo763.rtcandroid_example.view

import android.os.Bundle
import android.util.Log
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
import com.skfo763.rtcandroid_example.databinding.FragmentCallWaitingBinding
import com.skfo763.rtcandroid_example.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_call_waiting.*

class CallWaitingFragment : BaseFragment() {

    companion object {
        fun newInstance(): CallWaitingFragment {
            return CallWaitingFragment()
        }
    }

    lateinit var binding: FragmentCallWaitingBinding

    private val sharedViewModel: MainViewModel by activityViewModels()

    override fun onPageChanged() {
        sharedViewModel.rtcModule.attachVideoTrackToLocalSurface(binding.waitingSurface)
    }

    override fun onPageHideStart(nextPage: FragmentType) {
        sharedViewModel.rtcModule.detachVideoTrackFromLocalSurface(binding.waitingSurface)
    }

    override fun goBackConfirmed() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.back_confirm_title)
            .setMessage(R.string.back_confirm_message)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                sharedViewModel.rtcModule.stopCallSignFromClient(StopCallType.GO_TO_INTRO)
                sharedViewModel.rtcModule.detachVideoTrackFromLocalSurface(binding.waitingSurface)
                dialog.dismiss()
            }.setNegativeButton(R.string.deny) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_call_waiting, container, false)
        binding.lifecycleOwner = this
        binding.parentViewModel = sharedViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel.rtcModule.initSurfaceView(binding.waitingSurface)
        sharedViewModel.rtcModule.attachVideoTrackToLocalSurface(binding.waitingSurface)
    }
}
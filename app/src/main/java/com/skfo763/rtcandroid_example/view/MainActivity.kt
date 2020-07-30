package com.skfo763.rtcandroid_example.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.skfo763.rtc.contracts.IFaceChatViewModelListener
import com.skfo763.rtc.contracts.VoiceChatUiEvent
import com.skfo763.rtc.data.UserJoinInfo
import com.skfo763.rtcandroid_example.R
import com.skfo763.rtcandroid_example.base.BaseFragment
import com.skfo763.rtcandroid_example.viewmodel.MainViewModel
import com.skfo763.rtcandroid_example.viewmodel.ViewModelFactories
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), IFaceChatViewModelListener {

    companion object {
        const val TYPE_CALLING = 0
        const val TYPE_WAITING = 1
        const val REQUEST_CODE_PERMISSION = 1001
    }

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelFactories(this, this)
        ).get(MainViewModel::class.java)
    }

    private val fragmentList: List<BaseFragment> by lazy {
        listOf(CallingFragment.newInstance(), CallWaitingFragment.newInstance())
    }

    private val pageChangeCallback = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewPager2.isUserInputEnabled = position == TYPE_CALLING
            fragmentList[position].onPageChangedComplete()
        }

        override fun onPageScrollStateChanged(state: Int) {
            when(state) {
                ViewPager2.SCROLL_STATE_IDLE -> {

                }
                ViewPager2.SCROLL_STATE_DRAGGING -> {

                }
                else -> {

                }
            }
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel.token = intent.getStringExtra("token") ?: ""
        viewPager2.isUserInputEnabled = false

        requestPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    }

    private fun setFragmentViewPager() {
        viewPager2.apply {
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            viewPager2.offscreenPageLimit = 2
            adapter = PagerAdapter(this@MainActivity, fragmentList)
            registerOnPageChangeCallback(pageChangeCallback)
            setCurrentItem(TYPE_WAITING, false)
        }
    }

    private fun observeLiveData() {
        viewModel.apply {
            fragmentType.observe(this@MainActivity, Observer {
                viewPager2.setCurrentItem(it, false)
            })

        }
    }

    override fun updateWaitInfo(text: String) {


    }

    override fun onUiEvent(uiEvent: VoiceChatUiEvent) {
        runOnUiThread {
            when(uiEvent) {
                VoiceChatUiEvent.START_CALL -> {
                    viewPager2.setCurrentItem(TYPE_CALLING, false)
                }
            }
        }
    }

    override fun onError(e: Any) {
        runOnUiThread {
            if(this.isFinishing) return@runOnUiThread
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Unexpected error has occurred. Please retry in few seconds")
                .setCancelable(false)
                .setPositiveButton("Back") { dialog, _ -> dialog.dismiss(); finish() }
                .create()
                .show()
        }
    }

    override fun callUserInfo(): UserJoinInfo {
        return viewModel.getUserJoinInfo()
    }

    override fun sendTimerAndIdx(duration: Int, otherIdx: Int) {
        Log.d("hellohello", "sendTimerAndIdx")
    }

    override fun sendFinishInfo(displayRating: Boolean?, matchIdx: Int?) {


    }


    // Adapter class for viewpager2
    private class PagerAdapter(
        activity: AppCompatActivity,
        private val fragmentList: List<Fragment>
    ): FragmentStateAdapter(activity) {

        override fun getItemCount() = fragmentList.size

        override fun createFragment(position: Int) = fragmentList[position]

    }


    // Camera/Audio permission process
    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(vararg permissions: String) {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(!isPermissionGranted(Manifest.permission.CAMERA)
            || !isPermissionGranted(Manifest.permission.RECORD_AUDIO)
            || requestCode != REQUEST_CODE_PERMISSION
        ) {
            AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("You cannot use service due to denying of permission.")
                .setCancelable(false)
                .setPositiveButton("Back") { dialog, _ -> dialog.dismiss(); finish() }
                .create()
                .show()
        } else {
            setFragmentViewPager()
            observeLiveData()
        }
    }

    fun goToCallFragment() {
        viewPager2.setCurrentItem(TYPE_CALLING, false)
    }
}
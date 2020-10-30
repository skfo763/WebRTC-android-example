package com.skfo763.rtcandroid_example.view

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.skfo763.rtcandroid_example.R
import com.skfo763.rtcandroid_example.base.BaseFragment
import com.skfo763.rtcandroid_example.base.FragmentType
import com.skfo763.rtcandroid_example.base.MainActivityUseCase
import com.skfo763.rtcandroid_example.databinding.ActivityMainBinding
import com.skfo763.rtcandroid_example.utils.isPermissionGranted
import com.skfo763.rtcandroid_example.viewmodel.MainViewModel

class MainActivity : AppCompatActivity(), MainActivityUseCase {

    companion object {
        const val REQUEST_CODE_PERMISSION = 1001
    }

    private lateinit var binding: ActivityMainBinding

    private val viewModel by lazy {
        ViewModelProvider(this, MainViewModel.Factory(this, this)).get(MainViewModel::class.java)
    }

    private val fragmentList: List<BaseFragment> by lazy {
        listOf(CallingFragment.newInstance(), CallWaitingFragment.newInstance())
    }

    override val token: String get() = intent.extras?.getString("token", "") ?: ""

    private val pageChangeCallback = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            binding.viewPager2.isUserInputEnabled = position == FragmentType.TYPE_CALLING.index
            fragmentList[position].onPageChangeComplete()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.viewPager2.isUserInputEnabled = false
        observeLiveData()
        requestPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    }

    private fun setFragmentViewPager() {
        binding.viewPager2.apply {
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            offscreenPageLimit = 2
            adapter = PagerAdapter(this@MainActivity, fragmentList)
            registerOnPageChangeCallback(pageChangeCallback)
            setCurrentItem(FragmentType.TYPE_WAITING.index, false)
        }
    }

    private fun observeLiveData() {
        viewModel.apply {
            fragmentType.observe(this@MainActivity, Observer {
                val prevPosition = binding.viewPager2.currentItem
                if(it.first.index == prevPosition) return@Observer
                fragmentList[prevPosition].onPageHideStartFromActivity(it.first)
                binding.viewPager2.setCurrentItem(it.first.index, it.second)
            })
        }
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
                .setPositiveButton("Back") { dialog, _ -> dialog.dismiss(); finishActivity() }
                .create()
                .show()
        } else {
            viewModel.initRtcModule()
            setFragmentViewPager()
        }
    }

    private class PagerAdapter(
        activity: AppCompatActivity,
        private val fragmentList: List<Fragment>
    ): FragmentStateAdapter(activity) {

        override fun getItemCount() = fragmentList.size

        override fun createFragment(position: Int) = fragmentList[position]

    }

    override fun vibrate(milliSecond: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliSecond, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(milliSecond)
        }
    }

    override fun showTopBanner(stringResId: Int, colorResId: Int) {
        runOnUiThread {
            showAnimTopBanner(getString(stringResId), resources.getColor(colorResId))
        }
    }

    override fun showTopBanner(message: String, resId: Int) {
        runOnUiThread {
            showAnimTopBanner(message, resources.getColor(resId))
        }
    }

    override fun showToast(text: String) {
        Snackbar.make(binding.rootView, text, Snackbar.LENGTH_SHORT)
    }

    override fun finishActivity() {
        this.finish()
    }

    private fun showAnimTopBanner(message: String, @ColorInt color: Int) {
        val animate = TranslateAnimation(0f, 0f, 0f, -binding.banner.height.toFloat()).apply {
            duration = 2000
            fillAfter = true
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) = Unit
                override fun onAnimationStart(animation: Animation?) = run { binding.banner.visibility = View.VISIBLE }
                override fun onAnimationEnd(animation: Animation?) = run { binding.banner.visibility = View.INVISIBLE }
            })
        }
        binding.banner.text = message
        binding.banner.setBackgroundColor(color)
        binding.banner.startAnimation(animate)
    }

    override fun onDestroy() {
        viewModel.rtcModule.disposePeer()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (!isFinishing && binding.viewPager2.adapter != null) {
            fragmentList[binding.viewPager2.currentItem].onBackPressedComplete {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

}
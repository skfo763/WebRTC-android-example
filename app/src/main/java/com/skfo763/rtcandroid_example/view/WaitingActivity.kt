package com.skfo763.rtcandroid_example.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.CompoundButton
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.skfo763.rtcandroid_example.R
import com.skfo763.rtcandroid_example.base.WaitingActivityUseCase
import com.skfo763.rtcandroid_example.databinding.ActivityWaitingBinding
import com.skfo763.rtcandroid_example.utils.TokenManager
import com.skfo763.rtcandroid_example.utils.isPermissionGranted
import com.skfo763.rtcandroid_example.view.MainActivity.Companion.REQUEST_CODE_PERMISSION
import com.skfo763.rtcandroid_example.viewmodel.WaitingActivityViewModel
import kotlinx.android.synthetic.main.activity_waiting.*

class WaitingActivity : AppCompatActivity(), WaitingActivityUseCase {

    lateinit var binding: ActivityWaitingBinding

    private fun requestPermission(vararg permissions: String) {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION)
    }

    private val viewModel: WaitingActivityViewModel by lazy {
        ViewModelProvider(this, WaitingActivityViewModel.Factory(this)).get(WaitingActivityViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_waiting)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
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
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("token", viewModel.getToken(!binding.switchBtn.isChecked))
            })
        }
    }

    override fun startCall() {
        requestPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    }

    override fun getResString(genderMale: Int): String {
        return this.resources.getString(genderMale)
    }

}
package com.skfo763.rtcandroid_example.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.skfo763.rtcandroid_example.R
import com.skfo763.rtcandroid_example.utils.TokenManager
import com.skfo763.rtcandroid_example.view.MainActivity.Companion.REQUEST_CODE_PERMISSION
import kotlinx.android.synthetic.main.activity_waiting.*

class WaitingActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        setClickListeners()
        switch_btn.setOnCheckedChangeListener(this)
    }

    private fun setClickListeners() {
        appCompatButton.setOnClickListener {
            requestPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
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
                .setPositiveButton("Back") { dialog, _ -> dialog.dismiss(); finish() }
                .create()
                .show()
        } else {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("token", getToken())
            })
        }
    }

    private fun getToken(): String {
        return TokenManager.getToken(!switch_btn.isChecked)
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if(isChecked) {
            switch_btn.text = "여자"
        } else {
            switch_btn.text = "남자"
        }
    }
}
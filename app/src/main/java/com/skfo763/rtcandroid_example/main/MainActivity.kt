package com.skfo763.rtcandroid_example.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.skfo763.rtcandroid_example.R

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSON_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    private lateinit var roomName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun getExtras() {
        roomName = intent.getStringExtra("roomName") ?: ""
    }

    private fun checkCameraPermission() {
        if(ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {

            requestCameraPermission()
        } else {
            onCameraPermissionGranted()
        }
    }

    private fun requestCameraPermission(dialogShown: Boolean = false) {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)
                && !dialogShown) {
            showPermissionRationalDialog()
        } else {
            ActivityCompat.requestPermissions(
                    this, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSON_REQUEST_CODE)
        }
    }

    private fun showPermissionRationalDialog() {
        AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("This app need the camera to function")
                .setPositiveButton("Grant") { dialog, _ ->
                    dialog.dismiss()
                    requestCameraPermission(true)
                }
                .setNegativeButton("Deny") { dialog, _ ->
                    dialog.dismiss()
                    onCameraPermissionDenied()
                }
                .show()
    }

    private fun onCameraPermissionDenied() {
        Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show()
    }

    private fun onCameraPermissionGranted() {
        //...
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) { super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            CAMERA_PERMISSON_REQUEST_CODE -> {
                if(grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onCameraPermissionGranted()
                } else {
                    onCameraPermissionDenied()
                }
            }
        }
    }
}

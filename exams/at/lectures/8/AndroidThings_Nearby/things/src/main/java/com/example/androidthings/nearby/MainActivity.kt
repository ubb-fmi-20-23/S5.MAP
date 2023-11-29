package com.example.androidthings.nearby

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
  companion object {
    private const val REQUEST_CODE = 1410;
    private const val FINE_REQUEST_CODE = 1413;
  }

  private lateinit var advertisingManager: NearbyAdvertisingManager
  private lateinit var lcdManager: LCDManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    logi("Starting Android Things app...")
  }

  private fun buildManager() {
    advertisingManager = NearbyAdvertisingManager(this, object : NearbyAdvertisingManager.EventListener {
      override fun onMessage(message: String) {
        logi("received message: $message")
        if (message.contains("clear")) {
          lcdManager.displayString("")
        } else if (message.contains("game")) {
          lcdManager.displayString("Lecture #7:\n bit.ly/notHidden")
        } else if (message.contains("game8")) {
          lcdManager.displayString("Lecture #8:\n bit.ly/2yjeaaa")
        } else {
          lcdManager.displayString(message)
        }
      }
    })
  }

  override fun onStart() {
    super.onStart()
    lcdManager = LCDManager()
    lcdManager.displayString("Hello world!")

    checkForPermission(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_CODE) {
      checkForPermission(Manifest.permission.ACCESS_FINE_LOCATION, FINE_REQUEST_CODE) {
        buildManager()
      }
    }
  }

  private fun checkForPermission(permissionName: String, reqestCode: Int, callback: () -> Unit) {
    if (ContextCompat.checkSelfPermission(this, permissionName) != PackageManager.PERMISSION_GRANTED) {

      logd("Ask for permission")
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionName)) {
        logd("Permission granted")
        callback()

      } else {
        logd("Request permission ")
        ActivityCompat.requestPermissions(this, arrayOf(permissionName), reqestCode)
      }
    } else {
      logd("Permission already granted")
      callback()
    }
  }

  override fun onPause() {
    super.onPause()
    lcdManager.close()
  }

  override fun onStop() {
    super.onStop()
    advertisingManager.close()
  }
}

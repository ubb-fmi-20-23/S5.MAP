package com.example.androidthings.nearby

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
  private lateinit var discoveryManager: NearbyDiscoveryManager

  companion object {
    private const val REQUEST_CODE = 1410;
    private const val FINE_REQUEST_CODE = 1413;
  }

  private val listener = object : NearbyDiscoveryManager.EventListener {
    override fun onDiscovered() {
      logi("Endpoint discovered")
      Toast.makeText(this@MainActivity, "Endpoint discovered", Toast.LENGTH_LONG).show()
    }

    override fun startDiscovering() {
      logi("Start discovering...")
      Toast.makeText(this@MainActivity, "Start discovering...", Toast.LENGTH_LONG).show()
    }

    override fun onConnected() {
      logi("Connected")
      Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_LONG).show()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    btn.setOnClickListener {
      val txt = ed.text.toString()
      logd("Txt [$txt]")
      discoveryManager.sendData(txt)
    }
  }

  override fun onStart() {
    super.onStart()
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

  private fun buildManager() {
    discoveryManager = NearbyDiscoveryManager(this, listener)
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    logd("On Request Permission")
    when (requestCode) {
      REQUEST_CODE -> checkForPermission(Manifest.permission.ACCESS_FINE_LOCATION, FINE_REQUEST_CODE) {
        buildManager()
      }
      FINE_REQUEST_CODE -> buildManager()
    }
  }
}

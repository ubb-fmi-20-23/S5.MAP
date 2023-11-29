/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.bluetooth.audio

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.ServiceListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.view.KeyEvent
import com.google.android.things.bluetooth.BluetoothProfileManager
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import java.io.IOException
import java.util.*

/**
 * Sample usage of the A2DP sink bluetooth profile. At startup, this activity sets the Bluetooth
 * adapter in pairing mode for [.DISCOVERABLE_TIMEOUT_MS] ms.
 *
 *
 * To re-enable pairing mode, press "p" on an attached keyboard, use "adb shell input keyevent 44"
 * or press a button attached to the GPIO pin returned by [BoardDefaults.gPIOForPairing]
 *
 *
 * To forcefully disconnect any connected A2DP device, press "d" on an attached keyboard, use
 * "adb shell input keyevent 32" or press a button attached to the GPIO pin
 * returned by [BoardDefaults.gPIOForDisconnectAllBTDevices]
 *
 *
 *
 *
 * NOTE: While in pairing mode, pairing requests are auto-accepted - at this moment there's no
 * way to block specific pairing attempts while in pairing mode. This is known limitation that is
 * being worked on.
 */
class A2dpSinkActivity : Activity() {
  private var mBluetoothAdapter: BluetoothAdapter? = null
  private var mA2DPSinkProxy: BluetoothProfile? = null
  private var mPairingButtonDriver: ButtonInputDriver? = null
  private var mDisconnectAllButtonDriver: ButtonInputDriver? = null
  private var mTtsEngine: TextToSpeech? = null

  /**
   * Handle an intent that is broadcast by the Bluetooth adapter whenever it changes its
   * state (after calling enable(), for example).
   * Action is [BluetoothAdapter.ACTION_STATE_CHANGED] and extras describe the old
   * and the new states. You can use this intent to indicate that the device is ready to go.
   */
  private val mAdapterStateChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      val oldState = A2dpSinkHelper.getPreviousAdapterState(intent!!)
      val newState = A2dpSinkHelper.getCurrentAdapterState(intent)
      logi("Bluetooth Adapter changing state from $oldState to $newState")
      if (newState == BluetoothAdapter.STATE_ON) {
        logi("Bluetooth Adapter is ready")
        initA2DPSink()
      }
    }
  }

  /**
   * Handle an intent that is broadcast by the Bluetooth A2DP sink profile whenever a device
   * connects or disconnects to it.
   * Action is [A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED] and
   * extras describe the old and the new connection states. You can use it to indicate that
   * there's a device connected.
   */
  private val mSinkProfileStateChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED) {
        val oldState = A2dpSinkHelper.getPreviousProfileState(intent)
        val newState = A2dpSinkHelper.getCurrentProfileState(intent)
        val device = A2dpSinkHelper.getDevice(intent)
        logd("Bluetooth A2DP sink changing connection state from " + oldState +
            " to " + newState + " device " + device)
        val deviceName = Objects.toString(device.name, "a device")
        if (newState == BluetoothProfile.STATE_CONNECTED) {
          speak("Connected to $deviceName")
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
          speak("Disconnected from $deviceName")
        }
      }
    }
  }

  /**
   * Handle an intent that is broadcast by the Bluetooth A2DP sink profile whenever a device
   * starts or stops playing through the A2DP sink.
   * Action is [A2dpSinkHelper.ACTION_PLAYING_STATE_CHANGED] and
   * extras describe the old and the new playback states. You can use it to indicate that
   * there's something playing. You don't need to handle the stream playback by yourself.
   */
  private val mSinkProfilePlaybackChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == A2dpSinkHelper.ACTION_PLAYING_STATE_CHANGED) {
        val oldState = A2dpSinkHelper.getPreviousProfileState(intent)
        val newState = A2dpSinkHelper.getCurrentProfileState(intent)
        val device = A2dpSinkHelper.getDevice(intent)
        logd("Bluetooth A2DP sink changing playback state from " + oldState +
            " to " + newState + " device " + device)
        if (newState == A2dpSinkHelper.STATE_PLAYING) {
          logi("Playing audio from device " + device.address)
        } else if (newState == A2dpSinkHelper.STATE_NOT_PLAYING) {
          logi("Stopped playing audio from " + device.address)
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    if (mBluetoothAdapter == null) {
      logw("No default Bluetooth adapter. Device likely does not support bluetooth.")
      return
    }

    // We use Text-to-Speech to indicate status change to the user
    initTts()
    registerReceiver(mAdapterStateChangeReceiver, IntentFilter(
        BluetoothAdapter.ACTION_STATE_CHANGED))
    registerReceiver(mSinkProfileStateChangeReceiver, IntentFilter(
        A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED))
    registerReceiver(mSinkProfilePlaybackChangeReceiver, IntentFilter(
        A2dpSinkHelper.ACTION_PLAYING_STATE_CHANGED))
    if (mBluetoothAdapter!!.isEnabled) {
      logd("Bluetooth Adapter is already enabled.")
      initA2DPSink()
    } else {
      logd("Bluetooth adapter not enabled. Enabling.")
      mBluetoothAdapter!!.enable()
    }
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_P -> {
        // Enable Pairing mode (discoverable)
        enableDiscoverable()
        return true
      }
      KeyEvent.KEYCODE_D -> {
        // Disconnect any currently connected devices
        disconnectConnectedDevices()
        return true
      }
    }
    return super.onKeyUp(keyCode, event)
  }

  override fun onDestroy() {
    super.onDestroy()
    logd("onDestroy")
    try {
      if (mPairingButtonDriver != null) mPairingButtonDriver!!.close()
    } catch (e: IOException) { /* close quietly */
    }
    try {
      if (mDisconnectAllButtonDriver != null) mDisconnectAllButtonDriver!!.close()
    } catch (e: IOException) { /* close quietly */
    }
    unregisterReceiver(mAdapterStateChangeReceiver)
    unregisterReceiver(mSinkProfileStateChangeReceiver)
    unregisterReceiver(mSinkProfilePlaybackChangeReceiver)
    if (mA2DPSinkProxy != null) {
      mBluetoothAdapter!!.closeProfileProxy(A2dpSinkHelper.A2DP_SINK_PROFILE,
          mA2DPSinkProxy)
    }
    if (mTtsEngine != null) {
      mTtsEngine!!.stop()
      mTtsEngine!!.shutdown()
    }

    // we intentionally leave the Bluetooth adapter enabled, so that other samples can use it
    // without having to initialize it.
  }

  private fun setupBTProfiles() {
    val bluetoothProfileManager = BluetoothProfileManager.getInstance()
    val enabledProfiles = bluetoothProfileManager.enabledProfiles
    if (!enabledProfiles.contains(A2dpSinkHelper.A2DP_SINK_PROFILE)) {
      logd("Enabling A2dp sink mode.")
      val toDisable = listOf(BluetoothProfile.A2DP)
      val toEnable = listOf(
          A2dpSinkHelper.A2DP_SINK_PROFILE,
          A2dpSinkHelper.AVRCP_CONTROLLER_PROFILE)
      bluetoothProfileManager.enableAndDisableProfiles(toEnable, toDisable)
    } else {
      logd("A2dp sink profile is enabled.")
    }
  }

  /**
   * Initiate the A2DP sink.
   */
  private fun initA2DPSink() {
    if (mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) {
      loge("Bluetooth adapter not available or not enabled.")
      return
    }
    setupBTProfiles()
    logd("Set up Bluetooth Adapter name and profile")
    mBluetoothAdapter!!.name = ADAPTER_FRIENDLY_NAME
    mBluetoothAdapter!!.getProfileProxy(this, object : ServiceListener {
      override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
        mA2DPSinkProxy = proxy
        enableDiscoverable()
      }

      override fun onServiceDisconnected(profile: Int) {}
    }, A2dpSinkHelper.A2DP_SINK_PROFILE)
    configureButton()
  }

  /**
   * Enable the current [BluetoothAdapter] to be discovered (available for pairing) for
   * the next [.DISCOVERABLE_TIMEOUT_MS] ms.
   */
  private fun enableDiscoverable() {
    logd("Registering for discovery.")
    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
        DISCOVERABLE_TIMEOUT_MS)
    startActivityForResult(discoverableIntent, REQUEST_CODE_ENABLE_DISCOVERABLE)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE_ENABLE_DISCOVERABLE) {
      logd("Enable discoverable returned with result $resultCode")

      // ResultCode, as described in BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE, is either
      // RESULT_CANCELED or the number of milliseconds that the device will stay in
      // discoverable mode. In a regular Android device, the user will see a popup requesting
      // authorization, and if they cancel, RESULT_CANCELED is returned. In Android Things,
      // on the other hand, the authorization for pairing is always given without user
      // interference, so RESULT_CANCELED should never be returned.
      if (resultCode == RESULT_CANCELED) {
        loge("Enable discoverable has been cancelled by the user. " +
            "This should never happen in an Android Things device.")
        return
      }
      logi("Bluetooth adapter successfully set to discoverable mode. " +
          "Any A2DP source can find it with the name " + ADAPTER_FRIENDLY_NAME +
          " and pair for the next " + DISCOVERABLE_TIMEOUT_MS + " ms. " +
          "Try looking for it on your phone, for example.")

      // There is nothing else required here, since Android framework automatically handles
      // A2DP Sink. Most relevant Bluetooth events, like connection/disconnection, will
      // generate corresponding broadcast intents or profile proxy events that you can
      // listen to and react appropriately.
      speak("Bluetooth audio sink is discoverable for " + DISCOVERABLE_TIMEOUT_MS +
          " milliseconds. Look for a device named " + ADAPTER_FRIENDLY_NAME)
    }
  }

  private fun disconnectConnectedDevices() {
    if (mA2DPSinkProxy == null || mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) {
      return
    }
    speak("Disconnecting devices")
    for (device in mA2DPSinkProxy!!.connectedDevices) {
      logi("Disconnecting device $device")
      A2dpSinkHelper.disconnect(mA2DPSinkProxy!!, device)
    }
  }

  private fun configureButton() {
    try {
      mPairingButtonDriver = ButtonInputDriver(BoardDefaults.gPIOForPairing,
          Button.LogicState.PRESSED_WHEN_LOW  , KeyEvent.KEYCODE_P)
      mPairingButtonDriver!!.register()
      mDisconnectAllButtonDriver = ButtonInputDriver(
          BoardDefaults.gPIOForDisconnectAllBTDevices,
          Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_D)
      mDisconnectAllButtonDriver!!.register()
    } catch (e: IOException) {
      logw("Could not register GPIO button drivers. Use keyboard events to trigger " +
          "the functions instead", e)
    }
  }

  private fun initTts() {
    mTtsEngine = TextToSpeech(this@A2dpSinkActivity,
        OnInitListener { status ->
          if (status == TextToSpeech.SUCCESS) {
            mTtsEngine!!.language = Locale.US
          } else {
            logw("Could not open TTS Engine (onInit status=" + status
                + "). Ignoring text to speech")
            mTtsEngine = null
          }
        })
  }

  private fun speak(utterance: String) {
    logi(utterance)
    if (mTtsEngine != null) {
      mTtsEngine!!.speak(utterance, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
    }
  }

  companion object {
    private const val ADAPTER_FRIENDLY_NAME = "My Android Things device"
    private const val DISCOVERABLE_TIMEOUT_MS = 300
    private const val REQUEST_CODE_ENABLE_DISCOVERABLE = 100
    private const val UTTERANCE_ID = "com.example.androidthings.bluetooth.audio.UTTERANCE_ID"
  }
}
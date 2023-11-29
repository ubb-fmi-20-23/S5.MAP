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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Intent
import java.lang.reflect.InvocationTargetException

/**
 * Helper methods and constants related to the A2DP_SINK profile.
 *
 *
 * Most constants defined here are just copied from classes in the
 * [android.bluetooth] package, since they are hidden from the public Android API and cannot
 * be directly used.
 */
object A2dpSinkHelper {
  /**
   * Profile number for A2DP_SINK profile.
   */
  const val A2DP_SINK_PROFILE = 11

  /**
   * Profile number for AVRCP_CONTROLLER profile.
   */
  const val AVRCP_CONTROLLER_PROFILE = 12

  /**
   * Intent used to broadcast the change in connection state of the A2DP Sink
   * profile.
   *
   *
   * This intent will have 3 extras:
   *
   *  *  [BluetoothProfile.EXTRA_STATE] - The current state of the profile.
   *  *  [BluetoothProfile.EXTRA_PREVIOUS_STATE]- The previous state of the
   * profile.
   *  *  [BluetoothDevice.EXTRA_DEVICE] - The remote device.
   *
   *
   *
   * [BluetoothProfile.EXTRA_STATE] or [BluetoothProfile.EXTRA_PREVIOUS_STATE]
   * can be any of [BluetoothProfile.STATE_DISCONNECTED],
   * [BluetoothProfile.STATE_CONNECTING], [BluetoothProfile.STATE_CONNECTED],
   * [BluetoothProfile.STATE_DISCONNECTING].
   *
   *
   * Requires [android.Manifest.permission.BLUETOOTH] permission to
   * receive.
   */
  const val ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED"

  /**
   * Intent used to broadcast the change in the Playing state of the A2DP Sink
   * profile.
   *
   *
   * This intent will have 3 extras:
   *
   *  *  [BluetoothProfile.EXTRA_STATE] - The current state of the profile.
   *  *  [BluetoothProfile.EXTRA_PREVIOUS_STATE]- The previous state of the
   * profile.
   *  *  [BluetoothDevice.EXTRA_DEVICE] - The remote device.
   *
   *
   *
   * [BluetoothProfile.EXTRA_STATE] or [BluetoothProfile.EXTRA_PREVIOUS_STATE]
   * can be any of [.STATE_PLAYING], [.STATE_NOT_PLAYING],
   *
   *
   * Requires [android.Manifest.permission.BLUETOOTH] permission to
   * receive.
   */
  const val ACTION_PLAYING_STATE_CHANGED = "android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED"

  /**
   * A2DP sink device is streaming music. This state can be one of
   * [BluetoothProfile.EXTRA_STATE] or [BluetoothProfile.EXTRA_PREVIOUS_STATE] of
   * [.ACTION_PLAYING_STATE_CHANGED] intent.
   */
  const val STATE_PLAYING = 10

  /**
   * A2DP sink device is NOT streaming music. This state can be one of
   * [BluetoothProfile.EXTRA_STATE] or [BluetoothProfile.EXTRA_PREVIOUS_STATE] of
   * [.ACTION_PLAYING_STATE_CHANGED] intent.
   */
  const val STATE_NOT_PLAYING = 11
  fun getPreviousAdapterState(intent: Intent): Int {
    return intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1)
  }

  fun getCurrentAdapterState(intent: Intent): Int {
    return intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
  }

  fun getPreviousProfileState(intent: Intent): Int {
    return intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1)
  }

  fun getCurrentProfileState(intent: Intent): Int {
    return intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
  }

  fun getDevice(intent: Intent): BluetoothDevice {
    return intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
  }

  /**
   * Provides a way to call the disconnect method in the BluetoothA2dpSink class that is
   * currently hidden from the public API. Avoid relying on this for production level code, since
   * hidden code in the API is subject to change.
   *
   * @param profile
   * @param device
   * @return
   */
  fun disconnect(profile: BluetoothProfile, device: BluetoothDevice?): Boolean {
    return try {
      val m = profile.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
      m.invoke(profile, device)
      true
    } catch (e: NoSuchMethodException) {
      logw("No disconnect method in the " + profile.javaClass.name +
          " class, ignoring request.")
      false
    } catch (e: InvocationTargetException) {
      logw("Could not execute method 'disconnect' in profile " +
          profile.javaClass.name + ", ignoring request.", e)
      false
    } catch (e: IllegalAccessException) {
      logw("Could not execute method 'disconnect' in profile " +
          profile.javaClass.name + ", ignoring request.", e)
      false
    }
  }
}
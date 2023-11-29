package com.example.at.twillio

import android.app.Activity
import android.os.Bundle
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.PeripheralManager

import java.io.IOException


class MainActivity : Activity() {
  private companion object {
    const val BUTTON_PIN_NAME = "BCM14" // GPIO port wired to the button
    const val LONG_PRESS_DELAY_MS: Long = 600
  }

  private lateinit var buttonGpio: Gpio

  private var pressStartedAt: Long = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val manager = PeripheralManager.getInstance()
    try {
      buttonGpio = manager.openGpio(BUTTON_PIN_NAME)
      buttonGpio.setDirection(Gpio.DIRECTION_IN)
      buttonGpio.setEdgeTriggerType(Gpio.EDGE_BOTH)
      buttonGpio.registerGpioCallback(mCallback)
      logd("The button was registered sucessfully!")
    } catch (e: IOException) {
      loge("Error on PeripheralIO API", e)
    }
  }

  private val mCallback = GpioCallback {
    val gpioValue = it.value
    logd("Value: $gpioValue currentTime: ${System.nanoTime()}!")
    if (!gpioValue) {
      pressStartedAt = System.currentTimeMillis()
    } else {
      val duration = System.currentTimeMillis() - pressStartedAt
      logd("Duration: $duration!")
      if (duration > LONG_PRESS_DELAY_MS) {
        logd("Long press")
//        TwilioClient.sms("Alert! the button was pressed!");
        TwilioClient.call()
      } else {
        logd("False alarm!")
      }
    }
    true
  }

  override fun onDestroy() {
    super.onDestroy()

    // Step 6. Close the resource
    buttonGpio.unregisterGpioCallback(mCallback)
    logd("Unregistered the callback")
    try {
      buttonGpio.close()
    } catch (e: IOException) {
      loge("Error on PeripheralIO API", e)
    }
  }
}

package com.example.inputdemo

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import android.view.KeyEvent.KEYCODE_A
import android.view.View
import com.example.buttondemo.logd
import com.example.buttondemo.loge
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.Button.LogicState
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*


/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity() {

  private lateinit var mInputDriver: ButtonInputDriver
  private val gpioPinName = "GPIO6_IO15"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    try {
      mInputDriver = ButtonInputDriver(
          gpioPinName,
          Button.LogicState.PRESSED_WHEN_LOW,
          KeyEvent.KEYCODE_A // the keycode to send
      )
      mInputDriver.register()
    } catch (e: IOException) {
      loge("Unable to register the button :(", e)
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    return if (keyCode == KeyEvent.KEYCODE_A) {
      // do something awesome
      logd("Something super awesome, again!")
      val rnd = Random()
      layoutId.setBackgroundColor(Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)))
      true // indicate we handled the event
    } else super.onKeyDown(keyCode, event)
  }

  override fun onDestroy() {
    super.onDestroy()
    mInputDriver.unregister()
    try {
      mInputDriver.close()
    } catch (e: IOException) {
      // error closing input driver
      loge("Unable to close the button!")
    }
  }
}

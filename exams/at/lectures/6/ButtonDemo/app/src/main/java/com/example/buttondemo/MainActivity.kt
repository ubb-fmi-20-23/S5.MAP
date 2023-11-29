package com.example.buttondemo

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.pio.PeripheralManager
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

  private lateinit var mButton: Button
  private val gpioPinName = "BCM2"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    logd("GPIO ports: " + PeripheralManager.getInstance().gpioList)
    logd("Just press the button!")
    try {
      mButton = Button(
        gpioPinName,
        Button.LogicState.PRESSED_WHEN_LOW
      )
      mButton.setOnButtonEventListener { _, _ ->
        // do something awesome
        logd("Something awesome!")
        val rnd = Random()
        val color =
          Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
        panel.setBackgroundColor(color)
      }
    } catch (e: IOException) {
      loge("Unable to register the button :(")
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      mButton.close()
    } catch (e: IOException) {
      loge("Unable to close the button!")
    }
  }
}

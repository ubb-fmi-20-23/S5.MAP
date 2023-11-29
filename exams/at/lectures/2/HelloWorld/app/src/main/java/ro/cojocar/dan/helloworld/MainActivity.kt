package ro.cojocar.dan.helloworld

import android.app.Activity
import android.os.Build
import android.os.Bundle
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import java.io.IOException


class MainActivity : Activity() {
  companion object {
    private const val DEVICE_RPI3 = "rpi3"
    private const val DEVICE_IMX7D_PICO = "imx7d_pico"
  }

  private lateinit var mLedGpio: Gpio

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val manager = PeripheralManager.getInstance()
    logd("Available GPIO: " + manager.gpioList)
    logd("Available PWM: " + manager.pwmList)

    mLedGpio = manager.openGpio(getGPIOForLED())
    mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
  }


  override fun onDestroy() {
    super.onDestroy()
    try {
      mLedGpio.close()
    } catch (e: IOException) {
      loge("Error closing LED GPIO", e)
    }
  }

  override fun onStart() {
    super.onStart()
    var count = 10
    val ledBlinker = Runnable {
      while (--count > 0) {
        // Turn on the LED
        setLedValue(true)
        logd("on")
        sleep(3000)
        // Turn off the LED
        setLedValue(false)
        logd("off")
        sleep(2000)
      }
      setLedValue(false)
      logd("done")
    }
    Thread(ledBlinker).start()
  }

  private fun sleep(milliseconds: Int) {
    try {
      Thread.sleep(milliseconds.toLong())
    } catch (e: InterruptedException) {
      loge("Got interrupted", e)
    }
  }

  /**
   * Update the value of the LED output.
   */
  private fun setLedValue(value: Boolean) {
    try {
      mLedGpio.value = value
    } catch (e: IOException) {
      loge("Error updating GPIO value", e)
    }
  }

  private fun getGPIOForLED(): String? {
    return when (getBoardVariant()) {
      DEVICE_RPI3 -> "BCM26"
      DEVICE_IMX7D_PICO -> "GPIO_34"
      else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
    }
  }

  private fun getBoardVariant(): String {
    val sBoardVariant = Build.DEVICE
    if (sBoardVariant.isNotEmpty()) {
      return sBoardVariant
    }
    return "Unknown"
  }
}

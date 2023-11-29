package ro.cojocar.dan.homeactivity

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import kotlinx.android.synthetic.main.blink.*
import java.io.IOException


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
class BlinkActivity : Activity() {
  private companion object {
    const val LED_PIN_NAME = "BCM6" // GPIO port wired to the LED
    const val INTERVAL_BETWEEN_BLINKS_MS = 1000L
  }

  private val mHandler = Handler()

  private lateinit var ledGpio: Gpio

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.blink)

    // Step 1. Create GPIO connection.
    val manager = PeripheralManager.getInstance()
    try {
      ledGpio = manager.openGpio(LED_PIN_NAME)
      // Step 2. Configure as an output.
      ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

      // Step 4. Repeat using a handler.
      mHandler.post(blinkRunnable)
      toggle.text = getString(R.string.stop)
    } catch (e: IOException) {
      loge("Error on PeripheralIO API", e)
    }
    toggle.setOnClickListener {
      val button = it as Button
      if (button.text.contains("Stop")) {
        mHandler.removeCallbacks(blinkRunnable)
        ledGpio.value = false
        button.text = getString(R.string.start)
        toggle.setBackgroundColor(Color.GRAY)
      } else {
        mHandler.post(blinkRunnable)
        button.text = getString(R.string.stop)
      }
      logd("Toggle the status!")
    }
  }

  private val blinkRunnable = object : Runnable {
    override fun run() {
      try {
        // Step 3. Toggle the LED state
        ledGpio.value = !ledGpio.value
        if (ledGpio.value) {
          toggle.setBackgroundColor(Color.GREEN)
        } else {
          toggle.setBackgroundColor(Color.GRAY)
        }

        // Step 4. Schedule another event after delay.
        mHandler.postDelayed(this, INTERVAL_BETWEEN_BLINKS_MS)
      } catch (e: IOException) {
        loge("Error on PeripheralIO API", e)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    // Step 4. Remove handler events on close.
    mHandler.removeCallbacks(blinkRunnable)
    // Step 5. Close the resource.
    try {
      ledGpio.close()
    } catch (e: IOException) {
      loge("Error on PeripheralIO API", e)
    }
  }
}

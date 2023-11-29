package ro.cojocar.dan.homeactivity

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.PeripheralManager
import kotlinx.android.synthetic.main.button.*
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
class ButtonActivity : Activity() {
  private companion object {
    const val BUTTON_PIN_NAME = "BCM21" // GPIO port wired to the button
  }

  private lateinit var buttonGpio: Gpio

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.button)

    val manager = PeripheralManager.getInstance()
    try {
      // Step 1. Create GPIO connection.
      buttonGpio = manager.openGpio(BUTTON_PIN_NAME)
      logd("Configured the button")
      // Step 2. Configure as an input.
      buttonGpio.setDirection(Gpio.DIRECTION_IN)
      logd("Establish the direction")
      // Step 3. Enable edge trigger events.
      buttonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING)
      logd("Establish the trigger type")
      // Step 4. Register an event callback.
      buttonGpio.registerGpioCallback(mCallback)
      logd("Registered the callback")
      textView.setBackgroundColor(Color.WHITE)
    } catch (e: IOException) {
      loge("Error on PeripheralIO API", e)
    }
  }

  // Step 4. Register an event callback.
  private val mCallback = GpioCallback {
    logi("GPIO changed, button pressed")
    val rnd = Random()
    val argb = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    buttonLayout.setBackgroundColor(argb)
    textView.setBackgroundColor(argb)
    // Step 5. Return true to keep callback active.
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

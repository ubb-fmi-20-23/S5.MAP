package ro.cojocar.dan.myapplication

import android.app.Activity
import android.os.Bundle
import com.google.android.things.pio.PeripheralManager
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

  private lateinit var arduino: Arduino
  private val readSensorsTimer = Timer("ReadSensorsTimer", true)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val manager = PeripheralManager.getInstance()
    val deviceList: List<String> = manager.uartDeviceList
    if (deviceList.isEmpty()) {
      logi("No UART port available on this device.")
    } else {
      logi("List of available devices: $deviceList")
    }

    arduino = Arduino()
    val time = 5L * 1000L
    readSensorsTimer.scheduleAtFixedRate(ReadSensorsTask(arduino), 0L, time)

  }

  override fun onDestroy() {
    super.onDestroy()
    readSensorsTimer.cancel()
    arduino.close()
  }
}

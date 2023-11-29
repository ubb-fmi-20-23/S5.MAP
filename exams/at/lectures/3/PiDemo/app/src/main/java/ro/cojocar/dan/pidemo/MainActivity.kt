package ro.cojocar.dan.pidemo

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.pio.PeripheralManager
import java.io.Closeable


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

    private lateinit var buttons: Buttons
    private lateinit var display: Display

    companion object {
        val MESSAGES = mapOf(
            KeyEvent.KEYCODE_A to "AHOY",
            KeyEvent.KEYCODE_B to "TEST",
            KeyEvent.KEYCODE_C to "3.14"
        )

        const val DEFAULT_MESSAGE = "HI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pioManager = PeripheralManager.getInstance()
        logd("Available GPIO: " + pioManager.gpioList)
        buttons = Buttons()
        display = Display()

        display.displayMessage(DEFAULT_MESSAGE)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?) = when (keyCode) {
        in MESSAGES.keys -> {
            display.displayMessage(MESSAGES.getValue(keyCode))
            true
        }
        else -> super.onKeyUp(keyCode, event)
    }


    override fun onDestroy() {
        arrayOf(buttons, display).forEach(Closeable::close)
        super.onDestroy()
    }
}


class Display(private val display: AlphanumericDisplay = AlphanumericDisplay(Display.DISPLAY_I2C_BUS)) : Closeable {

    companion object {
        const val DISPLAY_I2C_BUS = "I2C1"
    }

    init {
        display.setEnabled(true)
        display.clear()
    }

    fun displayMessage(message: String) {
        display.display(message)
    }

    override fun close() {
        display.clear()
        display.setEnabled(false)
        display.close()
    }
}


class Buttons(
    private val buttonDrivers: List<ButtonInputDriver> = listOf(
        registerButtonDriver(Buttons.BUTTON_A_GPIO_PIN, KeyEvent.KEYCODE_A),
        registerButtonDriver(Buttons.BUTTON_B_GPIO_PIN, KeyEvent.KEYCODE_B),
        registerButtonDriver(Buttons.BUTTON_C_GPIO_PIN, KeyEvent.KEYCODE_C)
    )
) : Closeable {

    companion object {

        const val BUTTON_A_GPIO_PIN = "GPIO6_IO14"
        const val BUTTON_B_GPIO_PIN = "GPIO6_IO15"
        const val BUTTON_C_GPIO_PIN = "GPIO2_IO07"

        private fun registerButtonDriver(pin: String, keycode: Int): ButtonInputDriver {
            val driver = ButtonInputDriver(pin, Button.LogicState.PRESSED_WHEN_LOW, keycode)
            driver.register()
            return driver
        }
    }

    override fun close() {
        buttonDrivers.forEach(ButtonInputDriver::close)
    }
}
package com.example.mydriver

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager

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
class LEDSensor : AutoCloseable {
    private lateinit var redPin: Gpio
    private lateinit var greenPin: Gpio
    private lateinit var bluePin: Gpio

    fun init(redGPIOName: String = "BCM17", greenGPIOName: String = "BCM27", blueGPIOName: String = "BCM22") {
        val pioManager = PeripheralManager.getInstance()

        redPin = pioManager.openGpio(redGPIOName)
        greenPin = pioManager.openGpio(greenGPIOName)
        bluePin = pioManager.openGpio(blueGPIOName)
        initGPIO(redPin)
        initGPIO(greenPin)
        initGPIO(bluePin)
    }

    fun write(red: Boolean = false, green: Boolean = false, blue: Boolean = false) {
        redPin.value = red
        greenPin.value = green
        bluePin.value = blue
    }

    private fun initGPIO(pin: Gpio) {
        pin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        pin.setActiveType(Gpio.ACTIVE_HIGH)
    }

    override fun close() {
        redPin.close()
        greenPin.close()
        bluePin.close()
    }
}

package com.example.mydriver

import android.app.Activity
import android.os.Bundle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private lateinit var sensor: LEDSensor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensor = LEDSensor()
        sensor.init()
        sensor.write(blue = true)
        GlobalScope.launch {
            var nextInt = 0;
            repeat(10) {
                delay(1000)
                val random = Random()
                val oldColor = nextInt;
                while (nextInt == oldColor) {
                    nextInt = random.nextInt(3)
                }
                logd("Changing color to: $nextInt")
                sensor.write(red = nextInt == 0, green = nextInt == 1, blue = nextInt == 2)
            }
            sensor.write(red = false, green = false, blue = false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensor.close()
    }
}

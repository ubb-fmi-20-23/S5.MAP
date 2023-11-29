package ro.cojocar.mqttrainbow

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import com.google.android.things.contrib.driver.apa102.Apa102
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.google.android.things.pio.PeripheralManager
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.IOException


/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the PeripheralManager
 * For example, the snippet below will open a GPIO pin and set it to HIGH:
 *
 * val manager = PeripheralManager.getInstance()
 * val gpio = manager.openGpio("BCM6").apply {
 *     setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * }
 * gpio.value = true
 *
 * You can find additional examples on GitHub: https://github.com/androidthings
 */
class MainActivity : Activity(), MqttCallback {

    private lateinit var segment: AlphanumericDisplay
    private lateinit var ledstrip: Apa102
    private lateinit var rainbow: IntArray
    private lateinit var rainbowOff: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = PeripheralManager.getInstance()

        logd("pins: ${service.gpioList}")

        try {
            val client = MqttClient(
                "tcp://192.168.86.249:1883",
                "AndroidThing ${Build.DEVICE}",
                MemoryPersistence()
            )
            client.setCallback(this)
            client.connect()
            client.subscribe("topic/pico")
            client.subscribe("topic/all")
        } catch (e: MqttException) {
            logd("Error while setting up the mqtt endpoint", e)
        }

        // Light up the rainbow
        ledstrip = RainbowHat.openLedStrip()
        ledstrip.brightness = 10
        rainbow = IntArray(RainbowHat.LEDSTRIP_LENGTH)
        rainbowOff = IntArray(RainbowHat.LEDSTRIP_LENGTH)
        for (i in rainbow.indices) {
            rainbow[i] = Color.HSVToColor(255, floatArrayOf(i * 360f / rainbow.size, 1.0f, 1.0f))
            rainbowOff[i] = Color.HSVToColor(255, floatArrayOf(0f, 0f, 0f))
        }

        // Display a string on the segment display.
        segment = RainbowHat.openDisplay()
        segment.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX)
        segment.display("")
        segment.setEnabled(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        logd("onDestroy")
        try {
            ledstrip.close()
            segment.close()
        } catch (e: IOException) {
            loge("Error on PeripheralIO API", e)
        }
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {

        val payload = message?.payload
        val command = String(payload!!)
        logd("Received: $command")
        when (command) {
            "turn on" -> {
                logd("Turn the LED ON")
                ledstrip.write(rainbow)
                segment.display("ON")
            }
            "turn off" -> {
                logd("Turn the LED OFF")
                ledstrip.write(rainbowOff)
                segment.display("OFF")
            }
            else -> logd("Message: $command not supported!")
        }
    }

    override fun connectionLost(cause: Throwable?) {
        logd("We lost the connection")
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        logd("Completed the delivery")
    }
}
package ro.cojocar.mqttatdemo

import android.app.Activity
import android.os.Build
import android.os.Bundle
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import ro.cojocar.mqttatdemo.BoardDefaults.gPIOLed
import ro.cojocar.mqttatdemo.BoardDefaults.topicName
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
  private val LED_PIN = gPIOLed
  private lateinit var ledPin: Gpio

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val service = PeripheralManager.getInstance()

    logd("pins: ${service.gpioList}")

    try {
      val client = MqttClient("tcp://192.168.86.249:1883", "AndroidThing ${Build.DEVICE}", MemoryPersistence())
      client.setCallback(this)
      client.connect()
      client.subscribe(topicName)
      client.subscribe("topic/all")
    } catch (e: MqttException) {
      logd("Error while setting up the mqtt endpoint", e)
    }

    try {
      // Create GPIO connection for LED.
      ledPin = service.openGpio(LED_PIN)
      // Configure as an output.
      ledPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
    } catch (e: IOException) {
      loge("Error on PeripheralIO API", e)
    }

  }

  override fun onDestroy() {
    super.onDestroy()
    logd("onDestroy")
    try {
      ledPin.close()
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
        ledPin.value = true
      }
      "turn off" -> {
        logd("Turn the LED OFF")
        ledPin.value = false
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
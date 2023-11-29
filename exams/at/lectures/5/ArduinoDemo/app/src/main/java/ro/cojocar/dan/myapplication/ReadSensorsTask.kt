package ro.cojocar.dan.myapplication

import java.util.*

class ReadSensorsTask(val arduino: Arduino) : TimerTask() {

  companion object {
    private const val OPERATION_DELAY = 2000L
    private const val READ_DELAY = 500L
  }

  override fun run() {
    readHumidity()
    Thread.sleep(OPERATION_DELAY)
    readTemp()
    Thread.sleep(OPERATION_DELAY)
  }

  private fun readHumidity() {
    arduino.write("H")
    Thread.sleep(READ_DELAY)
    val humidity = arduino.read()
    logi("Humidity ${humidity}%")
  }

  private fun readTemp() {
    arduino.write("T")
    Thread.sleep(READ_DELAY)
    val temp = arduino.read()
    logi("Temperature ${temp}C")
  }
}
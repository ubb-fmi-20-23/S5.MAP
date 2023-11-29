package ro.cojocar.dan.pwmdemo

import android.app.Activity
import android.os.Bundle
import android.widget.SeekBar
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.Pwm
import kotlinx.android.synthetic.main.activity_main.*
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
class MainActivity : Activity() {
  companion object {
    private const val PWM_NAME = "PWM1"
  }

  private var pwm: Pwm? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val manager = PeripheralManager.getInstance()
    val portList: List<String> = manager.pwmList
    if (portList.isEmpty()) {
      logi("No PWM port available on this device.")
    } else {
      logi("List of available ports: $portList")
    }

    // Attempt to access the PWM port
    pwm = try {
      PeripheralManager.getInstance().openPwm(PWM_NAME)
    } catch (e: IOException) {
      logw("Unable to access $PWM_NAME", e)
      null
    }


    initButton.setOnClickListener {
      logi("Initialize $PWM_NAME")
      initializePwm(pwm!!)
    }

    frequency.min = 1
    frequency.max = 50
    frequency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      var progress = 0;
      override fun onStartTrackingTouch(seekBar: SeekBar?) {
      }

      override fun onStopTrackingTouch(seekBar: SeekBar?) {
        pwm?.setPwmFrequencyHz(progress.toDouble())
        logi("frequency set to: $progress")
      }

      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        this.progress = progress
      }
    })

    duty.min = 0
    duty.max = 100
    duty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      var progress = 0;
      override fun onStartTrackingTouch(seekBar: SeekBar?) {
      }

      override fun onStopTrackingTouch(seekBar: SeekBar?) {
        pwm?.setPwmDutyCycle(progress.toDouble())
        logi("dutyCycle set to: $progress")
      }

      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        this.progress = progress
      }
    })
  }

  @Throws(IOException::class)
  fun initializePwm(pwm: Pwm) {
    frequency.progress = 50
    duty.progress = 50
    pwm.apply {
      setPwmFrequencyHz(frequency.progress.toDouble())
      setPwmDutyCycle(duty.progress.toDouble())
      // Enable the PWM signal
      setEnabled(true)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      pwm?.setEnabled(false)
      pwm?.close()
      pwm = null
    } catch (e: IOException) {
      logw("Unable to close $PWM_NAME", e)
    }
  }
}

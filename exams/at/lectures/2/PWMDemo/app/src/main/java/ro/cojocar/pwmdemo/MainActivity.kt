package ro.cojocar.pwmdemo

import android.app.Activity
import com.polidea.androidthings.driver.steppermotor.Direction
import com.polidea.androidthings.driver.steppermotor.listener.RotationListener
import com.polidea.androidthings.driver.uln2003.driver.ULN2003Resolution
import com.polidea.androidthings.driver.uln2003.motor.ULN2003StepperMotor

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
class MainActivity : Activity() {

  private val in1Pin = "BCM4"
  private val in2Pin = "BCM17"
  private val in3Pin = "BCM27"
  private val in4Pin = "BCM22"

  private lateinit var stepper: ULN2003StepperMotor

  override fun onResume() {
    super.onResume()
    stepper = ULN2003StepperMotor(in1Pin, in2Pin, in3Pin, in4Pin)
    stepper.rotate(degrees = 60.0,
        direction = Direction.CLOCKWISE,
        resolutionId = ULN2003Resolution.FULL.id,
        rpm = 1.0,
        rotationListener = object : RotationListener {
          override fun onStarted() {
            logi("first rotation started")
          }

          override fun onFinishedSuccessfully() {
            logi("first rotation finished")
          }

          override fun onFinishedWithError(degreesToRotate: Double, rotatedDegrees: Double, exception: Exception) {
            loge("error, degrees to rotate: {$degreesToRotate}  rotated degrees: {$rotatedDegrees}")
          }
        })

    stepper.rotate(degrees = 60.0,
        direction = Direction.COUNTERCLOCKWISE,
        resolutionId = ULN2003Resolution.HALF.id,
        rpm = 2.5)

    stepper.rotate(degrees = 180.0,
        direction = Direction.CLOCKWISE,
        resolutionId = ULN2003Resolution.FULL.id,
        rpm = 5.0)

    stepper.rotate(degrees = 180.0,
        direction = Direction.COUNTERCLOCKWISE,
        resolutionId = ULN2003Resolution.HALF.id,
        rpm = 8.0,
        rotationListener = object : RotationListener {
          override fun onFinishedSuccessfully() {
            logi("all rotations finished")
          }

          override fun onFinishedWithError(degreesToRotate: Double, rotatedDegrees: Double, exception: Exception) {
            loge("error, degrees to rotate: {$degreesToRotate}  rotated degrees: {$rotatedDegrees}")
          }
        })
  }

  override fun onPause() {
    stepper.close()
    super.onPause()
  }
}

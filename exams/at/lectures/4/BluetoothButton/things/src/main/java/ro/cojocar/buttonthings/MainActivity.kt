package ro.cojocar.buttonthings

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle

import java.io.IOException
import java.util.HashSet

import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent

import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager

class MainActivity : Activity() {

  private val mRegisteredDevices = HashSet<BluetoothDevice>()

  private var mLedGpio: Gpio? = null
  private var mButtonInputDriver: ButtonInputDriver? = null

  private var bleScanner: BluetoothLeScanner? = null
  private var mBluetoothAdapter: BluetoothAdapter? = null
  private var mScanning: Boolean = false
  private var mHandler: Handler? = null

  private var mBluetoothLeService: BluetoothLeService? = null
  private var mConnected = false
  private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null

  private var mDeviceAddress: String? = null

  /**
   * Listens for Bluetooth adapter events to enable/disable
   * advertising and server functionality.
   */
  private val mBluetoothReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

      when (state) {
        BluetoothAdapter.STATE_ON -> {
          BluetoothHelper.startAdvertising()
          BluetoothHelper.startServer(this@MainActivity, mGattServerCallback)
        }
        BluetoothAdapter.STATE_OFF -> {
          BluetoothHelper.stopServer()
          BluetoothHelper.stopAdvertising()
        }
      }// Do nothing
    }
  }

  private val mLeScanCallback = object : ScanCallback() {

    override fun onScanResult(callbackType: Int, result: ScanResult?) {
      var isDeviceFound = false
      if (result != null) {
        val device = result.device
        if (device != null) {
          val deviceName = device.name
          if (deviceName != null && deviceName.length > 0) {
            if (deviceName == BluetoothHelper.MOBILE_DEVICE_NAME) {
              isDeviceFound = true
              mDeviceAddress = device.address
            }
          }
        }
      }

      if (isDeviceFound && !mConnected) {
        val gattServiceIntent = Intent(this@MainActivity, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
        mConnected = true
        BluetoothHelper.startAdvertising()
        BluetoothHelper.startServer(this@MainActivity, mGattServerCallback)
      }
    }
  }

  private val mServiceConnection = object : ServiceConnection {

    override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
      mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
      if (mBluetoothLeService!!.notInitialize()) {
        Log.e(TAG, "Unable to initialize Bluetooth")
        finish()
      }

      val result = mBluetoothLeService!!.connect(mDeviceAddress)
      Log.d(TAG, "Connect request result=$result")
      mConnected = true
      if (mScanning) {
        mScanning = false
//        bleScanner!!.stopScan(mLeScanCallback)
      }
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
      mBluetoothLeService = null
    }
  }

  /**
   * Callback to handle incoming requests to the GATT server.
   * All read/write requests for characteristics and descriptors are handled here.
   */
  private val mGattServerCallback = object : BluetoothGattServerCallback() {

    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        Log.i(TAG, "BluetoothDevice CONNECTED: $device")
        mRegisteredDevices.add(device)
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        Log.i(TAG, "BluetoothDevice DISCONNECTED: $device")
        //Remove device from any active subscriptions
        mRegisteredDevices.remove(device)
      }
    }

    override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                             characteristic: BluetoothGattCharacteristic) {
      if (RemoteLedProfile.REMOTE_LED_DATA == characteristic.uuid) {
        Log.i(TAG, "Read data")
        BluetoothHelper.bluetoothGattServer!!.sendResponse(device,
            requestId,
            BluetoothGatt.GATT_SUCCESS,
            0, null)
      } else {
        // Invalid characteristic
        Log.w(TAG, "Invalid Characteristic Read: " + characteristic.uuid)
        BluetoothHelper.bluetoothGattServer!!.sendResponse(device,
            requestId,
            BluetoothGatt.GATT_FAILURE,
            0, null)
      }
    }

    override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                         descriptor: BluetoothGattDescriptor) {
      BluetoothHelper.bluetoothGattServer!!.sendResponse(device,
          requestId,
          BluetoothGatt.GATT_FAILURE,
          0, null)
    }

    override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int,
                                          descriptor: BluetoothGattDescriptor,
                                          preparedWrite: Boolean, responseNeeded: Boolean,
                                          offset: Int, value: ByteArray) {
      if (responseNeeded) {
        BluetoothHelper.bluetoothGattServer!!.sendResponse(device,
            requestId,
            BluetoothGatt.GATT_FAILURE,
            0, null)
      }
    }
  }

  // Handles various events fired by the Service.
  // ACTION_GATT_CONNECTED: connected to a GATT server.
  // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
  // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
  // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
  //                        or notification operations.
  private val mGattUpdateReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
        mConnected = true

      } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
        mConnected = false

      } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
        // Show all the supported services and characteristics on the user interface.
        val services = mBluetoothLeService!!.supportedGattServices
        if (services != null) {
          for (gattService in services) {
            if (gattService.uuid == RemoteLedProfile.REMOTE_LED_SERVICE) {
              val characteristic = gattService.getCharacteristic(RemoteLedProfile.REMOTE_LED_DATA)
              if (characteristic != null) {
                val charaProp = characteristic.properties
                if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                  // If there is an active notification on a characteristic, clear
                  // it first so it doesn't update the data field on the user interface.
                  if (mNotifyCharacteristic != null) {
                    mBluetoothLeService!!.setCharacteristicNotification(
                        mNotifyCharacteristic!!, false)
                    mNotifyCharacteristic = null
                  }
                  mBluetoothLeService!!.readCharacteristic(characteristic)
                }
                if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                  mNotifyCharacteristic = characteristic
                  mBluetoothLeService!!.setCharacteristicNotification(
                      characteristic, true)
                }
              }
            }
          }
        }
      } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
        val data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
        if (data.contains("false")) {
          setLedValue(false)
        } else if (data.contains("true")) {
          setLedValue(true)
        }
        notifyRegisteredDevices()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.i(TAG, "Starting ButtonActivity")

    try {
      Log.i(TAG, "Configuring GPIO pins")
      mLedGpio = PeripheralManager.getInstance().openGpio(BoardDefaults.gpioForLED)
      mLedGpio!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

      Log.i(TAG, "Registering button driver")
      // Initialize and register the InputDriver that will emit SPACE key events
      // on GPIO state changes.
      mButtonInputDriver = ButtonInputDriver(
          BoardDefaults.gpioForButton,
          Button.LogicState.PRESSED_WHEN_LOW,
          KeyEvent.KEYCODE_SPACE)
      mHandler = Handler()
      BluetoothHelper.bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
      bleScanner = mBluetoothAdapter!!.bluetoothLeScanner
      if (mBluetoothAdapter != null) {
        if (mBluetoothAdapter!!.isEnabled) {
          Log.d(TAG, "Bluetooth Adapter is already enabled.")
          initScan()
        } else {
          Log.d(TAG, "Bluetooth adapter not enabled. Enabling.")
          mBluetoothAdapter!!.enable()
        }
      }
    } catch (e: IOException) {
      Log.e(TAG, "Error configuring GPIO pins", e)
    }

  }

  private fun initScan() {
    if (mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) {
      Log.e(TAG, "Bluetooth adapter not available or not enabled.")
      return
    }
    //setupBTProfiles();
    Log.d(TAG, "Set up Bluetooth Adapter name and profile")
    mBluetoothAdapter!!.name = BluetoothHelper.ANDROID_THINGS_DEVICE_NAME
    scanLeDevice()
  }

  private fun scanLeDevice() {
    // Stops scanning after a pre-defined scan period.
    mHandler!!.postDelayed({
      mScanning = false
      bleScanner!!.stopScan(mLeScanCallback)
    }, BluetoothHelper.SCAN_PERIOD)

    mScanning = true
    bleScanner!!.startScan(mLeScanCallback)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == BluetoothHelper.REQUEST_ENABLE_BT) {
      Log.d(TAG, "Enable discoverable returned with result $resultCode")

      // ResultCode, as described in BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE, is either
      // RESULT_CANCELED or the number of milliseconds that the device will stay in
      // discoverable mode. In a regular Android device, the user will see a popup requesting
      // authorization, and if they cancel, RESULT_CANCELED is returned. In Android Things,
      // on the other hand, the authorization for pairing is always given without user
      // interference, so RESULT_CANCELED should never be returned.
      if (resultCode == Activity.RESULT_CANCELED) {
        Log.e(TAG, "Enable discoverable has been cancelled by the user. " + "This should never happen in an Android Things device.")
      }
    }
  }

  /**
   * Send a remote led service notification to any devices that are subscribed
   * to the characteristic.
   */
  private fun notifyRegisteredDevices() {
    if (mRegisteredDevices.isEmpty()) {
      Log.i(TAG, "No subscribers registered")
      return
    }

    Log.i(TAG, "Sending update to " + mRegisteredDevices.size + " subscribers")
    for (device in mRegisteredDevices) {
      val ledDataCharacteristic = BluetoothHelper.bluetoothGattServer!!
          .getService(RemoteLedProfile.REMOTE_LED_SERVICE)
          .getCharacteristic(RemoteLedProfile.REMOTE_LED_DATA)
      ledDataCharacteristic.setValue(true.toString())
      BluetoothHelper.bluetoothGattServer!!.notifyCharacteristicChanged(device, ledDataCharacteristic, false)
    }
  }


  override fun onStart() {
    super.onStart()
    mButtonInputDriver!!.register()
    registerReceiver(mGattUpdateReceiver, BluetoothHelper.makeGattUpdateIntentFilter())
  }


  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    if (keyCode == KeyEvent.KEYCODE_SPACE) {
      // Turn on the LED
      setLedValue(true)
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    if (keyCode == KeyEvent.KEYCODE_SPACE) {
      // Turn off the LED
      setLedValue(false)
      return true
    }

    return super.onKeyUp(keyCode, event)
  }

  /**
   * Update the value of the LED output.
   */
  private fun setLedValue(value: Boolean) {
    try {
      mLedGpio!!.value = value
    } catch (e: IOException) {
      Log.e(TAG, "Error updating GPIO value", e)
    }

  }

  override fun onPause() {
    super.onPause()
    unregisterReceiver(mGattUpdateReceiver)
  }

  override fun onDestroy() {
    super.onDestroy()

    if (mButtonInputDriver != null) {
      mButtonInputDriver!!.unregister()
      try {
        mButtonInputDriver!!.close()
      } catch (e: IOException) {
        Log.e(TAG, "Error closing Button driver", e)
      } finally {
        mButtonInputDriver = null
      }
    }

    if (mLedGpio != null) {
      try {
        mLedGpio!!.close()
      } catch (e: IOException) {
        Log.e(TAG, "Error closing LED GPIO", e)
      } finally {
        mLedGpio = null
      }
      mLedGpio = null
    }

    if (BluetoothHelper.bluetoothManager!!.adapter.isEnabled) {
      BluetoothHelper.stopServer()
      BluetoothHelper.stopAdvertising()
    }

    unregisterReceiver(mBluetoothReceiver)
    unbindService(mServiceConnection)
    mBluetoothLeService = null
  }

  companion object {
    private val TAG = MainActivity::class.java!!.getSimpleName()
  }
}

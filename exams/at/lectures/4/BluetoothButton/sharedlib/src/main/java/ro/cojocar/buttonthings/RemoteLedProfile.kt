package ro.cojocar.buttonthings

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

import java.util.UUID

object RemoteLedProfile {

  /* Remote LED Service UUID */
  var REMOTE_LED_SERVICE = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
  /* Remote LED Data Characteristic */
  var REMOTE_LED_DATA = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb")

  /**
   * Return a configured [BluetoothGattService] instance for the
   * Remote LED Service.
   */
  fun createRemoteLedService(): BluetoothGattService {
    val service = BluetoothGattService(REMOTE_LED_SERVICE,
        BluetoothGattService.SERVICE_TYPE_PRIMARY)

    val ledData = BluetoothGattCharacteristic(REMOTE_LED_DATA,
        //Read-only characteristic, supports notifications
        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_READ)

    service.addCharacteristic(ledData)

    return service
  }
}

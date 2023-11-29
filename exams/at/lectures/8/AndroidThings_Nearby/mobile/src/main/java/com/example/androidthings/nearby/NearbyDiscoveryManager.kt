package com.example.androidthings.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class NearbyDiscoveryManager(private val ctx: Context, private val listener: EventListener) {

  private lateinit var currentEndpoint: String

  private val endpointDiscoveryCB = object : EndpointDiscoveryCallback() {
    override fun onEndpointFound(s: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
      logi("Endpoint found [$s]. Connecting....")
      listener.onDiscovered()
      getConnection(s)
    }

    override fun onEndpointLost(s: String) {
      loge("Endpoint lost [$s]")
    }
  }

  private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
    override fun onConnectionInitiated(s: String, connectionInfo: ConnectionInfo) {
      logi("Connected to endpoint [$s]")
      this@NearbyDiscoveryManager.currentEndpoint = s
      Nearby.getConnectionsClient(ctx).acceptConnection(s, payloadCallback)
    }

    override fun onConnectionResult(s: String, connectionResolution: ConnectionResolution) {
      when (connectionResolution.status.statusCode) {
        ConnectionsStatusCodes.STATUS_OK -> listener.onConnected()
        ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> logi("Connection rejected")
        ConnectionsStatusCodes.STATUS_ERROR -> logi("Connection error")
      }
    }

    override fun onDisconnected(s: String) {
    }
  }

  private val payloadCallback = object : PayloadCallback() {
    override fun onPayloadReceived(s: String, payload: Payload) {
      logi("Payload received")
      val b = payload.asBytes()
      val content = String(b!!)
      logi("Content [$content]")
    }

    override fun onPayloadTransferUpdate(s: String, payloadTransferUpdate: PayloadTransferUpdate) {
      logd("Payload Transfer update [$s]")
      if (payloadTransferUpdate.status == PayloadTransferUpdate.Status.SUCCESS) {

        logi("Payload received from Endpoint [$s]")
      }
    }
  }

  init {

    logi("NearbyDsvManager")
    val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()

    Nearby.getConnectionsClient(ctx)
        .startDiscovery(SERVICE_ID,
            endpointDiscoveryCB,
            discoveryOptions)
        .addOnSuccessListener {
          logi("OnSuccess...")
          listener.startDiscovering()
        }
        .addOnFailureListener { e ->
          loge("OnFailure", e)
        }
  }


  private fun getConnection(endpointId: String) {
    Nearby.getConnectionsClient(ctx)
        .requestConnection(endpointId, endpointId, connectionLifecycleCallback)
        .addOnSuccessListener { logd("Requesting connection..") }
        .addOnFailureListener { e -> loge("Error requesting connection", e) }

  }


  fun sendData(data: String) {
    logi("Sending data [$data]")
    logi("Current endpoint [$currentEndpoint]")
    logd("Sending data to [$data]")
    val payload = Payload.fromBytes(data.toByteArray())
    Nearby.getConnectionsClient(ctx).sendPayload(currentEndpoint, payload)
  }

  interface EventListener {
    fun onDiscovered()

    fun startDiscovering()

    fun onConnected()
  }

  companion object {
    private const val SERVICE_ID = "com.example.androidthings.nearby"
  }
}

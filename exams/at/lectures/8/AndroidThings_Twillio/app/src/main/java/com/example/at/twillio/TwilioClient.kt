package com.example.at.twillio


import java.io.IOException

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response


object TwilioClient {


  private const val TWILLIO_BASE_URL = "https://api.twilio.com/2010-04-01/Accounts/$TWILLIO_ACCOUNT_SID"
  private const val TWILLIO_MESSAGE_URL = "$TWILLIO_BASE_URL/Messages.json"
  private const val TWILLIO_CALL_URL = "$TWILLIO_BASE_URL/Calls.json"

  private val httpClient = OkHttpClient();

  fun sms(body: String) {
    logd("Sending SMS request...")

    val formBody = FormBody.Builder()
        .add("From", ORIGIN_PHONE_NUMBER)
        .add("To", DEST_PHONE_NUMBER)
        .add("Body", body)
        .build()

    val builder = Request.Builder()
        .url(TWILLIO_MESSAGE_URL)
        .post(formBody)

    execute(builder)
  }


  fun call() {
    logd("Sending call request...")

    val url = "https://someurl.com"

    val formBody = FormBody.Builder()
        .add("From", ORIGIN_PHONE_NUMBER)
        .add("To", DEST_PHONE_NUMBER)
        .add("Url", url)
        .build()

    val builder = Request.Builder()
        .url(TWILLIO_CALL_URL)
        .post(formBody)

    execute(builder)
  }


  private fun execute(builder: Request.Builder) {
    val credential = Credentials.basic(TWILLIO_ACCOUNT_SID, TWILLIO_AUTH_TOKEN)

    val request = builder
        .addHeader("Authorization", credential)
        .build()

    httpClient.newCall(request)?.enqueue(object : Callback {
      @Throws(IOException::class)
      override fun onResponse(call: Call, response: Response) {
        if (response.isSuccessful) {
          logd("Twilio request succeed.")
        } else {
          logd("Twilio request failed. Make sure you have the correct configuration")
          logd(response.code().toString() + " " + response.message())
          logd(response.body()!!.string())
        }
      }

      override fun onFailure(call: Call, e: IOException) {
        logd("Twilio request failed.", e)
      }
    })
  }
}

package com.google.firebase.quickstart.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.TwitterAuthProvider
import com.twitter.sdk.android.core.*
import com.twitter.sdk.android.core.identity.TwitterLoginButton
import kotlinx.android.synthetic.main.activity_twitter.*

class TwitterLoginActivity : BaseActivity(), View.OnClickListener {

  private lateinit var auth: FirebaseAuth

  private var loginButton: TwitterLoginButton? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Twitter SDK
    val authConfig = TwitterAuthConfig(
        getString(R.string.twitter_consumer_key),
        getString(R.string.twitter_consumer_secret))

    val twitterConfig = TwitterConfig.Builder(this)
        .twitterAuthConfig(authConfig)
        .build()

    Twitter.initialize(twitterConfig)

    // Inflate layout (must be done after Twitter is configured)
    setContentView(R.layout.activity_twitter)

    buttonTwitterSignout.setOnClickListener(this)

    // Initialize Firebase Auth
    auth = FirebaseAuth.getInstance()

    buttonTwitterLogin?.callback = object : Callback<TwitterSession>() {
      override fun success(result: Result<TwitterSession>) {
        logw("twitterLogin:success$result")
        handleTwitterSession(result.data)
      }

      override fun failure(exception: TwitterException) {
        logw("twitterLogin:failure", exception)
        updateUI(null)
      }
    }
  }

  public override fun onStart() {
    super.onStart()
    // Check if user is signed in (non-null) and update UI accordingly.
    val currentUser = auth.currentUser
    updateUI(currentUser)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    // Pass the activity result to the Twitter login button.
    loginButton!!.onActivityResult(requestCode, resultCode, data)
  }

  private fun handleTwitterSession(session: TwitterSession) {
    logd("handleTwitterSession:$session")
    showProgressDialog()

    val credential = TwitterAuthProvider.getCredential(
        session.authToken.token,
        session.authToken.secret)

    auth.signInWithCredential(credential)
        .addOnCompleteListener(this) { task ->
          if (task.isSuccessful) {
            // Sign in success, update UI with the signed-in user's information
            logd("signInWithCredential:success")
            val user = auth.currentUser
            updateUI(user)
          } else {
            // If sign in fails, display a message to the user.
            logw("signInWithCredential:failure", task.exception)
            Toast.makeText(baseContext, "Authentication failed.",
                Toast.LENGTH_SHORT).show()
            updateUI(null)
          }

          hideProgressDialog()
        }
  }

  private fun signOut() {
    auth.signOut()
    TwitterCore.getInstance().sessionManager.clearActiveSession()

    updateUI(null)
  }

  private fun updateUI(user: FirebaseUser?) {
    hideProgressDialog()
    if (user != null) {
      status.text = getString(R.string.twitter_status_fmt, user.displayName)
      detail.text = getString(R.string.firebase_status_fmt, user.uid)

      buttonTwitterLogin.visibility = View.GONE
      buttonTwitterSignout.visibility = View.VISIBLE
    } else {
      status.setText(R.string.signed_out)
      detail.text = null

      buttonTwitterLogin.visibility = View.VISIBLE
      buttonTwitterSignout.visibility = View.GONE
    }
  }

  override fun onClick(v: View) {
    val i = v.id
    if (i == R.id.buttonTwitterSignout) {
      signOut()
    }
  }
}

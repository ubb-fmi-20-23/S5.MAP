/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.example.basicawarenesssample;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.example.android.common.logger.LogFragment;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResponse;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResponse;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;


/**
 * Sample application which sets up a few context fences using the Awareness API, and takes
 * "snapshots" of data about the user and the device's surroundings.
 * <p>
 * NOTE: for this sample to work, you need to add an API key in the manifest. See
 * https://developers.google.com/awareness/android-api/get-a-key for instructions.
 */
public class MainActivity extends AppCompatActivity {

  // The fence key is how callback code determines which fence fired.
  private final String FENCE_KEY = "fence_key";

  private final String TAG = getClass().getSimpleName();

  private PendingIntent mPendingIntent;

  private FenceReceiver mFenceReceiver;

  private LogFragment mLogFragment;

  // The intent action which will be fired when your fence is triggered.
  private final String FENCE_RECEIVER_ACTION =
      BuildConfig.APPLICATION_ID + "FENCE_RECEIVER_ACTION";


  private static final int MY_PERMISSION_LOCATION = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        printSnapshot();
      }
    });

    Intent intent = new Intent(FENCE_RECEIVER_ACTION);
    mPendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

    mFenceReceiver = new FenceReceiver();
    registerReceiver(mFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

    mLogFragment = (LogFragment) getSupportFragmentManager().findFragmentById(R.id.log_fragment);
  }

  @Override
  protected void onResume() {
    super.onResume();
    setupFences();
  }

  @Override
  protected void onPause() {
    // Unregister the fence:
    Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
        .removeFence(FENCE_KEY)
        .build())
        .addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {
            Log.i(TAG, "Fence was successfully unregistered.");
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Fence could not be unregistered: " + e);
          }
        });

    super.onPause();
  }

  @Override
  protected void onStop() {
    if (mFenceReceiver != null) {
      unregisterReceiver(mFenceReceiver);
      mFenceReceiver = null;
    }
    super.onStop();
  }

  /**
   * Uses the snapshot API to print out some contextual information the device is "aware" of.
   */
  private void printSnapshot() {
    // Clear the console screen of previous snapshot / fence log data
    mLogFragment.getLogView().setText("");

    // Each type of contextual information in the snapshot API has a corresponding "get" method.
    //  For instance, this is how to get the user's current Activity.
    Awareness.getSnapshotClient(this).getDetectedActivity()
        .addOnSuccessListener(new OnSuccessListener<DetectedActivityResponse>() {
          @Override
          public void onSuccess(DetectedActivityResponse dar) {
            ActivityRecognitionResult arr = dar.getActivityRecognitionResult();
            // getMostProbableActivity() is good enough for basic Activity detection.
            // To work within a threshold of confidence,
            // use ActivityRecognitionResult.getProbableActivities() to get a list of
            // potential current activities, and check the confidence of each one.
            DetectedActivity probableActivity = arr.getMostProbableActivity();

            int confidence = probableActivity.getConfidence();
            String activityStr = probableActivity.toString();
            mLogFragment.getLogView().println("Activity: " + activityStr
                + ", Confidence: " + confidence + "/100");
          }
        })

        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Could not detect activity: " + e);
          }
        });

    // Pulling headphone state is similar, but doesn't involve analyzing confidence.
    Awareness.getSnapshotClient(this).getHeadphoneState()
        .addOnSuccessListener(new OnSuccessListener<HeadphoneStateResponse>() {
          @Override
          public void onSuccess(HeadphoneStateResponse headphoneStateResponse) {
            HeadphoneState headphoneState = headphoneStateResponse.getHeadphoneState();
            boolean pluggedIn = headphoneState.getState() == HeadphoneState.PLUGGED_IN;
            String stateStr =
                "Headphones are " + (pluggedIn ? "plugged in" : "unplugged");
            mLogFragment.getLogView().println(stateStr);
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Could not get headphone state: " + e);
          }
        });

    // Some of the data available via Snapshot API requires permissions that must be checked
    // at runtime.  Weather snapshots are a good example of this.  Since weather is protected
    // by a runtime permission, and permission request callbacks will happen asynchronously,
    // the easiest thing to do is put weather snapshot code in its own method.  That way it
    // can be called from here when permission has already been granted on subsequent runs,
    // and from the permission request callback code when permission is first granted.
    checkAndRequestWeatherPermissions();
  }

  /**
   * Helper method to retrieve weather data using the Snapshot API.  Since Weather is protected
   * by a runtime permission, this snapshot code is going to be called in multiple places:
   * {@link #printSnapshot()} when the permission has already been accepted, and
   * {@link #onRequestPermissionsResult(int, String[], int[])} when the permission is requested
   * and has been granted.
   */
  private void getWeatherSnapshot() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      Awareness.getSnapshotClient(this).getWeather()
          .addOnSuccessListener(new OnSuccessListener<WeatherResponse>() {
            @Override
            public void onSuccess(WeatherResponse weatherResponse) {
              Weather weather = weatherResponse.getWeather();
              weather.getConditions();
              mLogFragment.getLogView().println("Weather: " + weather);
            }
          })
          .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
              Log.e(TAG, "Could not get weather: " + e);
            }
          });
    }
  }

  /**
   * Helper method to handle requesting the runtime permissions required for weather snapshots.
   */
  private void checkAndRequestWeatherPermissions() {
    if (ContextCompat.checkSelfPermission(
        MainActivity.this,
        Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) {

      if (ActivityCompat.shouldShowRequestPermissionRationale
          (this, Manifest.permission.ACCESS_FINE_LOCATION)) {
        Log.i(TAG, "Permission previously denied and app shouldn't ask again.  Skipping" +
            " weather snapshot.");
      } else {
        ActivityCompat.requestPermissions(
            MainActivity.this,
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
            MY_PERMISSION_LOCATION
        );
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                         @NonNull int[] grantResults) {
    switch (requestCode) {
      case MY_PERMISSION_LOCATION: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          getWeatherSnapshot();
        } else {
          Log.i(TAG, "Location permission denied.  Weather snapshot skipped.");
        }
      }
    }
  }

  /**
   * Sets up {@link AwarenessFence}'s for the sample app, and registers callbacks for them
   * with a custom {@link BroadcastReceiver}
   */
  private void setupFences() {
    // DetectedActivityFence will fire when it detects the user performing the specified
    // activity.  In this case it's walking.
    AwarenessFence walkingFence = DetectedActivityFence.during(DetectedActivityFence.STILL);

    // There are lots of cases where it's handy for the device to know if headphones have been
    // plugged in or unplugged.  For instance, if a music app detected your headphones fell out
    // when you were in a library, it'd be pretty considerate of the app to pause itself before
    // the user got in trouble.
    AwarenessFence headphoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);

    // Combines multiple fences into a compound fence.  While the first two fences trigger
    // individually, this fence will only trigger its callback when all of its member fences
    // hit a true state.
    AwarenessFence walkingWithHeadphones = AwarenessFence.and(walkingFence, headphoneFence);

    // We can even nest compound fences.  Using both "and" and "or" compound fences, this
    // compound fence will determine when the user has headphones in and is engaging in at least
    // one form of exercise.
    // The below breaks down to "(headphones plugged in) AND (walking OR running OR bicycling)"
    AwarenessFence exercisingWithHeadphonesFence = AwarenessFence.and(
        headphoneFence,
        AwarenessFence.or(
            walkingFence,
            DetectedActivityFence.during(DetectedActivityFence.RUNNING),
            DetectedActivityFence.during(DetectedActivityFence.ON_BICYCLE)));


    // Now that we have an interesting, complex condition, register the fence to receive
    // callbacks.

    // Register the fence to receive callbacks.
    Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
        .addFence(FENCE_KEY, exercisingWithHeadphonesFence, mPendingIntent)
        .build())
        .addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {
            Log.i(TAG, "Fence was successfully registered.");
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Fence could not be registered: " + e);
          }
        });
  }

  /**
   * A basic BroadcastReceiver to handle intents from from the Awareness API.
   */
  public class FenceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (!TextUtils.equals(FENCE_RECEIVER_ACTION, intent.getAction())) {
        mLogFragment.getLogView()
            .println("Received an unsupported action in FenceReceiver: action="
                + intent.getAction());
        return;
      }

      // The state information for the given fence is em
      FenceState fenceState = FenceState.extract(intent);

      if (TextUtils.equals(fenceState.getFenceKey(), FENCE_KEY)) {
        String fenceStateStr;
        switch (fenceState.getCurrentState()) {
          case FenceState.TRUE:
            fenceStateStr = "true";
            break;
          case FenceState.FALSE:
            fenceStateStr = "false";
            break;
          case FenceState.UNKNOWN:
            fenceStateStr = "unknown";
            break;
          default:
            fenceStateStr = "unknown value";
        }
        mLogFragment.getLogView().println("Fence state: " + fenceStateStr);
      }
    }
  }
}
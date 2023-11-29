/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.musicplayercodelab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat


/**
 * Keeps track of a notification and updates it automatically for a given MediaSession. This is
 * required so that the music service don't get killed during playback.
 */
class MediaNotificationManager(private val mService: MusicService) : BroadcastReceiver() {

  private val mContext: Context

  private val mNotificationManager: NotificationManager

  private val mPlayAction: NotificationCompat.Action
  private val mPauseAction: NotificationCompat.Action
  private val mNextAction: NotificationCompat.Action
  private val mPrevAction: NotificationCompat.Action

  private var mStarted: Boolean = false

  init {
    mContext = mService.baseContext

    val pkg = mService.packageName
    val playIntent = PendingIntent.getBroadcast(
        mService,
        REQUEST_CODE,
        Intent(ACTION_PLAY).setPackage(pkg),
        PendingIntent.FLAG_CANCEL_CURRENT)
    val pauseIntent = PendingIntent.getBroadcast(
        mService,
        REQUEST_CODE,
        Intent(ACTION_PAUSE).setPackage(pkg),
        PendingIntent.FLAG_CANCEL_CURRENT)
    val nextIntent = PendingIntent.getBroadcast(
        mService,
        REQUEST_CODE,
        Intent(ACTION_NEXT).setPackage(pkg),
        PendingIntent.FLAG_CANCEL_CURRENT)
    val prevIntent = PendingIntent.getBroadcast(
        mService,
        REQUEST_CODE,
        Intent(ACTION_PREV).setPackage(pkg),
        PendingIntent.FLAG_CANCEL_CURRENT)

    mPlayAction = NotificationCompat.Action(
        R.drawable.ic_play_arrow_white_24dp,
        mService.getString(R.string.label_play),
        playIntent)
    mPauseAction = NotificationCompat.Action(
        R.drawable.ic_pause_white_24dp,
        mService.getString(R.string.label_pause),
        pauseIntent)
    mNextAction = NotificationCompat.Action(
        R.drawable.ic_skip_next_white_24dp,
        mService.getString(R.string.label_next),
        nextIntent)
    mPrevAction = NotificationCompat.Action(
        R.drawable.ic_skip_previous_white_24dp,
        mService.getString(R.string.label_previous),
        prevIntent)

    val filter = IntentFilter()
    filter.addAction(ACTION_NEXT)
    filter.addAction(ACTION_PAUSE)
    filter.addAction(ACTION_PLAY)
    filter.addAction(ACTION_PREV)

    mService.registerReceiver(this, filter)

    mNotificationManager = mService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Cancel all notifications to handle the case where the Service was killed and
    // restarted by the system.
    mNotificationManager.cancelAll()

    // Make sure that there is a notification channel on API 26+ devices
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannel()
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action
    when (action) {
      ACTION_PAUSE -> mService.mCallback.onPause()
      ACTION_PLAY -> mService.mCallback.onPlay()
      ACTION_NEXT -> mService.mCallback.onSkipToNext()
      ACTION_PREV -> mService.mCallback.onSkipToPrevious()
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationChannel() {
    val mChannel = NotificationChannel(
        CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
    mChannel.description = CHANNEL_DESCRIPTION
    mChannel.setShowBadge(false)
    mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    mNotificationManager.createNotificationChannel(mChannel)
  }

  fun update(
      metadata: MediaMetadataCompat?,
      state: PlaybackStateCompat?,
      token: MediaSessionCompat.Token) {
    if (state == null
        || state.state == PlaybackStateCompat.STATE_STOPPED
        || state.state == PlaybackStateCompat.STATE_NONE) {
      mService.stopForeground(true)
      try {
        mService.unregisterReceiver(this)
      } catch (ex: IllegalArgumentException) {
        // ignore receiver not registered
      }

      mService.stopSelf()
      return
    }
    if (metadata == null) {
      return
    }
    val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
    val notificationBuilder = NotificationCompat.Builder(mService, CHANNEL_ID)
    val description = metadata.description

    notificationBuilder
        .setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(0, 1, 2))
        .setColor(
            mService.application.resources.getColor(R.color.notification_bg))
        .setSmallIcon(R.drawable.ic_notification)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(createContentIntent())
        .setContentTitle(description.title)
        .setContentText(description.subtitle)
        .setLargeIcon(MusicLibrary.getAlbumBitmap(mService, description.mediaId!!))
        .setOngoing(isPlaying)
        .setWhen(if (isPlaying) System.currentTimeMillis() - state.position else 0)
        .setShowWhen(isPlaying)
        .setUsesChronometer(isPlaying)

    // If skip to next action is enabled
    if (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L) {
      notificationBuilder.addAction(mPrevAction)
    }

    notificationBuilder.addAction(if (isPlaying) mPauseAction else mPlayAction)

    // If skip to prev action is enabled
    if (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
      notificationBuilder.addAction(mNextAction)
    }

    val notification = notificationBuilder.build()

    if (isPlaying && !mStarted) {
      val intent = Intent(mContext, MusicService::class.java)
      ContextCompat.startForegroundService(mContext, intent)
      mService.startForeground(NOTIFICATION_ID, notification)
      mStarted = true
    } else {
      if (!isPlaying) {
        mService.stopForeground(false)
        mStarted = false
      }
      mNotificationManager.notify(NOTIFICATION_ID, notification)
    }
  }

  private fun createContentIntent(): PendingIntent {
    val openUI = Intent(mService, MusicPlayerActivity::class.java)
    openUI.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    return PendingIntent.getActivity(
        mService, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT)
  }

  companion object {
    private val NOTIFICATION_ID = 412
    private val REQUEST_CODE = 100

    private val CHANNEL_ID = "media_playback_channel"
    private val CHANNEL_NAME = "Media playback"
    private val CHANNEL_DESCRIPTION = "Media playback controls"

    private val ACTION_PAUSE = "com.example.android.musicplayercodelab.pause"
    private val ACTION_PLAY = "com.example.android.musicplayercodelab.play"
    private val ACTION_NEXT = "com.example.android.musicplayercodelab.next"
    private val ACTION_PREV = "com.example.android.musicplayercodelab.prev"
  }
}

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

import android.app.Activity
import android.graphics.drawable.AnimationDrawable
import android.support.v4.media.MediaDescriptionCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.media_list_item.*


internal class MediaItemViewHolder {

  lateinit var mImageView: ImageView
  lateinit var mTitleView: TextView
  lateinit var mDescriptionView: TextView

  companion object {

    private const val STATE_INVALID = -1
    const val STATE_NONE = 0
    private const val STATE_PLAYABLE = 1
    const val STATE_PAUSED = 2
    const val STATE_PLAYING = 3

    fun setupView(
        activity: Activity,
        convertView: View?,
        parent: ViewGroup,
        description: MediaDescriptionCompat,
        state: Int): View {
      var convertView = convertView

      val holder: MediaItemViewHolder

      var cachedState: Int? = STATE_INVALID

      if (convertView == null) {
        convertView = LayoutInflater.from(activity).inflate(R.layout.media_list_item, parent, false)
        holder = MediaItemViewHolder()
        holder.mImageView = convertView.findViewById<View>(R.id.play_eq) as ImageView
        holder.mTitleView = convertView.findViewById<View>(R.id.title) as TextView
        holder.mDescriptionView = convertView.findViewById<View>(R.id.description) as TextView
        convertView.tag = holder
      } else {
        holder = convertView.tag as MediaItemViewHolder
        cachedState = convertView.getTag(R.id.tag_mediaitem_state_cache) as Int
      }

      holder.mTitleView.text = description.title
      holder.mDescriptionView.text = description.subtitle

      // If the state of convertView is different, we need to adapt the view to the
      // new state.
      if (cachedState == null || cachedState != state) {
        when (state) {
          STATE_PLAYABLE -> {
            holder.mImageView.setImageDrawable(
                activity.resources
                    .getDrawable(R.drawable.ic_play_arrow_black_36dp))
            holder.mImageView.visibility = View.VISIBLE
          }
          STATE_PLAYING -> {
            val animation = activity.resources
                .getDrawable(R.drawable.ic_equalizer_white_36dp) as AnimationDrawable
            holder.mImageView.setImageDrawable(animation)
            holder.mImageView.visibility = View.VISIBLE
            animation.start()
          }
          STATE_PAUSED -> {
            holder.mImageView.setImageDrawable(
                activity.resources
                    .getDrawable(R.drawable.ic_equalizer1_white_36dp))
            holder.mImageView.visibility = View.VISIBLE
          }
          else -> holder.mImageView.visibility = View.GONE
        }
        convertView?.setTag(R.id.tag_mediaitem_state_cache, state)
      }

      return convertView!!
    }
  }
}

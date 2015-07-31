/*
 * Copyright (C) 2015 Brian Wernick
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

package com.devbrackets.android.exomedia.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.devbrackets.android.exomedia.R;
import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.util.TimeFormatUtil;

/**
 * Provides playback controls for the EMVideoView on Mobile
 * (Phone, Tablet, etc.) devices.
 */
public class DefaultControlsMobile extends DefaultControls {
    private SeekBar seekBar;
    private LinearLayout controlsContainer;
    private boolean pausedForSeek = false;

    public DefaultControlsMobile(Context context) {
        super(context);
    }

    public DefaultControlsMobile(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public DefaultControlsMobile(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DefaultControlsMobile(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.exomedia_video_controls_overlay;
    }

    /**
     * Used to update the control view visibilities to indicate that the video
     * is loading.  This is different from using {@link #loadCompleted()} and {@link #restartLoading()}
     * because those update additional information.
     *
     * @param isLoading True if loading progress should be shown
     */
    @Override
    public void setLoading(boolean isLoading) {
        super.setLoading(isLoading);
        controlsContainer.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    /**
     * Sets the current video position, updating the seek bar
     * and the current time field
     *
     * @param position The position in milliseconds
     */
    @Override
    public void setPosition(long position) {
        currentTime.setText(TimeFormatUtil.formatMs(position));
        seekBar.setProgress((int) position);
    }

    /**
     * Sets the video duration in Milliseconds to display
     * at the end of the progress bar
     *
     * @param duration The duration of the video in milliseconds
     */
    @Override
    public void setDuration(long duration) {
        if (duration != seekBar.getMax()) {
            endTime.setText(TimeFormatUtil.formatMs(duration));
            seekBar.setMax((int) duration);
        }
    }

    /**
     * Performs the progress update on the current time field,
     * and the seek bar
     *
     * @param event The most recent progress
     */
    @Override
    public void setProgressEvent(EMMediaProgressEvent event) {
        if (!userInteracting) {
            seekBar.setSecondaryProgress((int) (seekBar.getMax() * event.getBufferPercentFloat()));
            seekBar.setProgress((int) event.getPosition());
            currentTime.setText(TimeFormatUtil.formatMs(event.getPosition()));
        }
    }

    @Override
    protected void retrieveViews() {
        super.retrieveViews();
        seekBar = (SeekBar) findViewById(R.id.exomedia_controls_video_seek);
        controlsContainer = (LinearLayout) findViewById(R.id.exomedia_controls_interactive_container);
    }

    @Override
    protected void registerClickListeners() {
        super.registerClickListeners();
        seekBar.setOnSeekBarChangeListener(new SeekBarChanged());
    }

    /**
     * Listens to the seek bar change events and correctly handles the changes
     */
    private class SeekBarChanged implements SeekBar.OnSeekBarChangeListener {
        private int seekToTime;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }

            seekToTime = progress;
            if (seekCallbacks != null && seekCallbacks.onSeekStarted()) {
                return;
            }

            if (currentTime != null) {
                currentTime.setText(TimeFormatUtil.formatMs(progress));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            userInteracting = true;

            if (videoView.isPlaying()) {
                pausedForSeek = true;
                videoView.pause();
            }

            //Make sure to keep the controls visible during seek
            show();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            userInteracting = false;
            if (seekCallbacks != null && seekCallbacks.onSeekEnded(seekToTime)) {
                return;
            }

            videoView.seekTo(seekToTime);

            if (pausedForSeek) {
                pausedForSeek = false;
                videoView.start();
                hideDelayed(hideDelay);
            }
        }
    }
}
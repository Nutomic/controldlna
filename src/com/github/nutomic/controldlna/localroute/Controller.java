/*
Copyright (c) 2013, Felix Ableitner
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.nutomic.controldlna.localroute;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouter.ControlRequestCallback;
import android.util.Log;

/**
 * Receives control intents through media route and executes them on a MediaPlayer.
 * 
 * @author felix
 *
 */
public class Controller extends MediaRouteProvider.RouteController implements 
		MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
	
	private static final String TAG = "Controller";
	
	private Context mContext;
	
	private AudioManager mAudio;
    
    AudioManager.OnAudioFocusChangeListener mFocusListener;
	
    private final String mRouteId;
    
    private String mItemId;
    
    private int mState;
    
	private MediaPlayer mPlayer = new MediaPlayer();

    public Controller(String routeId, Context context) {
    	mContext = context;
        mRouteId = routeId;
        mAudio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
    }

    @Override
    public void onRelease() {
    	mPlayer.release();
    }

    @Override
    public void onSelect() {
    	mAudio.requestAudioFocus(mFocusListener, AudioManager.STREAM_MUSIC, 
    			AudioManager.AUDIOFOCUS_GAIN);
    }

    @Override
    public void onUnselect() {
    	mAudio.abandonAudioFocus(mFocusListener);
    }

    @Override
    public void onSetVolume(int volume) {
        mAudio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    @Override
    public void onUpdateVolume(int delta) {
        int currentVolume = mAudio.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudio.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume + delta, 0);
    }

    @Override
    public boolean onControlRequest(Intent intent, ControlRequestCallback callback) {
    	String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
    	String itemId = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
        if (intent.getAction().equals(MediaControlIntent.ACTION_PLAY)) {
        	try {
        		mPlayer.reset();
				mPlayer.setDataSource(mContext, intent.getData());
				mPlayer.prepareAsync();
				mItemId = intent.getDataString();
				mState = MediaItemStatus.PLAYBACK_STATE_BUFFERING;
				getStatus(mItemId, mRouteId, callback);
            	return true;
			} catch (IllegalArgumentException e) {
				mState = MediaItemStatus.PLAYBACK_STATE_ERROR;
				Log.d(TAG, "Failed to start playback", e);
			} catch (IOException e) {
				mState = MediaItemStatus.PLAYBACK_STATE_ERROR;
				Log.d(TAG, "Failed to start playback", e);
			}
    	}
        else if (intent.getAction().equals(MediaControlIntent.ACTION_PAUSE)) {
        	mPlayer.pause();
        	mState = MediaItemStatus.PLAYBACK_STATE_PAUSED;
            return true;
    	}
        else if (intent.getAction().equals(MediaControlIntent.ACTION_RESUME)) {
        	mPlayer.start();
        	mState = MediaItemStatus.PLAYBACK_STATE_PLAYING;
            return true;
    	}
        else if (intent.getAction().equals(MediaControlIntent.ACTION_STOP)) {
        	mPlayer.stop();
        	mState = MediaItemStatus.PLAYBACK_STATE_CANCELED;
            return true;
    	}
        else if (intent.getAction().equals(MediaControlIntent.ACTION_SEEK)) {
        	mPlayer.seekTo((int) intent.getLongExtra(
                				MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0));
        	getStatus(itemId, sessionId, callback);
            return true;
    	}
        else if(intent.getAction().equals(MediaControlIntent.ACTION_GET_STATUS)) {
        	getStatus(itemId, sessionId, callback);
            return true;
        }
		return false;
    }
    
    private void getStatus(String itemId, String sessionId, ControlRequestCallback callback) {
    	if (callback == null)
    		return;

		Bundle status = null;
		
		if (mItemId.equals(itemId)) {
			status = new MediaItemStatus.Builder(mState)
					.setContentPosition(mPlayer.getCurrentPosition())
					.setContentDuration(mPlayer.getDuration())
					.setTimestamp(SystemClock.elapsedRealtime())
					.build().asBundle();

			status.putString(MediaControlIntent.EXTRA_SESSION_ID, mRouteId);
			status.putString(MediaControlIntent.EXTRA_ITEM_ID, mItemId);
		}
		else
			status = new MediaItemStatus.Builder(MediaItemStatus.PLAYBACK_STATE_INVALIDATED)
					.build().asBundle();

		callback.onResult(status);    	
    }

    /**
     * Sets state to finished. 
     * 
     * Note: Do not set the listener before play() is called 
     *       (or this will be called immediately).
     */
	@Override
	public void onCompletion(MediaPlayer mp) {
		mState = MediaItemStatus.PLAYBACK_STATE_FINISHED;
		mPlayer.setOnCompletionListener(null);
	}

	/**
	 * Starts playback and sets completion listener.
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		mPlayer.start();
    	mState = MediaItemStatus.PLAYBACK_STATE_PLAYING;
        mPlayer.setOnCompletionListener(this);
	}
}

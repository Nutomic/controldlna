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

package com.github.nutomic.controldlna.upnp;

import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.meta.StateVariableAllowedValueRange;
import org.teleal.cling.support.avtransport.callback.Seek;
import org.teleal.cling.support.model.SeekMode;
import org.teleal.cling.support.renderingcontrol.callback.GetVolume;
import org.teleal.cling.support.renderingcontrol.callback.SetVolume;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;


/**
 * Handles connection to PlayService and provides methods related to playback.
 * 
 * @author Felix Ableitner
 *
 */
public class UpnpPlayer extends UpnpController {
	
	private static final String TAG = "UpnpPlayer";

	private PlayServiceBinder mPlayService;
	
	private int mMinVolume;
	
	private int mMaxVolume;
	
	private int mVolumeStep;
	
	private int mCurrentVolume;
	
	private ServiceConnection mPlayServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			mPlayService = (PlayServiceBinder) service;
        }

        public void onServiceDisconnected(ComponentName className) {
            mPlayService = null;
        }
    };

    @Override
    public void open(Context context) {
    	super.open(context);
        context.bindService(
            new Intent(context, PlayService.class),
            mPlayServiceConnection,
            Context.BIND_AUTO_CREATE
        );
    }
    
    @Override
    public void close(Context context) {
    	super.close(context);
        context.unbindService(mPlayServiceConnection);	
    }

    /**
     * Returns a device service by name for direct queries.
     */
	public Service<?, ?> getService(String name) {
		return getService(mPlayService.getService().getRenderer(), name);
	}
    
    /**
     * Sets an absolute volume.
     */
    public void setVolume(int newVolume) {
    	if (mPlayService.getService().getRenderer() == null)
    		return;

    	if (newVolume > mMaxVolume) newVolume = mMaxVolume;
    	if (newVolume < mMinVolume) newVolume = mMinVolume;
    	
    	mCurrentVolume = newVolume;
		mUpnpService.getControlPoint().execute(
				new SetVolume(getService("RenderingControl"), newVolume) {
			
			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation, 
					UpnpResponse operation, String defaultMessage) {
				Log.d(TAG, "Failed to set new Volume: " + defaultMessage);
			}
		});
    }


	/**
	 * Increases or decreases volume relative to current one.
	 * 
	 * @param amount Amount to change volume by (negative to lower volume).
	 */
    public void changeVolume(int delta) {
    	if (delta > 0 && delta < mVolumeStep) {
    		delta = mVolumeStep;
    	}
    	else if (delta < 0 && delta > -mVolumeStep) {
    		delta = -mVolumeStep;
    	}
    	setVolume(mCurrentVolume + delta);
    }
    
    /**
     * Selects the renderer for playback, applying its minimum and maximum volume.
     */
    public void selectRenderer(Device<?, ?, ?> renderer) {
    	mPlayService.getService().setRenderer(renderer);
    	
    	if (getService("RenderingControl").getStateVariable("Volume") != null) {
        	StateVariableAllowedValueRange volumeRange = 
        			getService("RenderingControl").getStateVariable("Volume")
        					.getTypeDetails().getAllowedValueRange();
        	mMinVolume = (int) volumeRange.getMinimum();
        	mMaxVolume = (int) volumeRange.getMaximum();
        	mVolumeStep = (int) volumeRange.getStep();
        }
        else {
        	mMinVolume = 0;
        	mMaxVolume = 100;
        }
		
		mUpnpService.getControlPoint().execute(
    			new GetVolume(getService("RenderingControl")) {
			
			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation, 
					UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Failed to get current Volume: " + defaultMessage);
			}
			
			@SuppressWarnings("rawtypes")
			@Override
			public void received(ActionInvocation invocation, int currentVolume) {
				mCurrentVolume = currentVolume;
			}
		});	
    }
    
    /**
     * Seeks to the given absolute time in seconds.
     */
    public void seek(int absoluteTime) {
    	if (mPlayService.getService().getRenderer() == null)
    		return;
    	
		mUpnpService.getControlPoint().execute(new Seek(
    			getService(mPlayService.getService().getRenderer(), "AVTransport"), 
    			SeekMode.REL_TIME, 
    			Integer.toString(absoluteTime)) {
			
			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation, 
					UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Seek failed: " + defaultMessage);
			}
		});		
    	
    }
    
    /**
     * Returns the service that handles actual playback.
     */
    public PlayService getPlayService() {
    	return (mPlayService != null) 
    			? mPlayService.getService()
    			: null;
    }

}

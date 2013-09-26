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

package com.github.nutomic.controldlna.mediarouter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.StateVariableAllowedValueRange;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.renderingcontrol.callback.GetVolume;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.github.nutomic.controldlna.upnp.DeviceListener.DeviceListenerCallback;
import com.github.nutomic.controldlna.upnp.UpnpController;
import com.github.nutomic.controldlna.upnp.UpnpPlayer;

/**
 * Allows UPNP playback from within different apps by providing a proxy interface.
 * 
 * @author Felix Ableitner
 *
 */
public class RemotePlayService extends Service implements DeviceListenerCallback {

	private static final String TAG = "RemotePlayService";  

	// Start device discovery.
	public static final int MSG_OPEN = 1;
	// Stop device discovery.
	public static final int MSG_CLOSE = 2;
	// Select renderer.
	// param: string device_id
	public static final int MSG_SELECT = 3;
	// Unselect renderer.
	// param: int device_id
	public static final int MSG_UNSELECT = 4;
	// Set absolute volume.
	// param: int volume
	public static final int MSG_SET_VOPLUME = 5;
	// Change volume relative to current volume.
	// param: int delta
	public static final int MSG_CHANGE_VOLUME = 6;
	// Play from uri.
	// param: String uri
	public static final int MSG_PLAY = 7;
	// Pause playback.
	public static final int MSG_PAUSE = 8;
	// Stop playback.
	public static final int MSG_STOP = 9;
	// Seek to absolute time in ms.
	// param: long milliseconds
	public static final int MSG_SEEK = 10;
	
	/**
     * Handles incoming messages from clients.
     */
    private static class IncomingHandler extends Handler {
    	
        private final WeakReference<RemotePlayService> mService; 

        IncomingHandler(RemotePlayService service) {
            mService = new WeakReference<RemotePlayService>(service);
        }
        
        @Override
        public void handleMessage(Message msg) {
        	if (mService.get() != null) {
        		mService.get().handleMessage(msg);
        	}
        }
    }
    
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));
    
    private final UpnpPlayer mPlayer = new UpnpPlayer();
    
    private Messenger mListener;
    
    private HashMap<String, Device<?, ?, ?>> mDevices = new HashMap<String, Device<?, ?, ?>>();
	
	@Override
	public IBinder onBind(Intent itnent) {
		return mMessenger.getBinder();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
    	mPlayer.open(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mPlayer.getPlayService().getRenderer() != null) {
			mPlayer.getPlayService().stop();
		}
		mPlayer.close(this);
	}

	@Override
	public void deviceAdded(final Device<?, ?, ?> device) {
		if (device.getType().getType().equals("MediaRenderer") && 
				device instanceof RemoteDevice) {
        	mDevices.put(device.getIdentity().getUdn().toString(), device);
        	
        	

    		mPlayer.execute(
        			new GetVolume(UpnpController.getService(device, "RenderingControl")) {
    			
    			@SuppressWarnings("rawtypes")
    			@Override
    			public void failure(ActionInvocation invocation, 
    					UpnpResponse operation, String defaultMessage) {
    				Log.w(TAG, "Failed to get current Volume: " + defaultMessage);
    			}
    			
    			@SuppressWarnings("rawtypes")
    			@Override
    			public void received(ActionInvocation invocation, int currentVolume) {
    				int maxVolume = 100;
	            	if (UpnpPlayer.getService(device, "RenderingControl").getStateVariable("Volume") != null) {
	                	StateVariableAllowedValueRange volumeRange = 
	                			UpnpPlayer.getService(device, "RenderingControl").getStateVariable("Volume")
	                					.getTypeDetails().getAllowedValueRange();
	                	maxVolume = (int) volumeRange.getMaximum();
	                }
            	
    	        	Message msg = Message.obtain(null, Provider.MSG_RENDERER_ADDED, 0, 0);
    	        	msg.getData().putParcelable("device", new Provider.Device(
    	        			device.getIdentity().getUdn().toString(), 
    	        			device.getDisplayString(), 
    	        			device.getDetails().getManufacturerDetails().getManufacturer(), 
    	        			currentVolume, 
    	        			maxVolume));
    		        try {
    		            mListener.send(msg);
    		        } catch (RemoteException e) {
    		            e.printStackTrace();
    		        }
    			}
    		});	
		}
	}

	@Override
	public void deviceRemoved(Device<?, ?, ?> device) {
		if (device.getType().getType().equals("MediaRenderer") && 
				device instanceof RemoteDevice) {
			Message msg = Message.obtain(null, Provider.MSG_RENDERER_REMOVED, 0, 0);

			String udn = device.getIdentity().getUdn().toString();
	    	msg.getData().putString("id", udn);
	    	mDevices.remove(udn);	
	        try {
	            mListener.send(msg);
	        } catch (RemoteException e) {
	            e.printStackTrace();
	        }
		}
	}
	
	public void handleMessage(Message msg) {
    	Bundle data = msg.getData();
        switch (msg.what) {
        case MSG_OPEN:
    		mPlayer.getDeviceListener().addCallback(this);
        	mListener = msg.replyTo;
        	break;
        case MSG_CLOSE:
        	break;
        case MSG_SELECT:
        	mPlayer.selectRenderer(mDevices.get(data.getString("id")));
        	break;
        case MSG_UNSELECT:
        	mPlayer.getPlayService().stop();
        	break;
        case MSG_SET_VOPLUME:
        	mPlayer.setVolume(data.getInt("volume"));
        	break;
        case MSG_CHANGE_VOLUME:
        	mPlayer.changeVolume(data.getInt("delta"));
        	break;
        case MSG_PLAY:
    		mPlayer.getPlayService().setShowNotification(false);
        	Item item = new Item();
        	item.addResource(new Res());
        	item.getFirstResource().setValue(data.getString("uri"));
        	List<Item> playlist = new ArrayList<Item>();
        	playlist.add(item);
        	mPlayer.getPlayService().setPlaylist(playlist, 0);
        	break;
        case MSG_PAUSE:
        	mPlayer.getPlayService().pause();
        	break;
        case MSG_STOP:
        	mPlayer.getPlayService().stop();
        	break;
        case MSG_SEEK:
        	mPlayer.seek((int) data.getLong("milliseconds") / 1000);
        	break;
        }
    }

}

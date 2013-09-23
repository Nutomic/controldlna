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

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ServiceType;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Handles UPNP connection, including device discovery and general queries.
 * 
 * @author Felix Ableitner
 *
 */
public class UpnpController {
	
	private static final String TAG = "DlnaController";
	
	private DeviceListener mDeviceListener = new DeviceListener();
	
    protected AndroidUpnpService mUpnpService;

    private ServiceConnection mUpnpServiceConnection = new ServiceConnection() {

    	/**
    	 * Registers DeviceListener, adds known devices and starts search.
    	 */
		public void onServiceConnected(ComponentName className, IBinder service) {
            mUpnpService = (AndroidUpnpService) service;
            Log.i(TAG, "Starting device search");          
            mUpnpService.getRegistry().addListener(mDeviceListener);
            for (Device<?, ?, ?> d : mUpnpService.getControlPoint().getRegistry().getDevices()) {
            	if (d instanceof LocalDevice) {
            		mDeviceListener.localDeviceAdded(mUpnpService.getRegistry(), (LocalDevice) d);
            	}
            	else {
            		mDeviceListener.remoteDeviceAdded(mUpnpService.getRegistry(), (RemoteDevice) d);
            	}
            }
            mUpnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            mUpnpService = null;
        }
    };
    
    /**
     * Opens connection to the Cling UPNP service.
     * 
     * @param context Application context.
     */
    public void open(Context context) {
        context.bindService(
            new Intent(context, AndroidUpnpServiceImpl.class),
            mUpnpServiceConnection,
            Context.BIND_AUTO_CREATE
        );    	
    }
    
    /**
     * Closes the connection to the Cling UPNP service.
     * 
     * @param context Application context.
     */
    public void close(Context context) {
        mUpnpService.getRegistry().removeListener(mDeviceListener);
        context.unbindService(mUpnpServiceConnection);	
    }


    /**
     * Returns a device service by name for direct queries.
     */
	public Service<?, ?> getService(Device<?, ?, ?> device, String name) {
		return device.findService(
    			new ServiceType("schemas-upnp-org", name));
	}
	
	public void execute(ActionCallback callback) {
		mUpnpService.getControlPoint().execute(callback);
	}

	public void execute(SubscriptionCallback callback) {
		mUpnpService.getControlPoint().execute(callback);
	}
	
	public DeviceListener getDeviceListener() {
		return mDeviceListener;
	}
	
}

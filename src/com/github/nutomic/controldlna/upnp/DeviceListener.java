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

import java.util.ArrayList;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;

import android.util.Log;

/**
 * Provides an interface that informs about UPNP devices being added or removed.
 * 
 * @author Felix Ableitner
 *
 */
public class DeviceListener implements RegistryListener {
	
	private static final String TAG = "DeviceListener";
	
	/**
	 * Callbacks may be called from a background thread.
	 * 
	 * @author Felix Ableitner
	 *
	 */
	public interface DeviceListenerCallback {
		public void deviceAdded(Device<?, ?, ?> device);
		public void deviceRemoved(Device<?, ?, ?> device);
		public void deviceUpdated(Device<?, ?, ?> device);
	}
	
	private ArrayList<Device<?, ?, ?>> mDevices = new ArrayList<Device<?, ?, ?>>();
	
	private ArrayList<DeviceListenerCallback> mListeners = new ArrayList<DeviceListenerCallback>();
	
	public void addCallback(DeviceListenerCallback callback) {
		mListeners.add(callback);
		for (Device<?, ?, ?> d : mDevices) {
			callback.deviceAdded(d);
		}
	}
	
	public void removeCallback(DeviceListenerCallback callback) {
		mListeners.remove(callback);
	}
	
	private void deviceAdded(Device<?, ?, ?> device) {
		mDevices.add(device);
		for (DeviceListenerCallback l : mListeners) {
			l.deviceAdded(device);
		}
	}

	private void deviceRemoved(Device<?, ?, ?> device) {
		mDevices.remove(device);
		for (DeviceListenerCallback l : mListeners) {
			l.deviceRemoved(device);
		}
	}
	
	@Override
	public void afterShutdown() {
	}

	@Override
	public void beforeShutdown(Registry registry) {
	}

	@Override
	public void localDeviceAdded(Registry registry, LocalDevice device) {
		deviceAdded(device);
	}

	@Override
	public void localDeviceRemoved(Registry registry, LocalDevice device) {
		deviceRemoved(device);
	}

	@Override
	public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
		deviceAdded(device);
	}

	@Override
	public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device,
			Exception exception) {
		Log.w(TAG, "Remote device discovery failed", exception);
	}

	@Override
	public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
	}

	@Override
	public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
		deviceRemoved(device);
	}

	@Override
	public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
		for (DeviceListenerCallback l : mListeners) {
			l.deviceUpdated(device);
		}	
	}

}

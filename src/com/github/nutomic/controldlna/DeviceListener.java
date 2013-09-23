package com.github.nutomic.controldlna;

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
	}
	
	private ArrayList<Device<?, ?, ?>> mDevices = new ArrayList<Device<?, ?, ?>>();
	
	private ArrayList<DeviceListenerCallback> mListeners = new ArrayList<DeviceListenerCallback>();
	
	public void addCallback(DeviceListenerCallback callback) {
		mListeners.add(callback);
		for (Device<?, ?, ?> d : mDevices) 
			callback.deviceAdded(d);
		
	}
	
	public void removeCallback(DeviceListenerCallback callback) {
		mListeners.remove(callback);
	}
	
	private void deviceAdded(Device<?, ?, ?> device) {
		mDevices.add(device);
		for (DeviceListenerCallback l : mListeners)
			l.deviceAdded(device);
	}

	private void deviceRemoved(Device<?, ?, ?> device) {
		mDevices.remove(device);
		for (DeviceListenerCallback l : mListeners)
			l.deviceRemoved(device);
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
		deviceRemoved(device);
		deviceAdded(device);
		
	}

}

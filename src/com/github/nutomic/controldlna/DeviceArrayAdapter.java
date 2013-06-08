package com.github.nutomic.controldlna;

import java.util.Collection;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Displays the devices that are inserted through the RegistryListener (either 
 * of type RENDERER or SERVER).
 * 
 * @author Felix
 *
 */
public class DeviceArrayAdapter extends ArrayAdapter<Device<?, ?, ?>> 
		implements RegistryListener {
	
	private static final String TAG = "DeviceArrayAdapter";

	public static final String RENDERER = "MediaRenderer";
	
	public static final String SERVER = "MediaServer";
	
	private Activity mActivity;
	
	private String mDeviceType;
		
	/**
	 * @param deviceType One of RENDERER or SERVER.
	 */
	public DeviceArrayAdapter(Activity activity, String deviceType) {
		super(activity, android.R.layout.simple_list_item_2);
		mActivity = activity;
		mDeviceType = deviceType;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
	        LayoutInflater inflater = (LayoutInflater) getContext()
	                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
		}
        TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
        tv.setText(getItem(position).getDisplayString());
        return convertView;
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
	public void remoteDeviceDiscoveryFailed(Registry registry, 
			RemoteDevice device, Exception exception) {
		Log.w(TAG, "Device discovery failed", exception);
	}

	@Override
	public void remoteDeviceDiscoveryStarted(Registry registry, 
			RemoteDevice device) {
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
	
	private void deviceAdded(final Device<?, ?, ?> device) {
		mActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if (device.getType().getType().equals(mDeviceType))
					add(device);	
			}
		});
	}

	private void deviceRemoved(final Device<?, ?, ?> device) {
		mActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				remove(device);	
			}
		});			
	}

	/**
	 * Not implemented on lower API levels.
	 */
	@Override
	public void addAll(Collection<? extends Device<?, ?, ?>> collection) {
		for (Device<?, ?, ?> d : collection)
			add(d);
	}
}

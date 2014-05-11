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

package com.github.nutomic.controldlna.utility;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.nutomic.controldlna.R;

import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;

/**
 * Displays the devices that are inserted through the RegistryListener (either
 * of type RENDERER or SERVER).
 *
 * @author Felix Ableitner
 *
 */
public class DeviceArrayAdapter extends ArrayAdapter<Device<?, ?, ?>>
		implements RegistryListener {

	private static final String TAG = "DeviceArrayAdapter";

	public static final String SERVER = "MediaServer";

	private Activity mActivity;

	private String mDeviceType;

	/**
	 * @param deviceType One of RENDERER or SERVER.
	 */
	public DeviceArrayAdapter(Activity activity, String deviceType) {
		super(activity, R.layout.list_item);
		mActivity = activity;
		mDeviceType = deviceType;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.list_item, parent, false);
		}
		TextView title = (TextView) convertView.findViewById(R.id.title);
		TextView subtitle = (TextView) convertView.findViewById(R.id.subtitle);
		title.setText(getItem(position).getDetails().getFriendlyName());
		subtitle.setText(getItem(position).getDisplayString());

		if (getItem(position).hasIcons()) {
			URI uri = getItem(position).getIcons()[0].getUri();
			if (getItem(position) instanceof RemoteDevice) {
				try {
					RemoteDevice device = (RemoteDevice) getItem(position);
					uri = device.normalizeURI(uri).toURI();
				}
				catch (URISyntaxException e) {
					Log.w(TAG, "Failed to get device icon URI", e);
				}
			}
			RemoteImageView icon = (RemoteImageView) convertView.findViewById(R.id.image);
			icon.setImageUri(uri);
		}

		return convertView;
	}

	/**
	 * Adds a new device to the list if its type equals mDeviceType.
	 */
	public void deviceAdded(final Device<?, ?, ?> device) {
		for (int i = 0; i < getCount(); i++) {
			if (getItem(i).equals(device)) {
				return;
			}
		}

		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (device.getType().getType().equals(mDeviceType)) {
					add(device);
					sort(new Comparator<Device<?, ?, ?>>() {

						@Override
						public int compare(Device<?, ?, ?> lhs,
								Device<?, ?, ?> rhs) {
							return lhs.getDetails().getFriendlyName()
									.compareTo(rhs.getDetails().getFriendlyName());
						}
					});
				}
			}
		});
	}

	/**
	 * Removes the device from the list (if it is an element).
	 */
	public void deviceRemoved(final Device<?, ?, ?> device) {
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (getPosition(device) != -1) {
					remove(device);
				}
			}
		});
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
	}

	@Override
	public void afterShutdown() {
	}

}

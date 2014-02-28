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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteDescriptor;
import android.support.v7.media.MediaRouteDiscoveryRequest;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderDescriptor.Builder;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.ControlRequestCallback;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Toast;

/**
 * Allows playing to a DLNA renderer from a remote app.
 *
 * @author Felix Ableitner
 */
final class Provider extends MediaRouteProvider {

	// Device has been added.
	// param: Device device
	public static final int MSG_RENDERER_ADDED = 1;

	// Device has been removed.
	// param: int id
	public static final int MSG_RENDERER_REMOVED = 2;

	// Playback status information, retrieved after RemotePlayService.MSG_GET_STATUS.
	// param: bundle media_item_status
	// param: int hash
	public static final int MSG_STATUS_INFO = 3;

	// Indicates an error in communication between RemotePlayService and renderer.
	// param: String error
	public static final int MSG_ERROR = 4;

	/**
	 * Allows passing and storing basic information about a device.
	 */
	static public class Device implements Parcelable {

		public String id;
		public String name;
		public String description;
		public int volume;
		public int volumeMax;

		public static final Parcelable.Creator<Device> CREATOR
		= new Parcelable.Creator<Device>() {
			public Device createFromParcel(Parcel in) {
				return new Device(in);
			}

			public Device[] newArray(int size) {
				return new Device[size];
			}
		};

		private Device(Parcel in) {
			id = in.readString();
			name = in.readString();
			description = in.readString();
			volume = in.readInt();
			volumeMax = in.readInt();
		}

		public Device(String id, String name, String description, int volume, int volumeMax) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.volume = volume;
			this.volumeMax = volumeMax;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(id);
			dest.writeString(name);
			dest.writeString(description);
			dest.writeInt(volume);
			dest.writeInt(volumeMax);
		}

	}

	private HashMap<String, Device> mDevices = new HashMap<String, Device>();

	private SparseArray<Pair<Intent, ControlRequestCallback>> mRequests =
			new SparseArray<Pair<Intent, ControlRequestCallback>>();

	IRemotePlayService mIRemotePlayService;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mIRemotePlayService = IRemotePlayService.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName className) {
			mIRemotePlayService = null;
		}
	};

	private static final ArrayList<IntentFilter> CONTROL_FILTERS;
	static {
		IntentFilter f = new IntentFilter();
		f.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		f.addAction(MediaControlIntent.ACTION_PLAY);
		f.addAction(MediaControlIntent.ACTION_PAUSE);
		f.addAction(MediaControlIntent.ACTION_SEEK);
		f.addAction(MediaControlIntent.ACTION_STOP);
		f.addDataScheme("http");
		f.addDataScheme("https");
		try {
			f.addDataType("video/*");
			f.addDataType("audio/*");
		} catch (MalformedMimeTypeException ex) {
			throw new RuntimeException(ex);
		}

		CONTROL_FILTERS = new ArrayList<IntentFilter>();
		CONTROL_FILTERS.add(f);
	}

	/**
	 * Listens for messages about devices.
	 */
	static private class DeviceListener extends Handler {

		private final WeakReference<Provider> mService;

		DeviceListener(Provider provider) {
			mService = new WeakReference<Provider>(provider);
		}

		@Override
		public void handleMessage(Message msg) {
			if (mService.get() != null)
				mService.get().handleMessage(msg);
		}
	}

	final Messenger mListener = new Messenger(new DeviceListener(this));

	public Provider(Context context) {
		super(context);
		context.bindService(
				new Intent(context, RemotePlayService.class),
				mConnection,
				Context.BIND_AUTO_CREATE
				);
	}

	public void close() {
		getContext().unbindService(mConnection);
	}

	@Override
	public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
		try {
			if (request != null && request.isActiveScan())
				mIRemotePlayService.startSearch(mListener);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public RouteController onCreateRouteController(String routeId) {
		return new RouteController(routeId);
	}

	private void updateRoutes() {
		Builder builder = new Builder();
		for (Entry<String, Device> d : mDevices.entrySet()) {
			MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
							d.getValue().id, d.getValue().name)
					.setDescription(d.getValue().description)
					.addControlFilters(CONTROL_FILTERS)
					.setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
					.setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
					.setVolumeMax(d.getValue().volumeMax)
					.setVolume(d.getValue().volume)
					.build();
			builder.addRoute(routeDescriptor);
		}
		setDescriptor(builder.build());
	}

	/**
	 * Receives and forwards device selections, volume change
	 * requests and control requests.
	 */
	private final class RouteController extends MediaRouteProvider.RouteController {

		private final String mRouteId;

		public RouteController(String routeId) {
			mRouteId = routeId;
		}

		@Override
		public void onRelease() {
		}

		@Override
		public void onSelect() {
			try {
				mIRemotePlayService.selectRenderer(mRouteId);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onUnselect() {
			try {
				mIRemotePlayService.unselectRenderer(mRouteId);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onSetVolume(int volume) {
			if (volume < 0 || volume > mDevices.get(mRouteId).volumeMax)
				return;

			try {
				mIRemotePlayService.setVolume(volume);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			mDevices.get(mRouteId).volume = volume;
			updateRoutes();
		}

		@Override
		public void onUpdateVolume(int delta) {
			onSetVolume(mDevices.get(mRouteId).volume + delta);
		}

		/**
		 * Handles play, pause, resume, stop, seek and get_status requests for this route.
		 */
		@Override
		public boolean onControlRequest(Intent intent, ControlRequestCallback callback) {
			try {
				if (intent.getAction().equals(MediaControlIntent.ACTION_PLAY)) {
					String metadata = (intent.hasExtra(MediaControlIntent.EXTRA_ITEM_METADATA))
							? intent.getExtras().getString(MediaControlIntent.EXTRA_ITEM_METADATA)
									: null;
							mIRemotePlayService.play(intent.getDataString(), metadata);
							// Store in intent extras for later.
							intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mRouteId);
							intent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, intent.getDataString());
							getItemStatus(intent, callback);
							return true;
				}
				else if (intent.getAction().equals(MediaControlIntent.ACTION_PAUSE)) {
					mIRemotePlayService.pause(mRouteId);
					return true;
				}
				else if (intent.getAction().equals(MediaControlIntent.ACTION_RESUME)) {
					mIRemotePlayService.resume(mRouteId);
					return true;
				}
				else if (intent.getAction().equals(MediaControlIntent.ACTION_STOP)) {
					mIRemotePlayService.stop(mRouteId);
					return true;
				}
				else if (intent.getAction().equals(MediaControlIntent.ACTION_SEEK)) {
					mIRemotePlayService.seek(mRouteId,
							intent.getStringExtra(
									MediaControlIntent.EXTRA_ITEM_ID),
									intent.getLongExtra(
											MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0));
					getItemStatus(intent, callback);
					return true;
				}
				else if(intent.getAction().equals(MediaControlIntent.ACTION_GET_STATUS)) {
					getItemStatus(intent, callback);
					return true;
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return false;
		}

	}

	/**
	 * Requests status info via RemotePlayService, stores intent and callback to
	 * access later in handleMessage.
	 */
	private void getItemStatus(Intent intent, ControlRequestCallback callback)
			throws RemoteException {
		if (callback == null)
			return;

		Pair<Intent, ControlRequestCallback> pair =
				new Pair<Intent, ControlRequestCallback>(intent, callback);
		int r = new Random().nextInt();
		mRequests.put(r, pair);
		mIRemotePlayService.getItemStatus(
				intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID),
				intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID),
				r);
	}

	/**
	 * Handles device add and remove as well as sending status info requested earlier.
	 */
	public void handleMessage(Message msg) {
		Bundle data = msg.getData();
		switch (msg.what) {
		case MSG_RENDERER_ADDED:
			msg.getData().setClassLoader(Device.class.getClassLoader());
			Device device = (Device) data.getParcelable("device");
			mDevices.put(device.id, device);
			updateRoutes();
			break;
		case MSG_RENDERER_REMOVED:
			mDevices.remove(data.getString("id"));
			updateRoutes();
			break;
		case MSG_STATUS_INFO:
			Pair<Intent, ControlRequestCallback> pair =
			mRequests.get(data.getInt("hash"));
			Bundle status = data.getBundle("media_item_status");

			if (pair.first.hasExtra(MediaControlIntent.EXTRA_SESSION_ID))
				status.putString(MediaControlIntent.EXTRA_SESSION_ID,
						pair.first.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID));
			if (pair.first.hasExtra(MediaControlIntent.EXTRA_ITEM_ID))
				status.putString(MediaControlIntent.EXTRA_ITEM_ID,
						pair.first.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID));
			pair.second.onResult(status);
			break;
		case MSG_ERROR:
			Toast.makeText(getContext(), data.getString("error"), Toast.LENGTH_SHORT).show();
			break;
		}

	}

}
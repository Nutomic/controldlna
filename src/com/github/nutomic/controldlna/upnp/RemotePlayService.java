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

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.StateVariableAllowedValueRange;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;
import org.teleal.cling.support.avtransport.callback.GetPositionInfo;
import org.teleal.cling.support.avtransport.callback.Pause;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.Seek;
import org.teleal.cling.support.avtransport.callback.SetAVTransportURI;
import org.teleal.cling.support.avtransport.callback.Stop;
import org.teleal.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.lastchange.LastChange;
import org.teleal.cling.support.model.PositionInfo;
import org.teleal.cling.support.model.SeekMode;
import org.teleal.cling.support.renderingcontrol.callback.GetVolume;
import org.teleal.cling.support.renderingcontrol.callback.SetVolume;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaItemStatus.Builder;
import android.util.Log;


/**
 * Allows UPNP playback from within different apps by providing a proxy interface.
 * 
 * @author Felix Ableitner
 *
 */
public class RemotePlayService extends Service implements RegistryListener {

	private static final String TAG = "RemotePlayService";
    
    private Messenger mListener;
    
    private ConcurrentHashMap<String, Device<?, ?, ?>> mDevices = 
    		new ConcurrentHashMap<String, Device<?, ?, ?>>();
    
    private Device<?, ?, ?> mCurrentRenderer;
    
    private int mPlaybackState;
    
    private boolean mManuallyStopped;
	
    protected AndroidUpnpService mUpnpService;

    private ServiceConnection mUpnpServiceConnection = new ServiceConnection() {

    	/**
    	 * Registers DeviceListener, adds known devices and starts search if requested.
    	 */
		public void onServiceConnected(ComponentName className, IBinder service) {
            mUpnpService = (AndroidUpnpService) service;
            mUpnpService.getRegistry().addListener(RemotePlayService.this);
            for (Device<?, ?, ?> d : mUpnpService.getControlPoint().getRegistry().getDevices()) {
            	if (d instanceof LocalDevice)
            		localDeviceAdded(mUpnpService.getRegistry(), (LocalDevice) d);
            	else
            		remoteDeviceAdded(mUpnpService.getRegistry(), (RemoteDevice) d);
            }
        	mUpnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            mUpnpService = null;
        }
    };

    /**
     * Receives events from current renderer.
     */
	private SubscriptionCallback mSubscriptionCallback;
	
	private final IRemotePlayService.Stub mBinder = new IRemotePlayService.Stub() {

		@Override
		public void startSearch(Messenger listener)
				throws RemoteException {
	    	mListener = listener;
		}

		@Override
		public void selectRenderer(String id) throws RemoteException {
			mCurrentRenderer = mDevices.get(id);
			mSubscriptionCallback = new SubscriptionCallback(
					mCurrentRenderer.findService(
			    			new ServiceType("schemas-upnp-org", "AVTransport")), 600) {
	
				@SuppressWarnings("rawtypes")
				@Override
				protected void established(GENASubscription sub) {
				}
	
				@SuppressWarnings("rawtypes")
				@Override
				protected void ended(GENASubscription sub, CancelReason reason,
						UpnpResponse response) {			
				}
	
				@SuppressWarnings("rawtypes")
				@Override
				protected void eventReceived(final GENASubscription sub) {				
					@SuppressWarnings("unchecked")
					Map<String, StateVariableValue> m = sub.getCurrentValues();
					try {
						LastChange lastChange = new LastChange(
								new AVTransportLastChangeParser(), 
								m.get("LastChange").toString());
						switch (lastChange.getEventedValue(0, 
								AVTransportVariable.TransportState.class)
										.getValue()) {
						case PLAYING:
							mPlaybackState = MediaItemStatus.PLAYBACK_STATE_PLAYING;
					    	break;
						case PAUSED_PLAYBACK:
							mPlaybackState = MediaItemStatus.PLAYBACK_STATE_PAUSED;
					    	break;
						case STOPPED:
							if (mManuallyStopped) {
								mManuallyStopped = false;
								mPlaybackState = MediaItemStatus.PLAYBACK_STATE_CANCELED;
							}
							else
								mPlaybackState = MediaItemStatus.PLAYBACK_STATE_FINISHED;							
							break;
						case TRANSITIONING:
							mPlaybackState = MediaItemStatus.PLAYBACK_STATE_PENDING;
							break;
						case NO_MEDIA_PRESENT:
							mPlaybackState = MediaItemStatus.PLAYBACK_STATE_ERROR;
							break;
						default:
					    }
						
					} catch (Exception e) {
						Log.w(TAG, "Failed to parse UPNP event", e);
					}	
				}
	
				@SuppressWarnings("rawtypes")
				@Override
				protected void eventsMissed(GENASubscription sub, 
						int numberOfMissedEvents) {	
				}
	
				@SuppressWarnings("rawtypes")
				@Override
				protected void failed(GENASubscription sub, UpnpResponse responseStatus,
						Exception exception, String defaultMsg) {	
				}
			};
			mUpnpService.getControlPoint().execute(mSubscriptionCallback);	
		}

		/**
		 * Ends selection, stops playback if possible.
		 */
		@Override
		public void unselectRenderer(String sessionId) throws RemoteException {
			if (mDevices.get(sessionId) != null)
				stop(sessionId);
	    	mSubscriptionCallback.end();
	    	mCurrentRenderer = null;					
		}

		/**
	     * Sets an absolute volume. The value is assumed to be within the valid 
	     * volume range.
	     */
		@Override
		public void setVolume(int volume) throws RemoteException {
			mUpnpService.getControlPoint().execute(
					new SetVolume(getService(mCurrentRenderer, 
							"RenderingControl"), volume) {
				
				@SuppressWarnings("rawtypes")
				@Override
				public void failure(ActionInvocation invocation, 
						UpnpResponse operation, String defaultMessage) {
					Log.w(TAG, "Failed to set new Volume: " + defaultMessage);
				}
			});
		}

		/**
		 * Sets playback source and metadata, then starts playing on 
		 * current renderer.
		 */
		@Override
		public void play(String uri, String metadata) throws RemoteException {
			mPlaybackState = MediaItemStatus.PLAYBACK_STATE_BUFFERING;
			mUpnpService.getControlPoint().execute(new SetAVTransportURI(
					getService(mCurrentRenderer, "AVTransport"), 
	    			uri, metadata) {
				@SuppressWarnings("rawtypes")
				@Override
	            public void failure(ActionInvocation invocation, 
	            		UpnpResponse operation, String defaultMsg) {
	                Log.w(TAG, "Playback failed: " + defaultMsg);
	            }
	            
				@SuppressWarnings("rawtypes")
				@Override
	    		public void success(ActionInvocation invocation) {
					mUpnpService.getControlPoint().execute(
							new Play(getService(mCurrentRenderer, 
									"AVTransport")) {
						
						@Override
						public void success(ActionInvocation invocation) {
							mPlaybackState = MediaItemStatus.PLAYBACK_STATE_PLAYING;
						}
						
						@Override
						public void failure(ActionInvocation invocation, 
								UpnpResponse operation, String defaultMessage) {
							Log.w(TAG, "Play failed: " + defaultMessage);
						}
					});
				}
	        });
		}
		
		/**
		 * Pauses playback on current renderer.
		 */
		@Override
		public void pause(final String sessionId) throws RemoteException {
			mUpnpService.getControlPoint().execute(
					new Pause(getService(mDevices.get(sessionId), "AVTransport")) {
				
				@SuppressWarnings("rawtypes")
				@Override
				public void failure(ActionInvocation invocation, 
						UpnpResponse operation, String defaultMessage) {
					Log.w(TAG, "Pause failed, trying stop: " + defaultMessage);
					// Sometimes stop works even though pause does not.
					try {
						stop(sessionId);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			});			
		}

		@Override
		public void resume(String sessionId) throws RemoteException {
			mUpnpService.getControlPoint().execute(
					new Play(getService(mDevices.get(sessionId), 
							"AVTransport")) {
						
				@Override
				@SuppressWarnings("rawtypes") 
				public void failure(ActionInvocation invocation, 
						UpnpResponse operation, String defaultMessage) {
					Log.w(TAG, "Play failed: " + defaultMessage);
				}
			});
		}

		/**
		 * Stops playback on current renderer.
		 */
		@Override
		public void stop(String sessionId) throws RemoteException {
			mManuallyStopped = true;
			mUpnpService.getControlPoint().execute(
				new Stop(getService(mDevices.get(sessionId), "AVTransport")) {
				
				@SuppressWarnings("rawtypes")
				@Override
				public void failure(ActionInvocation invocation,
						org.teleal.cling.model.message.UpnpResponse operation,
						String defaultMessage) {
					Log.w(TAG, "Stop failed: " + defaultMessage);				
				}
			});
		}
	    
	    /**
	     * Seeks to the given absolute time in seconds.
	     */
		@Override
		public void seek(String sessionId, String itemId, long milliseconds) 
				throws RemoteException {
			mUpnpService.getControlPoint().execute(new Seek(
	    			getService(mDevices.get(sessionId), "AVTransport"), 
	    			SeekMode.REL_TIME, 
	    			Integer.toString((int) milliseconds / 1000)) {
				
				@SuppressWarnings("rawtypes")
				@Override
				public void failure(ActionInvocation invocation, 
						UpnpResponse operation, String defaultMessage) {
					Log.w(TAG, "Seek failed: " + defaultMessage);
				}
			});			    
		}

		/**
		 * Sends a message with current status for the route and item.
		 * 
		 * If itemId does not match with the item currently played, 
		 * MediaItemStatus.PLAYBACK_STATE_INVALIDATED is returned.
		 * 
		 * @param sessionId Identifier of the session (equivalent to route) to get info for.
		 * @param itemId Identifier of the item to get info for.
		 * @param requestHash Passed back in message to find original request object.
		 */
		@Override
		public void getItemStatus(String sessionId, final String itemId, final int requestHash) 
				throws RemoteException {
			mUpnpService.getControlPoint().execute(new GetPositionInfo(
						getService(mDevices.get(sessionId), "AVTransport")) {
	
					@SuppressWarnings("rawtypes")
					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMessage) {
						Log.w(TAG, "Get position failed: " + defaultMessage);	
					}
	
					@SuppressWarnings("rawtypes")
					@Override
					public void received(ActionInvocation invocation, PositionInfo positionInfo) {
						if (positionInfo.getTrackURI() == null)
							return;
						
						Message msg = Message.obtain(null, Provider.MSG_STATUS_INFO, 0, 0);
						Builder status = null;
						
						if (positionInfo.getTrackURI().equals(itemId)) {
							status = new MediaItemStatus.Builder(mPlaybackState)
									.setContentPosition(positionInfo.getTrackElapsedSeconds() * 1000)
									.setContentDuration(positionInfo.getTrackDurationSeconds() * 1000)
									.setTimestamp(positionInfo.getAbsCount());
						}
						else {
							status = new MediaItemStatus.Builder(
									MediaItemStatus.PLAYBACK_STATE_INVALIDATED);
						}
						
				    	msg.getData().putBundle("media_item_status", status.build().asBundle());
				    	msg.getData().putInt("hash", requestHash);
				    	
				        try {
				            mListener.send(msg);
				        } catch (RemoteException e) {
				            e.printStackTrace();
				        }
					}
			});
		}
	};
	
	@Override
	public IBinder onBind(Intent itnent) {
		return mBinder;
	}
	
	/**
	 * Binds to cling service, registers wifi state change listener.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
        bindService(
            new Intent(this, AndroidUpnpServiceImpl.class),
            mUpnpServiceConnection,
            Context.BIND_AUTO_CREATE
        );
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mWifiReceiver, filter);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
        unbindService(mUpnpServiceConnection);
        unregisterReceiver(mWifiReceiver);
	}
	
	/**
	 * Starts device search on wifi connect, removes unreachable 
	 * devices on wifi disconnect.
	 */
	private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {

	    @Override
	    public void onReceive(Context context, Intent intent) {     
	        ConnectivityManager connManager = (ConnectivityManager) 
	        		getSystemService(CONNECTIVITY_SERVICE);
	        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

	        if (wifi.isConnected()) {
	        	if (mUpnpService != null) {
	                for (Device<?, ?, ?> d : mUpnpService.getControlPoint().getRegistry().getDevices())
	                	deviceAdded(d);
		        	mUpnpService.getControlPoint().search();
	        	}
	        }
	        else {
	        	for (Entry<String, Device<?, ?, ?>> d : mDevices.entrySet()) {
	            	if (mUpnpService.getControlPoint().getRegistry()
	            			.getDevice(new UDN(d.getKey()), false) == null) {
	            		deviceRemoved(d.getValue());
	            		if (d.getValue().equals(mCurrentRenderer))
	            			mCurrentRenderer = null;
	            	}	        		
	        	}
	        }
	    }   
	};

    /**
     * Returns a device service by name for direct queries.
     */
	private org.teleal.cling.model.meta.Service<?, ?> getService(
			Device<?, ?, ?> device, String name) {
		return device.findService(
    			new ServiceType("schemas-upnp-org", name));
	}

	/**
	 * Gather device data and send it to Provider.
	 */
	private void deviceAdded(final Device<?, ?, ?> device) {
		if (mDevices.containsValue(device))
			return;
		
		final org.teleal.cling.model.meta.Service<?, ?> rc = 
				getService(device, "RenderingControl");
		if (rc == null || mListener == null)
			return;
		
		if (device.getType().getType().equals("MediaRenderer") && 
				device instanceof RemoteDevice) {
        	mDevices.put(device.getIdentity().getUdn().toString(), device);

        	try {
        		mUpnpService.getControlPoint().execute(new GetVolume(rc) {
    			
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
		            	if (rc.getStateVariable("Volume") != null) {
		                	StateVariableAllowedValueRange volumeRange = 
		                			rc.getStateVariable("Volume").getTypeDetails().getAllowedValueRange();
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
        	catch (IllegalArgumentException e) {
        		e.printStackTrace();
        		return;
        	}	
		}
	}

	/**
	 * Remove the device from Provider.
	 */
	private void deviceRemoved(Device<?, ?, ?> device) {
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

	/**
	 * If a device was updated, we just add it again (devices are stored in 
	 * maps, so adding the same one again just overwrites the old one).
	 */
	private void deviceUpdated(Device<?, ?, ?> device) {
		deviceAdded(device);
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
		deviceUpdated(device);
	}
}

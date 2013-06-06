package com.github.nutomic.controldlna;

import java.util.Map;

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
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.SetAVTransportURI;
import org.teleal.cling.support.avtransport.callback.Stop;
import org.teleal.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.lastchange.LastChange;
import org.teleal.cling.support.model.item.Item;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.github.nutomic.controldlna.MainActivity.OnBackPressedListener;

/**
 * Shows a list of media servers, allowing to select one for playback.
 * 
 * @author Felix
 *
 */
public class RendererFragment extends Fragment implements 
		OnBackPressedListener, OnItemClickListener, OnClickListener {
	
	private final String TAG = "RendererFragment";
	
	private ListView mListView;
	
	private Button mPlayPause;
	
	private boolean mPlaying = false;
	
	private Item[] mPlaylist;
	
	/**
	 * ListView adapter of media renderers.
	 */
	private DeviceArrayAdapter mRendererAdapter;
	
	private FileArrayAdapter mPlaylistAdapter;
	
	/**
	 * The media renderer that is currently active.
	 */
	private Device<?, ?, ?> mCurrentRenderer;
	
	/**
	 * First track to be played when a renderer is selected (-1 for none).
	 */
	private int mCachedStart = -1;
	
	private SubscriptionCallback mSubscriptionCallback;
	
	/**
	 * Cling UPNP service. 
	 */
    private AndroidUpnpService mUpnpService;

    /**
     * Connection Cling to UPNP service.
     */
    private ServiceConnection mServiceConnection= new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            mUpnpService = (AndroidUpnpService) service;
            Log.i(TAG, "Starting device search");
            mUpnpService.getRegistry().addListener(mRegistryListener);
            mUpnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            mUpnpService = null;
        }
    };

    /**
     * Receives updates when devices are added or removed.
     */
    private RegistryListener mRegistryListener = new RegistryListener() {
		
		@Override
		public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
		}
		
		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			remove(device);	
		}
		
		@Override
		public void remoteDeviceDiscoveryStarted(Registry registry, 
				RemoteDevice device) {
		}
		
		@Override
		public void remoteDeviceDiscoveryFailed(Registry registry, 
				RemoteDevice device, Exception exception) {
			Log.w(TAG, "Device discovery failed" + exception.getMessage());
		}
		
		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			add(device);
		}
		
		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {
			remove(device);	
		}
		
		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
			add(device);
		}
		
		@Override
		public void beforeShutdown(Registry registry) {			
		}
		
		@Override
		public void afterShutdown() {
		}
		
		/**
		 * Add a device to the ListView.
		 */
		private void add(final Device<?, ?, ?> device) {
			getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					if (device.getType().getType().equals("MediaRenderer"))
						mRendererAdapter.add(device);	
				}
			});			
		}
		
		/**
		 * Remove a device from the ListView.
		 */
		private void remove(final Device<?, ?, ?> device) {
			getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					mRendererAdapter.remove(device);	
				}
			});			
		}
	};
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
        return inflater.inflate(R.layout.renderer_fragment, null);
	};
	
	/**
	 * Initializes ListView adapters, launches Cling UPNP service.
	 */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	mRendererAdapter = new DeviceArrayAdapter(getActivity());
    	mListView = (ListView) getView().findViewById(R.id.listview);
        mListView.setAdapter(mRendererAdapter);
        mListView.setOnItemClickListener(this);
        mPlayPause = (Button) getView().findViewById(R.id.playpause);
        mPlayPause.setOnClickListener(this);
    	mPlayPause.setText(R.string.play);

        getActivity().getApplicationContext().bindService(
            new Intent(getActivity(), AndroidUpnpServiceImpl.class),
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        );     
    }
    
    /**
     * Clears cached playback URI.
     */
    @Override
    public void onPause() {
    	super.onPause();    	
    	mCachedStart = -1;
    }

    /**
     * Closes Cling UPNP service.
     */
    @Override
	public void onDestroy() {
        super.onDestroy();
        if (mUpnpService != null)
            mUpnpService.getRegistry().removeListener(mRegistryListener);
        getActivity().getApplicationContext().unbindService(mServiceConnection);
    }
	
    /**
     * Plays the URIs in playlist to the current renderer, or caches parameters
     * until a renderer is selected.
     * 
     * @param playlist Array of URIs to play.
     * @param start Index of the URI which should be played first.
     */
	public void play(Item[] playlist, final int start) {
		mPlaylist = playlist;
		if (mCurrentRenderer != null) {
			mListView.setAdapter(mPlaylistAdapter);
	    	final Service<?, ?> service = mCurrentRenderer.findService(
	    			new ServiceType("schemas-upnp-org", "AVTransport"));
	    	mUpnpService.getControlPoint().execute(new SetAVTransportURI(service, 
	    			playlist[start].getFirstResource().getValue(), "NO METADATA") {
				@SuppressWarnings("rawtypes")
				@Override
	            public void failure(ActionInvocation invocation, 
	            		UpnpResponse operation, String defaultMsg) {
	                Log.w(TAG, "Playback failed: " + defaultMsg);
	            }
	            
				@SuppressWarnings("rawtypes")
				@Override
        		public void success(ActionInvocation invocation) {
	    	    	mUpnpService.getControlPoint().execute(new Play(service) {
	    	    		
	    	            @Override
	    	            public void failure(ActionInvocation invocation, 
	    	            		UpnpResponse operation, String defaultMsg) {
	    	                Log.w(TAG, "Playback failed: " + defaultMsg);
	    	            }
	    	        });
        		}
	        });
		}
		else {
			Toast.makeText(getActivity(), "Please select a renderer.", 
					Toast.LENGTH_SHORT).show();
			mCachedStart = start;
		}
	}
	
	/**
	 * Selects a media renderer.
	 */
	@Override
	public void onItemClick(AdapterView<?> a, View v, int position, long id) {
		if (mCurrentRenderer == null) {
			mCurrentRenderer = mRendererAdapter.getItem(position);
	    	Service<?, ?> service = mCurrentRenderer.findService(
	    			new ServiceType("schemas-upnp-org", "AVTransport"));	
	    	mSubscriptionCallback = new SubscriptionCallback(service, 600) {

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
    			protected void eventReceived(GENASubscription sub) {
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
							getActivity().runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
							    	mPlayPause.setText(R.string.pause);
									mPlaying = true;	
							    	Log.d(TAG, "play");
								}
							});
					    	break;
						case PAUSED_PLAYBACK:
							// fallthrough
						case STOPPED:
							getActivity().runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
							    	mPlayPause.setText(R.string.play);
									mPlaying = false;	
							    	Log.d(TAG, "stop");								
								}
							});
					    	break;
					    default:
					    }
						
					} catch (Exception e) {
						e.printStackTrace();
					}
    			}

    			@SuppressWarnings("rawtypes")
    			@Override
    			protected void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {		
    			}

    			@SuppressWarnings("rawtypes")
    			@Override
    			protected void failed(GENASubscription sub, UpnpResponse responseStatus,
    					Exception exception, String defaultMsg) {	
    				Log.d(TAG, defaultMsg);
    			}
			};
	    	mUpnpService.getControlPoint().execute(mSubscriptionCallback);
			if (mCachedStart != -1) {
				play(mPlaylist, mCachedStart);
				mCachedStart = -1;
			}

			mListView.setAdapter(mPlaylistAdapter);
			mPlayPause.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Unselects current media renderer if one is selected.
	 */
	@Override
	public boolean onBackPressed() {
		if (mCurrentRenderer != null) {
	        mCurrentRenderer = null;
			mSubscriptionCallback.end();
			
	        mListView.setAdapter(mRendererAdapter);  
	        mPlayPause.setVisibility(View.GONE);	        
	        return true;
		}
		return false;
	}

	/**
	 * Plays/pauses playback on button click.
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.playpause:
	    	final Service<?, ?> service = mCurrentRenderer.findService(
	    			new ServiceType("schemas-upnp-org", "AVTransport"));
			if (mPlaying) {
				mUpnpService.getControlPoint().execute(new Stop(service) {
					
					@SuppressWarnings("rawtypes")
					@Override
					public void failure(ActionInvocation invocation, 
							UpnpResponse operation, String defaultMessage) {
						Log.w(TAG, "Pause failed, trying stop: " + defaultMessage);
						// Sometimes stop works even though pause does not.
						mUpnpService.getControlPoint().execute(new Stop(service) {
							
							@Override
							public void failure(ActionInvocation invocation, 
									UpnpResponse operation, String defaultMessage) {
								Log.w(TAG, "Stop failed: " + defaultMessage);
							}
						});		
					}
				});			
			} else {
				mUpnpService.getControlPoint().execute(new Play(service) {
					
					@SuppressWarnings("rawtypes")
					@Override
					public void failure(ActionInvocation invocation, 
							UpnpResponse operation, String defaultMessage) {
						Log.w(TAG, "Play failed: " + defaultMessage);
					}
				});
			}
		}		
	}

}

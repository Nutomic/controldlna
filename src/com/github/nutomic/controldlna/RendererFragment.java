package com.github.nutomic.controldlna;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.SetAVTransportURI;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.github.nutomic.controldlna.MainActivity.OnBackPressedListener;

/**
 * Shows a list of media servers, allowing to select one for playback.
 * 
 * @author Felix
 *
 */
public class RendererFragment extends ListFragment implements OnBackPressedListener {
	
	private String TAG = "RendererFragment";
	
	/**
	 * ListView adapter of media renderers.
	 */
	private DeviceArrayAdapter mRendererAdapter;
	
	/**
	 * The media renderer that is currently active.
	 */
	@SuppressWarnings("rawtypes")
	private Device mCurrentRenderer;
	
	/**
	 * Stores uri that is to be played if no renderer is selected.
	 */
	private String mCachedPlayUri = "";
	
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
    
	/**
	 * Initializes ListView adapters, launches Cling UPNP service.
	 */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	mRendererAdapter = new DeviceArrayAdapter(getActivity());
    	
        setListAdapter(mRendererAdapter);  

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
    	mCachedPlayUri = "";
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
     * Plays an URI to a media renderer. If none is selected, the URI is 
     * cached and played after selecting one.
     */
	void play(String uri) {
		Log.d(TAG, uri);
		if (mCurrentRenderer != null) {
	    	final Service<?, ?> service = mCurrentRenderer.findService(
	    			new ServiceType("schemas-upnp-org", "AVTransport"));
	    	mUpnpService.getControlPoint().execute(new SetAVTransportURI(service, 
	    			uri, "NO METADATA") {
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
			mCachedPlayUri = uri;
		}
	}
	
	/**
	 * Selects a media renderer.
	 */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (mCurrentRenderer == null) {
			mCurrentRenderer = mRendererAdapter.getItem(position);
			setListAdapter(null);
			if (!mCachedPlayUri.equals("")) {
				play(mCachedPlayUri);
				mCachedPlayUri = "";
			}
		}
	}

	/**
	 * Unselects current media renderer if one is selected.
	 */
	@Override
	public boolean onBackPressed() {
		if (mCurrentRenderer != null) {
	        setListAdapter(mRendererAdapter);  
	        mCurrentRenderer = null;
	        return true;
		}
		return false;
	}

}

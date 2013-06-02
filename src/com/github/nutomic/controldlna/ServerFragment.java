package com.github.nutomic.controldlna;

import java.util.Stack;

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
import org.teleal.cling.support.contentdirectory.callback.Browse;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.Item;

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

import com.github.nutomic.controldlna.MainActivity.OnBackPressedListener;

/**
 * Shows a list of media servers, upon selecting one, allows browsing theur
 * directories.
 * 
 * @author Felix
 *
 */
public class ServerFragment extends ListFragment implements OnBackPressedListener {
	
	private String TAG = "ServerFragment";
	
	/**
	 * ListView adapter for showing a list of DLNA media servers.
	 */
	private DeviceArrayAdapter mServerAdapter;
	
	/**
	 * Reference to the media server of which folders are currently shown. 
	 * Null if media servers are shown.
	 */
	private Device<?, ?, ?> mCurrentServer;
	
	/**
	 * ListView adapter for showing a list of files/folders.
	 */
	private FileArrayAdapter mFileAdapter;

	/**
	 * Holds path to current directory on top, paths for higher directories 
	 * behind that.
	 */
	private Stack<String> mCurrentPath = new Stack<String>();
	
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
            mUpnpService.getRegistry().addListener(registryListener);
            mUpnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            mUpnpService = null;
        }
    };

    /**
     * Receives updates when devices are added or removed.
     */
    private RegistryListener registryListener = new RegistryListener() {
		
		@Override
		public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
			if (device == mCurrentServer)
				getFiles();
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
					if (device.getType().getType().equals("MediaServer"))
						mServerAdapter.add(device);	
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
					mServerAdapter.remove(device);	
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
    	mServerAdapter = new DeviceArrayAdapter(getActivity());
    	mFileAdapter = new FileArrayAdapter(getActivity());
    	
        setListAdapter(mServerAdapter);  

        getActivity().getApplicationContext().bindService(
            new Intent(getActivity(), AndroidUpnpServiceImpl.class),
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        );     
    }

    /**
     * Closes Cling UPNP service.
     */
    @Override
	public void onDestroy() {
        super.onDestroy();
        if (mUpnpService != null)
            mUpnpService.getRegistry().removeListener(registryListener);
        getActivity().getApplicationContext().unbindService(mServiceConnection);
    }
    
    /**
     * Enters directory browsing mode or enters a deeper level directory.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	if (getListAdapter() == mServerAdapter) {
    		setListAdapter(mFileAdapter);
    		mCurrentServer = mServerAdapter.getItem(position);
    		// Root directory.
    		getFiles("0");
    	}
    	else if (getListAdapter() == mFileAdapter) {
    		if (mFileAdapter.getItem(position) instanceof Container) {
    			getFiles(((Container) mFileAdapter.getItem(position)).getId());    			
    		}
    		else {
    			MainActivity activity = (MainActivity) getActivity();
    			activity.play(mFileAdapter.getItem(position)
    					.getFirstResource().getValue());
    		}
    	}
    }
    
    /**
     * Opens a new directory and displays it.
     */
    private void getFiles(String directory) {
		mCurrentPath.push(directory);    	
    	getFiles();
    }
    
    /**
     * Displays the current directory on the ListView.
     */
    private void getFiles() {
    	Service<?, ?> service = mCurrentServer.findService(
    			new ServiceType("schemas-upnp-org", "ContentDirectory"));
		mUpnpService.getControlPoint().execute(new Browse(service, 
				mCurrentPath.peek(), BrowseFlag.DIRECT_CHILDREN) {
		
					@SuppressWarnings("rawtypes")
					@Override
					public void received(ActionInvocation actionInvocation, 
							final DIDLContent didl) {
						getActivity().runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								mFileAdapter.clear();
								for (Container c : didl.getContainers()) 
									mFileAdapter.add(c);
								for (Item i : didl.getItems())
									mFileAdapter.add(i);
							}
						});	
					}
		
					@Override
					public void updateStatus(Status status) {
					}
		
					@SuppressWarnings("rawtypes")
					@Override
					public void failure(ActionInvocation actionInvocation, 
							UpnpResponse operation,	String defaultMessage) {
						Log.w(TAG, "Failed to load directory contents: " + 
							defaultMessage);
					}
					
				});    	
    }
	
    /**
     * Handles back button press to traverse directories (while in directory 
     * browsing mode).
     */
	public boolean onBackPressed() {
    	if (getListAdapter() == mServerAdapter)
    		return false;
		mCurrentPath.pop();
		if (mCurrentPath.empty()) {
    		setListAdapter(mServerAdapter);
    		mCurrentServer = null;
		}
		else {
			getFiles();
		}
		return true;		
	}

}

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

public class ServerFragment extends ListFragment {
	
	private String TAG = "ServerFragment";
	
	/**
	 * ListView adapter for showing a list of DLNA media servers.
	 */
	private ServerArrayAdapter serverAdapter;
	
	/**
	 * Reference to the media server of which folders are currently shown. 
	 * Null if media servers are shown.
	 */
	private Device<?, ?, ?> currentServer;
	
	/**
	 * ListView adapter for showing a list of files/folders.
	 */
	private FileArrayAdapter fileAdapter;

	/**
	 * Holds path to current directory on top, paths for higher directories 
	 * behind that.
	 */
	private Stack<String> currentPath = new Stack<String>();
	
	/**
	 * Cling UPNP service. 
	 */
    private AndroidUpnpService upnpService;

    /**
     * Connection Cling to UPNP service.
     */
    private ServiceConnection serviceConnection= new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;
            Log.i(TAG, "Starting device search");
            upnpService.getRegistry().addListener(registryListener);
            upnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    /**
     * Receives updates when devices are added or removed.
     */
    private RegistryListener registryListener = new RegistryListener() {
		
		@Override
		public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
			if (device == currentServer)
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
			Log.w(TAG, "Device discovery failed");
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
						serverAdapter.add(device);	
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
					serverAdapter.remove(device);	
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
    	serverAdapter = new ServerArrayAdapter(getActivity());
    	fileAdapter = new FileArrayAdapter(getActivity());
    	
        setListAdapter(serverAdapter);  

        getActivity().getApplicationContext().bindService(
            new Intent(getActivity(), AndroidUpnpServiceImpl.class),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        );     
    }

    /**
     * Closes Cling UPNP service.
     */
    @Override
	public void onDestroy() {
        super.onDestroy();
        if (upnpService != null)
            upnpService.getRegistry().removeListener(registryListener);
        getActivity().getApplicationContext().unbindService(serviceConnection);
    }
    
    /**
     * Enters directory browsing mode or enters a deeper level directory.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	if (getListAdapter() == serverAdapter) {
    		setListAdapter(fileAdapter);
    		currentServer = serverAdapter.getItem(position);
    		// Root directory.
    		getFiles("0");
    	}
    	else if (getListAdapter() == fileAdapter) {
    		if (fileAdapter.getItem(position) instanceof Container) {
    			getFiles(((Container) fileAdapter.getItem(position)).getId());    			
    		}
    	}
    }
    
    /**
     * Opens a new directory and displays it.
     */
    private void getFiles(String directory) {
		currentPath.push(directory);    	
    	getFiles();
    }
    
    /**
     * Displays the current directory on the ListView.
     */
    private void getFiles() {
    	Service<?, ?> service = currentServer.findService(
    			new ServiceType("schemas-upnp-org", "ContentDirectory"));
		upnpService.getControlPoint().execute(new Browse(service, 
				currentPath.peek(), BrowseFlag.DIRECT_CHILDREN) {
		
					@SuppressWarnings("rawtypes")
					@Override
					public void received(ActionInvocation actionInvocation, 
							final DIDLContent didl) {
						getActivity().runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								fileAdapter.clear();
								for (Container c : didl.getContainers()) 
									fileAdapter.add(c);
								for (Item i : didl.getItems())
									fileAdapter.add(i);
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
     * 
     * @return True if button press was handled.
     */
	public boolean onBackPressed() {
    	if (getListAdapter() == serverAdapter)
    		return false;
		currentPath.pop();
		if (currentPath.empty()) {
    		setListAdapter(serverAdapter);
    		currentServer = null;
		}
		else {
			getFiles();
		}
		return true;		
	}

}

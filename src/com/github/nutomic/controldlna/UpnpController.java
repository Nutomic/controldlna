package com.github.nutomic.controldlna;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ServiceType;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Handles UPNP connection, including device discovery and general queries.
 * 
 * @author Felix Ableitner
 *
 */
public class UpnpController {
	
	private static final String TAG = "DlnaController";
	
	private DeviceListener mDeviceListener = new DeviceListener();
	
    protected AndroidUpnpService mUpnpService;

    private ServiceConnection mUpnpServiceConnection = new ServiceConnection() {

    	/**
    	 * Registers DeviceListener, adds known devices and starts search.
    	 */
		public void onServiceConnected(ComponentName className, IBinder service) {
            mUpnpService = (AndroidUpnpService) service;
            Log.i(TAG, "Starting device search");          
            mUpnpService.getRegistry().addListener(mDeviceListener);
            for (Device<?, ?, ?> d : mUpnpService.getControlPoint().getRegistry().getDevices()) {
            	if (d instanceof LocalDevice) {
            		mDeviceListener.localDeviceAdded(mUpnpService.getRegistry(), (LocalDevice) d);
            	}
            	else {
            		mDeviceListener.remoteDeviceAdded(mUpnpService.getRegistry(), (RemoteDevice) d);
            	}
            }
            mUpnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            mUpnpService = null;
        }
    };
    
    /**
     * Opens connection to the Cling UPNP service.
     * 
     * @param context Application context.
     */
    public void open(Context context) {
        context.bindService(
            new Intent(context, AndroidUpnpServiceImpl.class),
            mUpnpServiceConnection,
            Context.BIND_AUTO_CREATE
        );    	
    }
    
    /**
     * Closes the connection to the Cling UPNP service.
     * 
     * @param context Application context.
     */
    public void close(Context context) {
        mUpnpService.getRegistry().removeListener(mDeviceListener);
        context.unbindService(mUpnpServiceConnection);	
    }


    /**
     * Returns a device service by name for direct queries.
     */
	public Service<?, ?> getService(Device<?, ?, ?> device, String name) {
		return device.findService(
    			new ServiceType("schemas-upnp-org", name));
	}
	
	public void execute(ActionCallback callback) {
		mUpnpService.getControlPoint().execute(callback);
	}

	public void execute(SubscriptionCallback callback) {
		mUpnpService.getControlPoint().execute(callback);
	}
	
	public DeviceListener getDeviceListener() {
		return mDeviceListener;
	}
	
}

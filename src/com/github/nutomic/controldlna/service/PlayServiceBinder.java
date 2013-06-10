package com.github.nutomic.controldlna.service;

import android.os.Binder;

public class PlayServiceBinder extends Binder {
	
	PlayService mService;
	
	public PlayServiceBinder(PlayService service) {
		mService = service;
	}
	
	public PlayService getService() {
        return mService;
    }
}

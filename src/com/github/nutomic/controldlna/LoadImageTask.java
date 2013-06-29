package com.github.nutomic.controldlna;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Handles background task of loading a bitmap by URI.
 * 
 * @author Felix
 *
 */
public class LoadImageTask extends AsyncTask<URI, Void, Bitmap> {
	
	private static final String TAG = "LoadImageTask";

    @Override
    protected Bitmap doInBackground(URI... uri) {
    	if (uri[0] == null)
    		return null;
    	
		Bitmap bm = null;
	    try {
	        URLConnection conn = new URL(uri[0].toString())
	        		.openConnection();
	        conn.connect();
	        InputStream is = conn.getInputStream();
	        BufferedInputStream bis = new BufferedInputStream(is);
	        bm = BitmapFactory.decodeStream(bis);
	        bis.close();
	        is.close();
	    } catch (IOException e) {
	        Log.w(TAG, "Failed to load artwork image", e);
	    }	
        return bm;
    }

}

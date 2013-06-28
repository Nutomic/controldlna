package com.github.nutomic.controldlna;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * ImageView that can directly load from a UPNP URI.
 * 
 * @author Felix
 *
 */
public class RemoteImageView extends ImageView {
	
	private static final String TAG = "RemoteImageView";
	
	private class LoadImageTask extends AsyncTask<URI, Void, Bitmap> {

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
	
	    @Override
	    protected void onPostExecute(Bitmap bm) {
	    	if (bm != null)
				setImageBitmap(bm);	   
	    	else
				setImageDrawable(null);
	    }
		
	};
	
	public RemoteImageView(Context context) {
		super(context);
	}
	
	public RemoteImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
	
	public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

	public void setImageUri(URI uri) {
		setImageDrawable(null);
		new LoadImageTask().execute(uri);
	}
	
}

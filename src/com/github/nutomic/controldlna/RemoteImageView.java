package com.github.nutomic.controldlna;

import java.net.URI;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * ImageView that can directly load from a UPNP URI.
 * 
 * @author Felix
 *
 */
public class RemoteImageView extends ImageView {
	
	/**
	 * Assigns the icon as image drawable when it is loaded.
	 * 
	 * @author Felix
	 *
	 */
	private class AssignImageTask extends LoadImageTask {
	
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

	/**
	 * Sets the URI where the image should be loaded from, loads and assigns it.
	 */
	public void setImageUri(URI uri) {
		setImageDrawable(null);
		new AssignImageTask().execute(uri);
	}
	
}

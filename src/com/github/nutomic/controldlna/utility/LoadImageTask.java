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

package com.github.nutomic.controldlna.utility;

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
 * @author Felix Ableitner
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

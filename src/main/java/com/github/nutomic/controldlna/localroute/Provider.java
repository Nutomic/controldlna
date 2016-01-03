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

package com.github.nutomic.controldlna.localroute;

import android.content.Context;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.media.AudioManager;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteDescriptor;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderDescriptor;
import android.support.v7.media.MediaRouter;

import com.github.nutomic.controldlna.R;

import java.util.ArrayList;

/**
 * MediaRouteProvider that details the local audio route with its
 * controls to the system.
 *
 * @author felix
 *
 */
final class Provider extends MediaRouteProvider {

	private static final String ROUTE_ID = "local_route";

	AudioManager mAudio = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

	private static final ArrayList<IntentFilter> CONTROL_FILTERS;
	static {
		IntentFilter f = new IntentFilter();
		f.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		f.addAction(MediaControlIntent.ACTION_PLAY);
		f.addAction(MediaControlIntent.ACTION_PAUSE);
		f.addAction(MediaControlIntent.ACTION_SEEK);
		f.addAction(MediaControlIntent.ACTION_STOP);
		f.addDataScheme("http");
		f.addDataScheme("https");
		addDataTypeUnchecked(f, "audio/*");

		CONTROL_FILTERS = new ArrayList<IntentFilter>();
		CONTROL_FILTERS.add(f);
	}

	private static void addDataTypeUnchecked(IntentFilter filter, String type) {
		try {
			filter.addDataType(type);
		}
		catch (MalformedMimeTypeException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Provider(Context context) {
		super(context);

		String routeName = context.getString(R.string.local_device);
		if (context.getPackageName().endsWith(".debug")) {
			routeName = routeName + " (" + context.getString(R.string.debug) + ")";
		}
		MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
				ROUTE_ID, routeName)
				.addControlFilters(CONTROL_FILTERS)
				.setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
				.setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
				.setVolume(mAudio.getStreamVolume(AudioManager.STREAM_MUSIC))
				.setVolumeMax(mAudio.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
				.build();


		MediaRouteProviderDescriptor providerDescriptor =
				new MediaRouteProviderDescriptor.Builder()
				.addRoute(routeDescriptor)
				.build();
		setDescriptor(providerDescriptor);
	}

	@Override
	public RouteController onCreateRouteController(String routeId) {
		return new Controller(routeId, getContext());
	}
}
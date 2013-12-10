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

package com.github.nutomic.controldlna.mediarouter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.MusicTrack;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.ControlRequestCallback;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;

import com.github.nutomic.controldlna.R;
import com.github.nutomic.controldlna.gui.MainActivity;
import com.github.nutomic.controldlna.gui.RouteFragment;
import com.github.nutomic.controldlna.utility.LoadImageTask;

/**
 * Background service that handles media playback to a single UPNP media renderer.
 * 
 * @author Felix Ableitner
 *
 */
public class MediaRouterPlayService extends Service {

	private static final String TAG = "PlayService";
	
	private static final int NOTIFICATION_ID = 1;
	
	private final MediaRouterPlayServiceBinder mBinder = new MediaRouterPlayServiceBinder(this);
	
	private MediaRouter mMediaRouter;
	
	/**
	 * Media items that should be played.
	 */
	private List<Item> mPlaylist = new ArrayList<Item>();
	
	/**
	 * The track that is currently being played.
	 */
	private int mCurrentTrack = -1;
	
	private String mItemId;
	
	private String mSessionId;
	
	private WeakReference<RouteFragment> mRouterFragment = 
			new WeakReference<RouteFragment>(null);
	
	private boolean mPollingStatus = false;
	
	private boolean mBound;
	
	/**
	 * Route that is currently being played to. May be invalid.
	 */
	private RouteInfo mCurrentRoute;
	
	/*
	 * Stops foreground mode and notification if the current route 
	 * has been removed.
	 */
	private MediaRouter.Callback mRouteRemovedCallback = 
			new MediaRouter.Callback() {
		@Override
		public void onRouteRemoved(MediaRouter router, RouteInfo route) {
			if (route.equals(mCurrentRoute))
				stopForeground(true);
		}
};
	
	/**
	 * Creates a notification after the icon bitmap is loaded.
	 */
	private class CreateNotificationTask extends LoadImageTask {
		
		@Override
		protected void onPostExecute(Bitmap result) {
			String title = "";
			String artist = "";
			if (mCurrentTrack < mPlaylist.size()) {
				title = mPlaylist.get(mCurrentTrack).getTitle();
				if (mPlaylist.get(mCurrentTrack) instanceof MusicTrack) {
		        	MusicTrack track = (MusicTrack) mPlaylist.get(mCurrentTrack);
		        	artist = track.getArtists()[0].getName();
				}
			}
			Notification notification = new NotificationCompat.Builder(MediaRouterPlayService.this)
					.setContentIntent(PendingIntent.getActivity(MediaRouterPlayService.this, 0, 
							new Intent(MediaRouterPlayService.this, MainActivity.class), 0))
					.setContentTitle(title)
					.setContentText(artist)
					.setLargeIcon(result)
					.setSmallIcon(R.drawable.ic_launcher)
					.build();
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			startForeground(NOTIFICATION_ID, notification);
		}
		
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
    	mMediaRouter = MediaRouter.getInstance(this);
		pollStatus();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		mBound = true;
		return mBinder;
	}
	
	/**
	 * Stops service after a delay if no media is playing (delay in case the 
	 * fragment is recreated for screen rotation).
	 */
	@Override
	public boolean onUnbind(Intent intent) {		
		new Handler().postDelayed(new Runnable() {
			public void run() {
		    	if (!mPollingStatus && !mBound)
					stopSelf();
		    }
		}, 5000);
		mBound = false;
		return super.onUnbind(intent);
	}
	
	public void setRouterFragment(RouteFragment rf) {
		mRouterFragment = new WeakReference<RouteFragment>(rf);
	}
	
	public void selectRoute(RouteInfo route) {
		mMediaRouter.removeCallback(mRouteRemovedCallback);
		mMediaRouter.selectRoute(route);
		 MediaRouteSelector selector = new MediaRouteSelector.Builder()
		         .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
		         .build();

		 mMediaRouter.addCallback(selector, mRouteRemovedCallback, 0);
		 mCurrentRoute = route;
	}
	
	public void sendControlRequest(Intent intent) {
		mMediaRouter.getSelectedRoute().sendControlRequest(intent, null);
	}
	
	/**
	 * Sets current track in renderer to specified item in playlist, then 
	 * starts playback.
	 */
	public void play(int trackNumber) {        
		if (trackNumber < 0 || trackNumber >= mPlaylist.size())
			return;
		
		mCurrentTrack = trackNumber;
		Item track = mPlaylist.get(trackNumber);
    	DIDLParser parser = new DIDLParser();
		DIDLContent didl = new DIDLContent();
		didl.addItem(track);
		String metadata = "";
		try	{
			metadata = parser.generate(didl, true);
		}
		catch (Exception e)	{
			Log.w(TAG, "Metadata generation failed", e);
		}
		
        Intent intent = new Intent(MediaControlIntent.ACTION_PLAY);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.setData(Uri.parse(track.getFirstResource().getValue()));
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_METADATA, metadata);
        
        mMediaRouter.getSelectedRoute().sendControlRequest(intent, 
        		new ControlRequestCallback() {
		        	@Override
		        	public void onResult(Bundle data) {
		        		mSessionId = data.getString(MediaControlIntent.EXTRA_SESSION_ID);
		        		mItemId = data.getString(MediaControlIntent.EXTRA_ITEM_ID);
		                mPollingStatus = true;
		                
		                new CreateNotificationTask().execute(mPlaylist.get(mCurrentTrack)
		                		.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class));

						if (mRouterFragment.get() != null)
							mRouterFragment.get().receiveIsPlaying(mCurrentTrack);
		        	}
				});
	}
	
	/**
	 * Sends 'pause' signal to current renderer.
	 */
	public void pause() {
        Intent intent = new Intent(MediaControlIntent.ACTION_PAUSE);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        mMediaRouter.getSelectedRoute().sendControlRequest(intent, null);
	}
	
	/**
	 * Sends 'resume' signal to current renderer.
	 */
	public void resume() {
        Intent intent = new Intent(MediaControlIntent.ACTION_RESUME);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        mMediaRouter.getSelectedRoute().sendControlRequest(intent, null);	
        mPollingStatus = true;
	}
	
	/**
	 * Sends 'stop' signal to current renderer.
	 */
	public void stop() {
        Intent intent = new Intent(MediaControlIntent.ACTION_STOP);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        mMediaRouter.getSelectedRoute().sendControlRequest(intent, null);
	}
	
	public void seek(int seconds) {
        Intent intent = new Intent(MediaControlIntent.ACTION_SEEK);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
		intent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, mItemId);
		intent.putExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 
				(long) seconds * 1000);
        mMediaRouter.getSelectedRoute().sendControlRequest(intent, null);		
	}
	
	/**
	 * Sets a new playlist and starts playing.
	 * 
	 * @param playlist The media files in the playlist.
	 */
	public void setPlaylist(List<Item> playlist) {
		mPlaylist = playlist;
	}
	
	public void playNext() {
		play(mCurrentTrack + 1);
	}
	
	public void playPrevious() {
		play(mCurrentTrack - 1);
	}
	
	public int getCurrentTrack() {
		return mCurrentTrack;
	}
	/**
	 * Requests playback information every second, as long as RendererFragment
	 * is attached or media is playing.
	 */
	private void pollStatus() {
		if (mPollingStatus && mSessionId != null && mItemId != null) {
			Intent i = new Intent();
			i.setAction(MediaControlIntent.ACTION_GET_STATUS);
			i.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
			i.putExtra(MediaControlIntent.EXTRA_ITEM_ID, mItemId);
			mMediaRouter.getSelectedRoute().sendControlRequest(i, 
					new ControlRequestCallback() {
						@Override
						public void onResult(Bundle data) {
							MediaItemStatus status = MediaItemStatus.fromBundle(data);

							if (mRouterFragment.get() != null)
								mRouterFragment.get().receivePlaybackStatus(status);
							if (status.getPlaybackState() != MediaItemStatus.PLAYBACK_STATE_PENDING &&
									status.getPlaybackState() != MediaItemStatus.PLAYBACK_STATE_BUFFERING &&
											status.getPlaybackState() != MediaItemStatus.PLAYBACK_STATE_PLAYING)
								stopForeground(true);
							
							if (status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_FINISHED) {
								if (mCurrentTrack + 1 < mPlaylist.size())
									playNext();
								else {
									if (!mBound)
										stopSelf();
									mPollingStatus = false;		
								}
							}
						}
					});
		}
		
		new Handler().postDelayed(new Runnable() {
		
			@Override
			public void run() {
				pollStatus();
				}
		}, 1000);
	}	
	
	public void increaseVolume() {
		mMediaRouter.getSelectedRoute().requestUpdateVolume(1);	
	}
	
	public void decreaseVolume() {
		mMediaRouter.getSelectedRoute().requestUpdateVolume(-1);	
	}
	
	public List<Item> getPlaylist() {
		return mPlaylist;
	}

}

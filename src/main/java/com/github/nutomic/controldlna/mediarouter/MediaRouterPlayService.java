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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.ControlRequestCallback;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.github.nutomic.controldlna.R;
import com.github.nutomic.controldlna.gui.MainActivity;
import com.github.nutomic.controldlna.gui.PreferencesActivity;
import com.github.nutomic.controldlna.gui.RouteFragment;
import com.github.nutomic.controldlna.utility.LoadImageTask;

import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

	private boolean mShuffle = false;

	private boolean mRepeat = false;

	private String mItemId;

	private String mSessionId;

	private WeakReference<RouteFragment> mRouterFragment =
			new WeakReference<RouteFragment>(null);

	private boolean mPollingStatus = false;

	private int mStartingTrack = 0;

	private boolean mBound;

	/**
	 * Route that is currently being played to. May be invalid.
	 */
	private RouteInfo mCurrentRoute;

	/*
	 * Stops foreground mode and notification if the current route
	 * has been removed. If the service is not bound, stops it.
	 */
	private MediaRouter.Callback mMediaRouterCallback =
			new MediaRouter.Callback() {
		@Override
		public void onRouteRemoved(MediaRouter router, RouteInfo route) {
			if (route.equals(mCurrentRoute)) {
				stopForeground(true);
			}

			if (!mBound && !mPollingStatus) {
				stopSelf();
			}
		}

		@Override
		public void onRouteAdded(MediaRouter router, RouteInfo route) {
			if (mCurrentRoute != null && route.getId().equals(mCurrentRoute.getId())) {
				selectRoute(route);
				if (mCurrentTrack >= 0 && mCurrentTrack < mPlaylist.size()) {
					new CreateNotificationTask().execute(mPlaylist.get(mCurrentTrack)
							.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class));
				}
			}
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
					if (track.getArtists().length > 0) {
						artist = track.getArtists()[0].getName();
					}
				}
			}
			Intent intent = new Intent(MediaRouterPlayService.this, MainActivity.class);
			intent.setAction("showRouteFragment");
			Notification notification = new NotificationCompat.Builder(MediaRouterPlayService.this)
					.setContentIntent(PendingIntent.getActivity(MediaRouterPlayService.this, 0,
							intent, 0))
					.setContentTitle(title)
					.setContentText(artist)
					.setLargeIcon(result)
					.setSmallIcon(R.drawable.ic_launcher)
					.build();
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			startForeground(NOTIFICATION_ID, notification);
		}

	}

	/**
	 * Listens for incoming phone calls and pauses playback then.
	 */
	private class PhoneCallListener extends PhoneStateListener {

		private boolean mPausedForCall = false;

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {

			if (!PreferenceManager.getDefaultSharedPreferences(MediaRouterPlayService.this)
					.getBoolean(PreferencesActivity.KEY_INCOMING_PHONE_CALL_PAUSE, true)) {
				return;
			}

			if (TelephonyManager.CALL_STATE_RINGING == state ||
					TelephonyManager.CALL_STATE_OFFHOOK == state) {
				// phone ringing or call active
				pause();
				mPausedForCall = true;
			}

			if (mPausedForCall && TelephonyManager.CALL_STATE_IDLE == state) {
				// run when class initial and phone call ended
				resume();
				mPausedForCall = false;
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mMediaRouter = MediaRouter.getInstance(this);
		pollStatus();

		PhoneCallListener phoneListener = new PhoneCallListener();
		TelephonyManager telephonyManager =
				(TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);

		MediaRouteSelector selector = new MediaRouteSelector.Builder()
				.addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
				.build();
		mMediaRouter.addCallback(selector, mMediaRouterCallback,
				MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mMediaRouter.removeCallback(mMediaRouterCallback);
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
		if (!mPollingStatus) {
			stopSelf();
		}
		mBound = false;
		return super.onUnbind(intent);
	}

	public void setRouterFragment(RouteFragment rf) {
		mRouterFragment = new WeakReference<RouteFragment>(rf);
	}

	public void selectRoute(RouteInfo route) {
		mMediaRouter.selectRoute(route);
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
		mStartingTrack = 3;
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

				if (mRouterFragment.get() != null) {
					mRouterFragment.get().scrollToCurrent();
				}
			}
		});
	}

	/**
	 * Sends 'pause' signal to current renderer.
	 */
	public void pause() {
		if (mPlaylist.isEmpty())
			return;

		Intent intent = new Intent(MediaControlIntent.ACTION_PAUSE);
		intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
		mMediaRouter.getSelectedRoute().sendControlRequest(intent, null);
		mPollingStatus = false;
		stopForeground(true);
	}

	/**
	 * Sends 'resume' signal to current renderer.
	 */
	public void resume() {
		if (mPlaylist.isEmpty())
			return;

		Intent intent = new Intent(MediaControlIntent.ACTION_RESUME);
		intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
		mMediaRouter.getSelectedRoute().sendControlRequest(intent, null);
		mPollingStatus = true;
		if (mCurrentTrack >= 0 && mCurrentTrack < mPlaylist.size()) {
			new CreateNotificationTask().execute(mPlaylist.get(mCurrentTrack)
					.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class));
		}
	}

	/**
	 * Sends 'stop' signal to current renderer.
	 */
	public void stop() {
		if (mPlaylist.isEmpty())
			return;

		Intent intent = new Intent(MediaControlIntent.ACTION_STOP);
		intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
		mMediaRouter.getSelectedRoute().sendControlRequest(intent, null);
		mPollingStatus = false;
		stopForeground(true);
	}

	public void seek(int seconds) {
		if (mPlaylist.isEmpty())
			return;

		Intent intent = new Intent(MediaControlIntent.ACTION_SEEK);
		intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
		intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
		intent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, mItemId);
		intent.putExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, (long) seconds * 1000);
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

	public void append(List<Item> list)
	{
		mPlaylist.addAll(list);
	}

	public void remove(int index)
	{
		mPlaylist.remove(index);
	}

	public void insert(Item obj, int pos)
	{
		mPlaylist.add(pos,obj);
	}

	/**
	 * Plays the track after current in the playlist.
	 *
	 * @return True if another item is played, false if the end
	 * of the playlist is reached.
	 */
	public boolean playNext() {
		if (mCurrentTrack == -1)
			return false;

		if (mShuffle) {
			// Play random item.
			play(new Random().nextInt(mPlaylist.size()));
			return true;
		}
		else if (mCurrentTrack + 1 < mPlaylist.size()) {
			// Playlist not over, play next item.
			play(mCurrentTrack + 1);
			return true;
		}
		else if (mRepeat) {
			// Playlist over, repeat it.
			play(0);
			return true;
		}
		else {
			// Playlist over, stop playback.
			stop();
			if (!mBound) {
				stopSelf();
			}
			mPollingStatus = false;
			return false;
		}
	}


	/**
	 * Plays the track before current in the playlist.
	 */
	public void playPrevious() {
		if (mCurrentTrack == -1)
			return;

		if (mShuffle) {
			// Play random item.
			play(new Random().nextInt(mPlaylist.size()));
		}
		else {
			play(mCurrentTrack - 1);
		}
	}

	/**
	 * Returns index of the track that is currently played (zero-based).
	 * @return
	 */
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
				public void onError(String error, Bundle data) {
					if (error != null) {
						Log.w(TAG, "Failed to get status: " + error);
					}
				}

				@Override
				public void onResult(Bundle data) {
					MediaItemStatus status = MediaItemStatus.fromBundle(data);
					if (status == null)
						return;

					if (mRouterFragment.get() != null) {
						mRouterFragment.get().receivePlaybackStatus(status);
					}
					if (status.getPlaybackState() != MediaItemStatus.PLAYBACK_STATE_PENDING &&
							status.getPlaybackState() != MediaItemStatus.PLAYBACK_STATE_BUFFERING &&
							status.getPlaybackState() != MediaItemStatus.PLAYBACK_STATE_PLAYING) {
						stopForeground(true);
					}

					if ((status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_FINISHED ||
							status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_CANCELED) &&
							(mStartingTrack == 0)) {
						playNext();
					}
					if (mStartingTrack > 0)
						mStartingTrack--;
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

	public String getVolumeText()
	{
		return String.format(getResources().getString(R.string.volume_text),
			mMediaRouter.getSelectedRoute().getVolume(),
			mMediaRouter.getSelectedRoute().getVolumeMax());
	}

	public List<Item> getPlaylist() {
		return mPlaylist;
	}

	public void toggleShuffleEnabled() {
		mShuffle = !mShuffle;
	}

	public boolean getShuffleEnabled() {
		return mShuffle;
	}

	public void toggleRepeatEnabled() {
		mRepeat = !mRepeat;
	}

	public boolean getRepeatEnabled() {
		return mRepeat;
	}

	public RouteInfo getCurrentRoute() {
		return mCurrentRoute;
	}

	public boolean isLocal()
	{
		return mCurrentRoute.getName().startsWith(getResources().getString(R.string.local_device));
	}

}

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

package com.github.nutomic.controldlna.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.support.avtransport.callback.Pause;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.SetAVTransportURI;
import org.teleal.cling.support.avtransport.callback.Stop;
import org.teleal.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.lastchange.LastChange;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.MusicTrack;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.github.nutomic.controldlna.R;
import com.github.nutomic.controldlna.RendererFragment;

public class PlayService extends Service {

	private static final String TAG = "PlayService";
	
	private static final int mNotificationId = 1;
	
	private final PlayServiceBinder mBinder = new PlayServiceBinder(this);
	
	/**
	 * The DLNA media renderer device that is currently active.
	 */
	private Device<?, ?, ?> mRenderer;
	
	/**
	 * Media items that should be played.
	 */
	private List<Item> mPlaylist = new ArrayList<Item>();
	
	/**
	 * The track that is currently being played.
	 */
	private int mCurrentTrack;
	
	/**
	 * True if a playlist was set with no renderer active.
	 */
	private boolean mWaitingForRenderer;
	
	/**
	 * Used to determine when the player stops due to the media file being 
	 * over (so the next one can be played).
	 */
	private AtomicBoolean mManuallyStopped = new AtomicBoolean(false);
	
	private org.teleal.cling.model.meta.Service<?, ?> mAvTransportService;
	
	/**
	 * Cling UPNP service. 
	 */
    private AndroidUpnpService mUpnpService;

    /**
     * Connection Cling to UPNP service.
     */
    private ServiceConnection mUpnpServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
            mUpnpService = (AndroidUpnpService) service;
        }

        public void onServiceDisconnected(ComponentName className) {
            mUpnpService = null;
        }
    };

	private SubscriptionCallback mSubscriptionCallback;
	
	
	@Override
	public void onCreate() {
		super.onCreate();
        getApplicationContext().bindService(
            new Intent(this, AndroidUpnpServiceImpl.class),
            mUpnpServiceConnection,
            Context.BIND_AUTO_CREATE
        );
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "test2");
		return mBinder;
	}
	
	/**
	 * Sets current track in renderer to specified item in playlist, then 
	 * starts playback.
	 */
	public void playTrack(int track) {
		if (track < 0 || track >= mPlaylist.size())
			return;
		mCurrentTrack = track;
    	DIDLParser parser = new DIDLParser();
		DIDLContent didl = new DIDLContent();
		didl.addItem(mPlaylist.get(track));
		String metadata;
		try	{
			metadata = parser.generate(didl, true);
		}
		catch (Exception e)	{
			Log.w(TAG, "Metadata serialization failed", e);
			metadata = "NO METADATA";
		}
    	mUpnpService.getControlPoint().execute(new SetAVTransportURI(
    			mAvTransportService, 
    			mPlaylist.get(track).getFirstResource().getValue(), metadata) {
			@SuppressWarnings("rawtypes")
			@Override
            public void failure(ActionInvocation invocation, 
            		UpnpResponse operation, String defaultMsg) {
                Log.w(TAG, "Playback failed: " + defaultMsg);
            }
            
			@SuppressWarnings("rawtypes")
			@Override
    		public void success(ActionInvocation invocation) {
    	    	play();
    		}
        });
	}
	
	private void updateNotification() {
		String title = "";
		String artist = "";
		if (mCurrentTrack < mPlaylist.size()) {
			title = mPlaylist.get(mCurrentTrack).getTitle();
			if (mPlaylist.get(mCurrentTrack) instanceof MusicTrack) {
	        	MusicTrack track = (MusicTrack) mPlaylist.get(mCurrentTrack);
	        	artist = track.getArtists()[0].getName();
			}
		}
		Notification notification = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentIntent(PendingIntent.getActivity(this, 0, 
						new Intent(this, RendererFragment.class), 0))
				.setContentTitle(title)
				.setContentText(artist)
				.build();
		NotificationManager notificationManager =
			    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(mNotificationId, notification);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
	}
	
	/**
	 * Sends 'play' signal to current renderer.
	 */
	public void play() {
		updateNotification();
		mUpnpService.getControlPoint().execute(
				new Play(mAvTransportService) {
			
			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation, 
					UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Play failed: " + defaultMessage);
			}
		});
	}
	
	/**
	 * Sends 'pause' signal to current renderer.
	 */
	public void pause() {
		mManuallyStopped.set(true);
		mUpnpService.getControlPoint().execute(
				new Pause(mAvTransportService) {
			
			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation, 
					UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Pause failed, trying stop: " + defaultMessage);
				// Sometimes stop works even though pause does not.
				mUpnpService.getControlPoint().execute(
						new Stop(mAvTransportService) {
					
					@Override
					public void failure(ActionInvocation invocation, 
							UpnpResponse operation, String defaultMessage) {
						Log.w(TAG, "Stop failed: " + defaultMessage);
					}
				});		
			}
		});			
	}
	
	public void setRenderer(Device<?, ?, ?> renderer) {
		if (mSubscriptionCallback != null)
			mSubscriptionCallback.end();
		if (mRenderer != null && renderer != mRenderer)
			pause();

		mRenderer = renderer;
		mAvTransportService = mRenderer.findService(
    			new ServiceType("schemas-upnp-org", "AVTransport"));
    	mSubscriptionCallback = new SubscriptionCallback(
    			mAvTransportService, 600) {

    		@SuppressWarnings("rawtypes")
			@Override
			protected void established(GENASubscription sub) {
			}

    		@SuppressWarnings("rawtypes")
			@Override
			protected void ended(GENASubscription sub, CancelReason reason,
					UpnpResponse response) {			
			}

			@SuppressWarnings("rawtypes")
			@Override
			protected void eventReceived(final GENASubscription sub) {				
				@SuppressWarnings("unchecked")
				Map<String, StateVariableValue> m = sub.getCurrentValues();
				try {
					LastChange lastChange = new LastChange(
							new AVTransportLastChangeParser(), 
							m.get("LastChange").toString());
					switch (lastChange.getEventedValue(0, 
							AVTransportVariable.TransportState.class)
									.getValue()) {
					case PLAYING:
				    	break;
					case STOPPED:
						if (!mManuallyStopped.get() && 
								(mPlaylist.size() > mCurrentTrack + 1)) {
							mManuallyStopped.set(false);
							playTrack(mCurrentTrack +1);
							break;
						}
						// fallthrough
					case PAUSED_PLAYBACK:
						NotificationManager notificationManager =
					    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
								notificationManager.cancel(mNotificationId);
						mManuallyStopped.set(false);
				    	break;
				    default:
				    }
					
				} catch (Exception e) {
					Log.w(TAG, "Failed to parse UPNP event", e);
				}	
			}

			@SuppressWarnings("rawtypes")
			@Override
			protected void eventsMissed(GENASubscription sub, 
					int numberOfMissedEvents) {	
			}

			@SuppressWarnings("rawtypes")
			@Override
			protected void failed(GENASubscription sub, UpnpResponse responseStatus,
					Exception exception, String defaultMsg) {	
				Log.d(TAG, defaultMsg);
			}
		};
		mUpnpService.getControlPoint().execute(mSubscriptionCallback);
		if (mWaitingForRenderer)
			playTrack(mCurrentTrack);
	}
	
	/**
	 * Sets a new playlist and starts playing.
	 * 
	 * @param playlist The media files in the playlist.
	 * @param first Index of the first file to play.
	 */
	public void setPlaylist(List<Item> playlist, int first) {
		mPlaylist = playlist;
		if (mRenderer == null) {
			mWaitingForRenderer = true;
			Toast.makeText(this, R.string.select_renderer, Toast.LENGTH_SHORT)
					.show();
			mCurrentTrack = first;
		}
		else 
			playTrack(first);
	}
	
	public void playNext() {
		playTrack(mCurrentTrack + 1);
	}
	
	public void playPrevious() {
		playTrack(mCurrentTrack - 1);
	}
	
	public List<Item> getPlaylist() {
		return mPlaylist;
	}

}

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

package com.github.nutomic.controldlna.upnp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.support.avtransport.callback.GetPositionInfo;
import org.teleal.cling.support.avtransport.callback.Pause;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.Seek;
import org.teleal.cling.support.avtransport.callback.SetAVTransportURI;
import org.teleal.cling.support.avtransport.callback.Stop;
import org.teleal.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.lastchange.LastChange;
import org.teleal.cling.support.model.PositionInfo;
import org.teleal.cling.support.model.SeekMode;
import org.teleal.cling.support.renderingcontrol.callback.SetVolume;

import android.annotation.SuppressLint;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaItemStatus.Builder;
import android.util.Log;

/**
 * Binder for RemotePlayService. Provides a direct interface to a specific route.
 *
 * Clients should use the MediaRouter api through Provider.
 *
 * @author Felix Ableitner
 *
 */
public class RemotePlayServiceBinder extends IRemotePlayService.Stub {

	private static final String TAG = "RemotePlayServiceBinder";

	Device<?, ?, ?> mCurrentRenderer;

	private int mPlaybackState;

	SubscriptionCallback mSubscriptionCallback;

	private RemotePlayService mRps;

	private boolean mStartingPlayback = false;
	public RemotePlayServiceBinder(RemotePlayService rps) {
		mRps = rps;
	}

	@Override
	public void startSearch(Messenger listener)
			throws RemoteException {
		mRps.mListener = listener;
	}

	@Override
	public void selectRenderer(String id) throws RemoteException {
		mCurrentRenderer = mRps.mDevices.get(id);
		for (RemotePlayServiceBinder b : mRps.mBinders.keySet())
			if (b != this && mCurrentRenderer.equals(b.mCurrentRenderer))
				b.unselectRenderer("");

		mSubscriptionCallback = new SubscriptionCallback(
				mCurrentRenderer.findService(
						new ServiceType("schemas-upnp-org", "AVTransport")), 600) {

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
					if (mStartingPlayback || lastChange.getEventedValue(0,
							AVTransportVariable.TransportState.class) == null)
						return;

					switch (lastChange.getEventedValue(0,
							AVTransportVariable.TransportState.class)
							.getValue()) {
							case PLAYING:
								mPlaybackState = MediaItemStatus.PLAYBACK_STATE_PLAYING;
								break;
							case PAUSED_PLAYBACK:
								mPlaybackState = MediaItemStatus.PLAYBACK_STATE_PAUSED;
								break;
							case STOPPED:
								mPlaybackState = MediaItemStatus.PLAYBACK_STATE_FINISHED;
								break;
							case TRANSITIONING:
								mPlaybackState = MediaItemStatus.PLAYBACK_STATE_PENDING;
								break;
							case NO_MEDIA_PRESENT:
								mPlaybackState = MediaItemStatus.PLAYBACK_STATE_ERROR;
								break;
							default:
					}

				} catch (Exception e) {
					Log.w(TAG, "Failed to parse UPNP event", e);
					mRps.sendError("Failed to parse UPNP event");
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
				Log.w(TAG, "Register Subscription Callback failed: " + defaultMsg, exception);
				mRps.sendError("Register Subscription Callback failed: " + defaultMsg);
			}
		};
		mRps.mUpnpService.getControlPoint().execute(mSubscriptionCallback);
	}

	/**
	 * Ends selection, stops playback if possible.
	 */
	@Override
	public void unselectRenderer(String sessionId) throws RemoteException {
		if (mRps.mDevices.get(sessionId) != null)
			stop(sessionId);
		if (mSubscriptionCallback != null)
			mSubscriptionCallback.end();
		mCurrentRenderer = null;
	}

	/**
	 * Sets an absolute volume. The value is assumed to be within the valid
	 * volume range.
	 */
	@Override
	public void setVolume(int volume) throws RemoteException {
		mRps.mUpnpService.getControlPoint().execute(
				new SetVolume(mRps.getService(mCurrentRenderer,
						"RenderingControl"), volume) {

					@SuppressWarnings("rawtypes")
					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMessage) {
						Log.w(TAG, "Set volume failed: " + defaultMessage);
						mRps.sendError("Set volume failed: " + defaultMessage);
					}
				});
	}

	/**
	 * Sets playback source and metadata, then starts playing on
	 * current renderer.
	 */
	@Override
	public void play(final String uri, final String metadata) throws RemoteException {
		mStartingPlayback = true;
		mPlaybackState = MediaItemStatus.PLAYBACK_STATE_BUFFERING;
		mRps.mUpnpService.getControlPoint().execute(
				new Stop(mRps.getService(mCurrentRenderer, "AVTransport")) {

					@SuppressWarnings("rawtypes")
					@Override
					public void failure(ActionInvocation invocation,
							org.teleal.cling.model.message.UpnpResponse operation,
							String defaultMessage) {
						Log.w(TAG, "Stop failed: " + defaultMessage);
						mRps.sendError("Stop failed: " + defaultMessage);
						mStartingPlayback = false;
					}

					@SuppressWarnings("rawtypes")
					@Override
					public void success(ActionInvocation invocation) {
						mRps.mUpnpService.getControlPoint().execute(new SetAVTransportURI(
								mRps.getService(mCurrentRenderer, "AVTransport"),
								uri, metadata) {
							@Override
							public void failure(ActionInvocation invocation,
									UpnpResponse operation, String defaultMsg) {
								Log.w(TAG, "Set URI failed: " + defaultMsg);
								mRps.sendError("Set URI failed: " + defaultMsg);
								mStartingPlayback = false;
							}

							@Override
							public void success(ActionInvocation invocation) {
								// Can't use resume here as we don't have the session id to call.
								mRps.mUpnpService.getControlPoint().execute(
										new Play(mRps.getService(mCurrentRenderer,
												"AVTransport")) {

											@Override
											public void success(ActionInvocation invocation) {
												mPlaybackState = MediaItemStatus.PLAYBACK_STATE_PLAYING;
												mStartingPlayback = false;
											}

											@Override
											public void failure(ActionInvocation invocation,
													UpnpResponse operation, String defaultMessage) {
												Log.w(TAG, "Play failed: " + defaultMessage);
												mRps.sendError("Play failed: " + defaultMessage);
												mStartingPlayback = false;
											}
								});
							}
						});
					}
				});

	}

	/**
	 * Pauses playback on current renderer.
	 */
	@Override
	public void pause(final String sessionId) throws RemoteException {
		mRps.mUpnpService.getControlPoint().execute(
				new Pause(mRps.getService(mRps.mDevices.get(sessionId), "AVTransport")) {

					@SuppressWarnings("rawtypes")
					@Override
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMessage) {
						Log.w(TAG, "Pause failed, trying stop: " + defaultMessage);
						mRps.sendError("Pause failed, trying stop: " + defaultMessage);
						// Sometimes stop works even though pause does not.
						try {
							stop(sessionId);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				});
	}

	@Override
	public void resume(String sessionId) throws RemoteException {
		mRps.mUpnpService.getControlPoint().execute(
				new Play(mRps.getService(mRps.mDevices.get(sessionId),
						"AVTransport")) {

					@Override
					@SuppressWarnings("rawtypes")
					public void failure(ActionInvocation invocation,
							UpnpResponse operation, String defaultMessage) {
						Log.w(TAG, "Resume failed: " + defaultMessage);
						mRps.sendError("Resume failed: " + defaultMessage);
					}
				});
	}

	/**
	 * Stops playback on current renderer.
	 */
	@Override
	public void stop(String sessionId) throws RemoteException {
		mRps.mUpnpService.getControlPoint().execute(
				new Stop(mRps.getService(mRps.mDevices.get(sessionId), "AVTransport")) {

					@SuppressWarnings("rawtypes")
					@Override
					public void failure(ActionInvocation invocation,
							org.teleal.cling.model.message.UpnpResponse operation,
							String defaultMessage) {
						Log.w(TAG, "Stop failed: " + defaultMessage);
						mRps.sendError("Stop failed: " + defaultMessage);
					}
				});
	}

	/**
	 * Seeks to the given absolute time in seconds.
	 */
	@SuppressLint("SimpleDateFormat")
	@Override
	public void seek(String sessionId, String itemId, long milliseconds)
			throws RemoteException {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		mRps.mUpnpService.getControlPoint().execute(new Seek(
				mRps.getService(mRps.mDevices.get(sessionId), "AVTransport"),
				SeekMode.REL_TIME,
				df.format(new Date(milliseconds))) {

			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation,
					UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Seek failed: " + defaultMessage);
				mRps.sendError("Seek failed: " + defaultMessage);
			}

		});
	}

	/**
	 * Sends a message with current status for the route and item.
	 *
	 * If itemId does not match with the item currently played,
	 * MediaItemStatus.PLAYBACK_STATE_INVALIDATED is returned.
	 *
	 * @param sessionId Identifier of the session (equivalent to route) to get info for.
	 * @param itemId Identifier of the item to get info for.
	 * @param requestHash Passed back in message to find original request object.
	 */
	@Override
	public void getItemStatus(String sessionId, final String itemId, final int requestHash)
			throws RemoteException {
		mRps.mUpnpService.getControlPoint().execute(new GetPositionInfo(
				mRps.getService(mRps.mDevices.get(sessionId), "AVTransport")) {

			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation,
					UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Get position failed: " + defaultMessage);
			}

			@SuppressWarnings("rawtypes")
			@Override
			public void received(ActionInvocation invocation, PositionInfo positionInfo) {

				Message msg = Message.obtain(null, Provider.MSG_STATUS_INFO, 0, 0);
				Builder status = null;

				if (positionInfo.getTrackURI() != null && positionInfo.getTrackURI().equals(itemId))
					status = new MediaItemStatus.Builder(mPlaybackState)
							.setContentPosition(positionInfo.getTrackElapsedSeconds() * 1000)
							.setContentDuration(positionInfo.getTrackDurationSeconds() * 1000)
							.setTimestamp(positionInfo.getAbsCount());
				else
					status = new MediaItemStatus.Builder(mPlaybackState);

				msg.getData().putBundle("media_item_status", status.build().asBundle());
				msg.getData().putInt("hash", requestHash);
				mRps.sendMessage(msg);
			}
		});
	}

};
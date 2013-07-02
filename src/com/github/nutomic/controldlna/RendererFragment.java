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

package com.github.nutomic.controldlna;

import java.util.List;
import java.util.Map;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.support.avtransport.callback.GetPositionInfo;
import org.teleal.cling.support.avtransport.callback.Seek;
import org.teleal.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.lastchange.LastChange;
import org.teleal.cling.support.model.PositionInfo;
import org.teleal.cling.support.model.SeekMode;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.renderingcontrol.callback.GetVolume;
import org.teleal.cling.support.renderingcontrol.callback.SetVolume;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.github.nutomic.controldlna.MainActivity.OnBackPressedListener;
import com.github.nutomic.controldlna.service.PlayService;
import com.github.nutomic.controldlna.service.PlayServiceBinder;

/**
 * Shows a list of media servers, allowing to select one for playback.
 * 
 * @author Felix
 *
 */
public class RendererFragment extends Fragment implements 
		OnBackPressedListener, OnItemClickListener, OnClickListener, 
		OnSeekBarChangeListener, OnScrollListener {
	
	private final String TAG = "RendererFragment";
	
	private ListView mListView;
	
	private View mControls;
	private SeekBar mProgressBar;
	private ImageButton mPlayPause;
	
	private boolean mPlaying = false;
	
	private Device<?, ?, ?> mCurrentRenderer;
	
	private View mCurrentTrackView;
	
	/**
	 * ListView adapter of media renderers.
	 */
	private DeviceArrayAdapter mRendererAdapter;
	
	private FileArrayAdapter mPlaylistAdapter;
	
	private SubscriptionCallback mSubscriptionCallback;

	private PlayServiceBinder mPlayService;
	
	private ServiceConnection mPlayServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			mPlayService = (PlayServiceBinder) service;
        }

        public void onServiceDisconnected(ComponentName className) {
            mPlayService = null;
        }
    };
	
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
            Log.i(TAG, "Starting device search");
            mUpnpService.getRegistry().addListener(mRendererAdapter);
            mUpnpService.getControlPoint().search();
            mRendererAdapter.add(
            		mUpnpService.getControlPoint().getRegistry().getDevices());
        }

        public void onServiceDisconnected(ComponentName className) {
            mUpnpService = null;
        }
    };
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
        return inflater.inflate(R.layout.renderer_fragment, null);
	};
	
	/**
	 * Initializes ListView adapters, launches Cling UPNP service.
	 */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	mListView = (ListView) getView().findViewById(R.id.listview);
    	mPlaylistAdapter = new FileArrayAdapter(getActivity());
    	mRendererAdapter = new DeviceArrayAdapter(
    			getActivity(), DeviceArrayAdapter.RENDERER);
        mListView.setAdapter(mRendererAdapter);
        mListView.setOnItemClickListener(this);
		mListView.setOnScrollListener(this);
        mControls = getView().findViewById(R.id.controls);
        mProgressBar = (SeekBar) getView().findViewById(R.id.progressBar);
        mProgressBar.setOnSeekBarChangeListener(this);
        ImageButton previous = (ImageButton) getView().findViewById(R.id.previous);
        previous.setImageResource(R.drawable.ic_media_previous);
        ImageButton next = (ImageButton) getView().findViewById(R.id.next);
        next.setImageResource(R.drawable.ic_media_next);
        mPlayPause = (ImageButton) getView().findViewById(R.id.playpause);
        mPlayPause.setOnClickListener(this);
    	mPlayPause.setImageResource(R.drawable.ic_media_play);
    	getView().findViewById(R.id.previous).setOnClickListener(this);
    	getView().findViewById(R.id.next).setOnClickListener(this);

    	getActivity().startService(new Intent(getActivity(), PlayService.class));
        getActivity().getApplicationContext().bindService(
            new Intent(getActivity(), PlayService.class),
            mPlayServiceConnection,
            Context.BIND_AUTO_CREATE
        );

        getActivity().getApplicationContext().bindService(
            new Intent(getActivity(), AndroidUpnpServiceImpl.class),
            mUpnpServiceConnection,
            Context.BIND_AUTO_CREATE
        );
    }
    
    /**
     * Polls the renderer for the current play progress as long as 
     * mPlaying is true.
     */
    private void pollTimePosition() {
    	final Service<?, ?> service = mCurrentRenderer.findService(
    			new ServiceType("schemas-upnp-org", "AVTransport"));
		mUpnpService.getControlPoint().execute(
				new GetPositionInfo(service) {
			
			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation, 
					UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Get position failed: " + defaultMessage);			
			}
			
			@SuppressWarnings("rawtypes")
			@Override
			public void received(ActionInvocation invocation, PositionInfo positionInfo) {
				mProgressBar.setMax((int) positionInfo.getTrackDurationSeconds());
				mProgressBar.setProgress((int) positionInfo.getTrackElapsedSeconds());
			}
		});
    	
		if (mPlaying) {
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					pollTimePosition();
				}
	        }, 1000);    	
		}
    }

    /**
     * Closes Cling UPNP service.
     */
    @Override
	public void onDestroy() {
        super.onDestroy();
        mUpnpService.getRegistry().removeListener(mRendererAdapter);
        getActivity().getApplicationContext().unbindService(mUpnpServiceConnection);
        getActivity().getApplicationContext().unbindService(mPlayServiceConnection);
    }
	
    /**
     * Sets the new playlist.
     */
	public void setPlaylist(List<Item> playlist, int start) {
		mPlaylistAdapter.clear();
		mPlaylistAdapter.add(playlist);
		mPlayService.getService().setPlaylist(playlist, start);
	}
	
	/**
	 * Selects a media renderer.
	 */
	@Override
	public void onItemClick(AdapterView<?> a, View v, final int position, long id) {
		if (mListView.getAdapter() == mRendererAdapter) {
			if (mCurrentRenderer != null &&
					mCurrentRenderer != mRendererAdapter.getItem(position)) {
				new AlertDialog.Builder(getActivity())
						.setMessage(R.string.exit_renderer)
						.setPositiveButton(android.R.string.yes, 
						new DialogInterface.OnClickListener() {
					
									@Override
									public void onClick(DialogInterface dialog, 
											int which) {
										mControls.setVisibility(View.VISIBLE);
										selectRenderer(mRendererAdapter
												.getItem(position));
									}
								})
						.setNegativeButton(android.R.string.no, null)
						.show();
				
			}
			else {
				mControls.setVisibility(View.VISIBLE);
				selectRenderer(mRendererAdapter.getItem(position));
			}
		}
		else if (mListView.getAdapter() == mPlaylistAdapter)
			mPlayService.getService().playTrack(position);
	}
	
	/**
	 * Shows controls and playlist for the selected media renderer.
	 * 
	 * @param renderer The new renderer to select.
	 */
	private void selectRenderer(Device<?, ?, ?> renderer) {
		if (mCurrentRenderer != renderer) {
			if (mSubscriptionCallback != null)
				mSubscriptionCallback.end();
			
			mCurrentRenderer = renderer;
			mPlayService.getService().setRenderer(renderer);
	    	mSubscriptionCallback = new SubscriptionCallback(
	    			getService("AVTransport"), 600) {
	
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
					if (getActivity() == null) return;
					getActivity().runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
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
							    	mPlayPause.setImageResource(R.drawable.ic_media_pause);
							    	mPlayPause.setContentDescription(getResources().
							    			getString(R.string.pause));
									mPlaying = true;
									pollTimePosition();
									enableTrackHighlight();
							    	break;
								case STOPPED:
									// fallthrough
								case PAUSED_PLAYBACK:
							    	mPlayPause.setImageResource(R.drawable.ic_media_play);
							    	mPlayPause.setContentDescription(getResources().
							    			getString(R.string.play));
									mPlaying = false;	
							    	break;
								default:
									break;
							    }
								
							} catch (Exception e) {
								Log.w(TAG, "Failed to parse UPNP event", e);
							}				
						}
					});
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
		}
		mPlaylistAdapter.clear();
		mPlaylistAdapter.add(mPlayService.getService().getPlaylist());
		mListView.setAdapter(mPlaylistAdapter);
	}
	
	/**
	 * Sets colored background on the item that is currently playing.
	 */
	private void enableTrackHighlight() {
		if (mListView.getAdapter() == mRendererAdapter)
			return;
		disableTrackHighlight();
		mCurrentTrackView = mListView.getChildAt(mPlayService.getService()
				.getCurrentTrack()
				- mListView.getFirstVisiblePosition() + mListView.getHeaderViewsCount());
		if (mCurrentTrackView != null)
			mCurrentTrackView.setBackgroundColor(
					getResources().getColor(R.color.currently_playing_background));	
	}
	
	/**
	 * Removes highlight from the item that was last highlighted.
	 */
	private void disableTrackHighlight() {
		if (mCurrentTrackView != null)
			mCurrentTrackView.setBackgroundColor(Color.TRANSPARENT);
	}

	/**
	 * Unselects current media renderer if one is selected.
	 */
	@Override
	public boolean onBackPressed() {
		if (mListView.getAdapter() == mPlaylistAdapter) {
			mControls.setVisibility(View.GONE);
			mListView.setAdapter(mRendererAdapter);
			disableTrackHighlight();
	        return true;
		}
		return false;
	}

	/**
	 * Plays/pauses playback on button click.
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.playpause:
			if (mPlaying)
				mPlayService.getService().pause();
			else
				mPlayService.getService().play();
			break;
		case R.id.previous:
			mPlayService.getService().playPrevious();
			break;
		case R.id.next:
			mPlayService.getService().playNext();
			break;
		}		
	}

	/**
	 * Sends manual seek on progress bar to renderer.
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, 
			boolean fromUser) {
		if (fromUser) {
			mUpnpService.getControlPoint().execute(new Seek(
	    			getService("AVTransport"), SeekMode.REL_TIME, 
	    			Integer.toString(progress)) {
				
				@SuppressWarnings("rawtypes")
				@Override
				public void failure(ActionInvocation invocation, 
						UpnpResponse operation, String defaultMessage) {
					Log.w(TAG, "Seek failed: " + defaultMessage);
				}
			});			
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	/**
	 * Increases or decreases volume.
	 * 
	 * @param increase True to increase volume, false to decrease.
	 */
	@SuppressWarnings("rawtypes")
	public void changeVolume(final boolean increase) {
		if (mCurrentRenderer == null) {
			Toast.makeText(getActivity(), R.string.select_renderer, 
					Toast.LENGTH_SHORT).show();
			return;			
		}
		final Service<?, ?> service = getService("RenderingControl");
		mUpnpService.getControlPoint().execute(
    			new GetVolume(service) {
			
			@Override
			public void failure(ActionInvocation invocation, 
					UpnpResponse operation, String defaultMessage) {
				Log.d(TAG, "Failed to get current Volume: " + defaultMessage);
			}
			
			@Override
			public void received(ActionInvocation invocation, int volume) {
				int newVolume = volume + ((increase) ? 4 : -4);
				if (newVolume < 0) newVolume = 0;
				mUpnpService.getControlPoint().execute(
						new SetVolume(service, newVolume) {
					
					@Override
					public void failure(ActionInvocation invocation, 
							UpnpResponse operation, String defaultMessage) {
						Log.d(TAG, "Failed to set new Volume: " + defaultMessage);
					}
				});
			}
		});	
	}
	
	private Service<?, ?> getService(String name) {
		return mCurrentRenderer.findService(
    			new ServiceType("schemas-upnp-org", name));
	}

	@Override
	public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
		enableTrackHighlight();
	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {
		enableTrackHighlight();
	}

}

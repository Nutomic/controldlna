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

package com.github.nutomic.controldlna.gui;

import java.util.List;

import org.teleal.cling.support.model.item.Item;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.MediaRouteDiscoveryFragment;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.Callback;
import android.support.v7.media.MediaRouter.ProviderInfo;
import android.support.v7.media.MediaRouter.RouteInfo;
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
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.github.nutomic.controldlna.R;
import com.github.nutomic.controldlna.gui.MainActivity.OnBackPressedListener;
import com.github.nutomic.controldlna.mediarouter.MediaRouterPlayService;
import com.github.nutomic.controldlna.mediarouter.MediaRouterPlayServiceBinder;
import com.github.nutomic.controldlna.utility.FileArrayAdapter;
import com.github.nutomic.controldlna.utility.RouteAdapter;

/**
 * Controls media playback by showing a list of routes, and after selecting one, 
 * the current playlist and playback controls.
 * 
 * @author Felix Ableitner
 *
 */
public class RouteFragment extends MediaRouteDiscoveryFragment implements 
		OnBackPressedListener, OnItemClickListener, OnClickListener, 
		OnSeekBarChangeListener, OnScrollListener {
	
	private ListView mListView;
	
	private View mControls;
	private SeekBar mProgressBar;
	private ImageButton mPlayPause;
	
	private View mCurrentTrackView;
	
	private boolean mPlaying;
	
	private RouteAdapter mRouteAdapter;
	
	private FileArrayAdapter mPlaylistAdapter;
	
	private RouteInfo mSelectedRoute;
	
	/**
	 * If true, the item at this position will be played as soon as a route is selected.
	 */
	private int mStartPlayingOnSelect = -1;

	private MediaRouterPlayServiceBinder mMediaRouterPlayService;
	
	private ServiceConnection mPlayServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			mMediaRouterPlayService = (MediaRouterPlayServiceBinder) service;
			mMediaRouterPlayService.getService().setRouterFragment(RouteFragment.this);
			mPlaylistAdapter.addAll(mMediaRouterPlayService.getService().getPlaylist());
        }

        public void onServiceDisconnected(ComponentName className) {
            mMediaRouterPlayService = null;
        }
    };
    
    /**
     * Selects remote playback route category.
     */
    public RouteFragment() {		
        MediaRouteSelector mSelector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .build();        
        setRouteSelector(mSelector);
	}
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {

        return inflater.inflate(R.layout.route_fragment, null);
	};
	
	/**
	 * Initializes views, connects to service, adds default route.
	 */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	
    	mRouteAdapter = new RouteAdapter(getActivity());
    	mRouteAdapter.addAll(MediaRouter.getInstance(getActivity()).getRoutes());
    	mRouteAdapter.remove(MediaRouter.getInstance(getActivity()).getDefaultRoute());
    	mPlaylistAdapter = new FileArrayAdapter(getActivity());
    	
    	mListView = (ListView) getView().findViewById(R.id.listview);
        mListView.setAdapter(mRouteAdapter);
        mListView.setOnItemClickListener(this);
		mListView.setOnScrollListener(this);
		mListView.setEmptyView(getView().findViewById(android.R.id.empty));
		
        mControls = getView().findViewById(R.id.controls);
        mProgressBar = (SeekBar) getView().findViewById(R.id.progressBar);
        mProgressBar.setOnSeekBarChangeListener(this);
        
        ImageButton previous = (ImageButton) getView().findViewById(R.id.previous);
        previous.setImageResource(R.drawable.ic_media_previous);
    	getView().findViewById(R.id.previous).setOnClickListener(this);
        
        ImageButton next = (ImageButton) getView().findViewById(R.id.next);
        next.setImageResource(R.drawable.ic_media_next);
    	getView().findViewById(R.id.next).setOnClickListener(this);
        
        mPlayPause = (ImageButton) getView().findViewById(R.id.playpause);
        mPlayPause.setOnClickListener(this);
    	mPlayPause.setImageResource(R.drawable.ic_media_play);    	

    	getActivity().getApplicationContext().startService(
    			new Intent(getActivity(), MediaRouterPlayService.class));
        getActivity().getApplicationContext().bindService(
	            new Intent(getActivity(), MediaRouterPlayService.class),
	            mPlayServiceConnection,
	            Context.BIND_AUTO_CREATE
        );
        
        if (savedInstanceState != null) {
        	mListView.onRestoreInstanceState(savedInstanceState.getParcelable("list_state"));
        	if (savedInstanceState.getBoolean("route_selected")) {
        		mSelectedRoute = MediaRouter.getInstance(getActivity())
        				.getSelectedRoute();
    			mListView.setAdapter(mPlaylistAdapter);
    			mControls.setVisibility(View.VISIBLE);        		
        	}
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
 
    	outState.putBoolean("route_selected", mSelectedRoute != null);
    	outState.putParcelable("list_state", mListView.onSaveInstanceState());
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	getActivity().getApplicationContext().unbindService(mPlayServiceConnection);
    }

    /**
     * Starts active route discovery (which is automatically stopped on 
     * fragment stop by parent class).
     */
    @Override
    public int onPrepareCallbackFlags() {
        return  MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY 
                | MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN;
    }
    
    @Override
    public Callback onCreateCallback() {

        return new MediaRouter.Callback() {
            @Override
            public void onRouteAdded(MediaRouter router, RouteInfo route) {
                mRouteAdapter.add(route);
            }

            @Override
            public void onRouteChanged(MediaRouter router, RouteInfo route) {
                mRouteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onRouteRemoved(MediaRouter router, RouteInfo route) {
            	mRouteAdapter.remove(route);
            	if (route.equals(mSelectedRoute)) {
            		mPlaying = false;
                	onBackPressed();
            	}
            }

            @Override
            public void onRouteSelected(MediaRouter router, RouteInfo route) {
            }

            @Override
            public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            }

            @Override
            public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
            }

            @Override
            public void onRoutePresentationDisplayChanged(
                    MediaRouter router, RouteInfo route) {
            }

            @Override
            public void onProviderAdded(MediaRouter router, ProviderInfo provider) {
            }

            @Override
            public void onProviderRemoved(MediaRouter router, ProviderInfo provider) {
            }

            @Override
            public void onProviderChanged(MediaRouter router, ProviderInfo provider) {
            }
        };
        
    }
	
	/**
	 * Selects a route or starts playback (depending on current ListAdapter).
	 */
	@Override
	public void onItemClick(AdapterView<?> a, View v, final int position, long id) {
		if (mListView.getAdapter() == mRouteAdapter) {
			mSelectedRoute = mRouteAdapter.getItem(position);
			mMediaRouterPlayService.getService().selectRoute(mSelectedRoute);
			mListView.setAdapter(mPlaylistAdapter);
			mControls.setVisibility(View.VISIBLE);
			if (mStartPlayingOnSelect != -1) {
				mMediaRouterPlayService.getService().play(mStartPlayingOnSelect);
				mStartPlayingOnSelect = -1;
			}
    		TextView emptyView = (TextView) mListView.getEmptyView();
    		emptyView.setText(R.string.playlist_empty);
		}
		else
			mMediaRouterPlayService.getService().play(position);
	}
	
	/**
	 * Sets colored background on the item that is currently playing.
	 */
	private void enableTrackHighlight() {
		if (mListView.getAdapter() == mRouteAdapter || mMediaRouterPlayService == null || !isVisible())
			return;
		
		disableTrackHighlight();
		mCurrentTrackView = mListView.getChildAt(mMediaRouterPlayService.getService()
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
	 * Unselects current media renderer if one is selected (with dialog).
	 */
	@Override
	public boolean onBackPressed() {
		if (mListView.getAdapter() == mPlaylistAdapter) {
			if (mPlaying) {
				new AlertDialog.Builder(getActivity())
						.setMessage(R.string.exit_renderer)
						.setPositiveButton(android.R.string.yes, 
								new DialogInterface.OnClickListener() {
					
									@Override
									public void onClick(DialogInterface dialog, 
											int which) {
										mMediaRouterPlayService.getService().stop();
										mControls.setVisibility(View.GONE);
										mListView.setAdapter(mRouteAdapter);
										disableTrackHighlight();
										mSelectedRoute = null;
							    		TextView emptyView = (TextView) mListView.getEmptyView();
							    		emptyView.setText(R.string.device_list_empty);
									}
								})
						.setNegativeButton(android.R.string.no, null)
						.show();
			}
			else {
				mControls.setVisibility(View.GONE);
				mListView.setAdapter(mRouteAdapter);
				disableTrackHighlight();
				mSelectedRoute = null;
	    		TextView emptyView = (TextView) mListView.getEmptyView();
	    		emptyView.setText(R.string.device_list_empty);
			}
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
				mMediaRouterPlayService.getService().pause();
			else
				mMediaRouterPlayService.getService().resume();
			break;
		case R.id.previous:
			mMediaRouterPlayService.getService().playPrevious();
			break;
		case R.id.next:
			mMediaRouterPlayService.getService().playNext();
			break;
		}		
	}

	/**
	 * Sends manual seek on progress bar to renderer.
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, 
			boolean fromUser) {
		if (fromUser)
			mMediaRouterPlayService.getService().seek(progress);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	/**
	 * Keeps track highlighting on the correct item while views are rebuilt.
	 */
	@Override
	public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
		enableTrackHighlight();
	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {
		enableTrackHighlight();
	}
	
	public void increaseVolume() {
		mMediaRouterPlayService.getService().increaseVolume();
	}
	
	public void decreaseVolume() {
		mMediaRouterPlayService.getService().decreaseVolume();
	}

	/**
	 * Applies the playlist and starts playing at position.
	 */
	public void play(List<Item> playlist, int start) {
		mPlaylistAdapter.clear();
		mPlaylistAdapter.addAll(playlist);
		mMediaRouterPlayService.getService().setPlaylist(playlist);
		
		if (mSelectedRoute != null)
			mMediaRouterPlayService.getService().play(start);
		else {
			Toast.makeText(getActivity(), R.string.select_renderer, Toast.LENGTH_SHORT)
					.show();
			mStartPlayingOnSelect = start;
		}
	}

	/**
	 * Receives information from MediaRouterPlayService about playback status.
	 */
	public void receivePlaybackStatus(MediaItemStatus status) {
		mProgressBar.setProgress((int) status.getContentPosition() / 1000);
		mProgressBar.setMax((int) status.getContentDuration() / 1000);
		if (status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_PLAYING ||
				status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_BUFFERING ||
				status.getPlaybackState() == MediaItemStatus.PLAYBACK_STATE_PENDING) {
			mPlaying = true;
			mPlayPause.setImageResource(R.drawable.ic_media_pause);
		}
		else {
			mPlaying = false;
			mPlayPause.setImageResource(R.drawable.ic_media_play);
		}
		
		if (mListView.getAdapter() == mPlaylistAdapter) 
			enableTrackHighlight();
		
	}

}

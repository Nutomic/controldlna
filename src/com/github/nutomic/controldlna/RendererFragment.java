package com.github.nutomic.controldlna;

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
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.Seek;
import org.teleal.cling.support.avtransport.callback.SetAVTransportURI;
import org.teleal.cling.support.avtransport.callback.Stop;
import org.teleal.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.lastchange.LastChange;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.PositionInfo;
import org.teleal.cling.support.model.SeekMode;
import org.teleal.cling.support.model.item.Item;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.github.nutomic.controldlna.MainActivity.OnBackPressedListener;

/**
 * Shows a list of media servers, allowing to select one for playback.
 * 
 * @author Felix
 *
 */
public class RendererFragment extends Fragment implements 
		OnBackPressedListener, OnItemClickListener, OnClickListener, 
		OnSeekBarChangeListener {
	
	private final String TAG = "RendererFragment";
	
	private ListView mListView;
	
	private View mControls;
	private SeekBar mProgressBar;
	private Button mPlayPause;
	
	private boolean mPlaying = false;
	
	private Item[] mPlaylist;
	
	/**
	 * ListView adapter of media renderers.
	 */
	private DeviceArrayAdapter mRendererAdapter;
	
	private FileArrayAdapter mPlaylistAdapter;
	
	/**
	 * The media renderer that is currently active.
	 */
	private Device<?, ?, ?> mCurrentRenderer;
	
	/**
	 * First track to be played when a renderer is selected (-1 for none).
	 */
	private int mCachedStart = -1;
	
	private SubscriptionCallback mSubscriptionCallback;
	
	/**
	 * Cling UPNP service. 
	 */
    private AndroidUpnpService mUpnpService;

    /**
     * Connection Cling to UPNP service.
     */
    private ServiceConnection mServiceConnection= new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            mUpnpService = (AndroidUpnpService) service;
            Log.i(TAG, "Starting device search");
            mUpnpService.getRegistry().addListener(mRendererAdapter);
            mUpnpService.getControlPoint().search();
            for (Device<?, ?, ?> d : mUpnpService
            		.getControlPoint().getRegistry().getDevices())
            	mRendererAdapter.add(d);
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
    	mRendererAdapter = new DeviceArrayAdapter(
    			getActivity(), DeviceArrayAdapter.RENDERER);
        mListView.setAdapter(mRendererAdapter);
        mListView.setOnItemClickListener(this);
        mControls = getView().findViewById(R.id.controls);
        mProgressBar = (SeekBar) getView().findViewById(R.id.progressBar);
        mProgressBar.setOnSeekBarChangeListener(this);
        mPlayPause = (Button) getView().findViewById(R.id.playpause);
        mPlayPause.setOnClickListener(this);
    	mPlayPause.setText(R.string.play);

        getActivity().getApplicationContext().bindService(
            new Intent(getActivity(), AndroidUpnpServiceImpl.class),
            mServiceConnection,
            Context.BIND_AUTO_CREATE
        );     
    }
    
    private void pollTimePosition() {
    	final Service<?, ?> service = mCurrentRenderer.findService(
    			new ServiceType("schemas-upnp-org", "AVTransport"));
		mUpnpService.getControlPoint().execute(new GetPositionInfo(service) {
			
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
     * Clears cached playback URI.
     */
    @Override
    public void onPause() {
    	super.onPause();    	
    	mCachedStart = -1;
    }

    /**
     * Closes Cling UPNP service.
     */
    @Override
	public void onDestroy() {
        super.onDestroy();
        if (mUpnpService != null)
            mUpnpService.getRegistry().removeListener(mRendererAdapter);
        getActivity().getApplicationContext().unbindService(mServiceConnection);
    }
	
    /**
     * Sets the new playlist and starts playing it (if a renderer is selected).
     */
	public void setPlaylist(Item[] playlist, int start) {
		mPlaylist = playlist;
		playTrack(start);
	}
	
	/**
	 * Plays the specified track in the current playlist, caches value if no 
	 * renderer is selected.
	 */
	private void playTrack(int track) {
		if (mCurrentRenderer != null) {
			mListView.setAdapter(mPlaylistAdapter);
	    	final Service<?, ?> service = mCurrentRenderer.findService(
	    			new ServiceType("schemas-upnp-org", "AVTransport"));
	    	DIDLParser parser = new DIDLParser();
			DIDLContent didl = new DIDLContent();
			didl.addItem(mPlaylist[track]);
			String metadata;
			try	{
				metadata = parser.generate(didl, true);
			}
			catch (Exception e)	{
				Log.w(TAG, "Metadata serialization failed", e);
				metadata = "NO METADATA";
			}
	    	mUpnpService.getControlPoint().execute(new SetAVTransportURI(service, 
	    			mPlaylist[track].getFirstResource().getValue(), metadata) {
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
		else {
			Toast.makeText(getActivity(), "Please select a renderer.", 
					Toast.LENGTH_SHORT).show();
			mCachedStart = track;
		}		
	}
	
	/**
	 * Selects a media renderer.
	 */
	@Override
	public void onItemClick(AdapterView<?> a, View v, int position, long id) {
		if (mListView.getAdapter() == mRendererAdapter) {
			mCurrentRenderer = mRendererAdapter.getItem(position);
	    	Service<?, ?> service = mCurrentRenderer.findService(
	    			new ServiceType("schemas-upnp-org", "AVTransport"));	
	    	mSubscriptionCallback = new SubscriptionCallback(service, 600) {

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
    			protected void eventReceived(GENASubscription sub) {
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
							getActivity().runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
							    	mPlayPause.setText(R.string.pause);
									mPlaying = true;	
									pollTimePosition();
								}
							});
					    	break;
						case PAUSED_PLAYBACK:
							// fallthrough
						case STOPPED:
							getActivity().runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
							    	mPlayPause.setText(R.string.play);
									mPlaying = false;								
								}
							});
					    	break;
					    default:
					    }
						
					} catch (Exception e) {
						e.printStackTrace();
					}
    			}

    			@SuppressWarnings("rawtypes")
    			@Override
    			protected void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {		
    			}

    			@SuppressWarnings("rawtypes")
    			@Override
    			protected void failed(GENASubscription sub, UpnpResponse responseStatus,
    					Exception exception, String defaultMsg) {	
    				Log.d(TAG, defaultMsg);
    			}
			};
	    	mUpnpService.getControlPoint().execute(mSubscriptionCallback);
			if (mCachedStart != -1) {
				setPlaylist(mPlaylist, mCachedStart);
				mCachedStart = -1;
			}

			mListView.setAdapter(mPlaylistAdapter);
			mControls.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Unselects current media renderer if one is selected.
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
								pause();
								exitPlaylistMode();
							}
						})
				    .setNegativeButton(android.R.string.no, null)
				    .show();
			}     
			else 
				exitPlaylistMode();
	        return true;
		}
		return false;
	}
	
	private void exitPlaylistMode() {
		mCurrentRenderer = null;
		mSubscriptionCallback.end();
		
        mListView.setAdapter(mRendererAdapter);  
        mControls.setVisibility(View.GONE);			
	}

	/**
	 * Plays/pauses playback on button click.
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.playpause:
			if (mPlaying)
				pause();
			else
				play();
		}		
	}
	
	/**
	 * Sends 'pause' signal to current renderer.
	 */
	private void pause() {
    	final Service<?, ?> service = mCurrentRenderer.findService(
    			new ServiceType("schemas-upnp-org", "AVTransport"));
		mUpnpService.getControlPoint().execute(new Stop(service) {
			
			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation, 
					UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Pause failed, trying stop: " + defaultMessage);
				// Sometimes stop works even though pause does not.
				mUpnpService.getControlPoint().execute(new Stop(service) {
					
					@Override
					public void failure(ActionInvocation invocation, 
							UpnpResponse operation, String defaultMessage) {
						Log.w(TAG, "Stop failed: " + defaultMessage);
					}
				});		
			}
		});			
	}
	
	/**
	 * Sends 'play' signal to current renderer.
	 */
	private void play() {
    	final Service<?, ?> service = mCurrentRenderer.findService(
    			new ServiceType("schemas-upnp-org", "AVTransport"));
		mUpnpService.getControlPoint().execute(new Play(service) {
			
			@SuppressWarnings("rawtypes")
			@Override
			public void failure(ActionInvocation invocation, 
					UpnpResponse operation, String defaultMessage) {
				Log.w(TAG, "Play failed: " + defaultMessage);
			}
		});
	}

	/**
	 * Sends manual seek on progress bar to renderer.
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, 
			boolean fromUser) {
		if (fromUser) {
	    	final Service<?, ?> service = mCurrentRenderer.findService(
	    			new ServiceType("schemas-upnp-org", "AVTransport"));
	    	mUpnpService.getControlPoint().execute(new Seek(service, 
	    			SeekMode.REL_TIME, Integer.toString(progress)) {
				
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

}

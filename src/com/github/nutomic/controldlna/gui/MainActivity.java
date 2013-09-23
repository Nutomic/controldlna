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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.github.nutomic.controldlna.R;
import com.github.nutomic.controldlna.UpnpPlayer;

/**
 * Main activity, with tabs for media servers and media renderers.
 * 
 * @author Felix Ableitner
 * 
 */
public class MainActivity extends SherlockFragmentActivity implements
		ActionBar.TabListener {
	
	/**
	 * Manages all UPNP connections including playback.
	 */
	private UpnpPlayer mPlayer = new UpnpPlayer();

	/**
	 * Provides Fragments, holding all of them in memory.
	 */
	private SectionsPagerAdapter mSectionsPagerAdapter = 
			new SectionsPagerAdapter(getSupportFragmentManager());

	/**
	 * Holds the section contents.
	 */
	private ViewPager mViewPager;
	
	/**
	 * Fragment for first tab, holding media renderers.
	 */
	private RendererFragment mRendererFragment = new RendererFragment();
	
	/**
	 * Fragment for second tab, holding media servers.
	 */
	private ServerFragment mServerFragment = new ServerFragment();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// Select correct tab after swiping.
		mViewPager.setOnPageChangeListener(
				new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		actionBar.addTab(actionBar.newTab()
				.setText(R.string.title_server)
				.setTabListener(this));
		actionBar.addTab(actionBar.newTab()
				.setText(R.string.title_renderer)
				.setTabListener(this));
		
		mPlayer.open(getApplicationContext());
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mPlayer.close(getApplicationContext());
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * Returns Fragment corresponding to current tab.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0: return mServerFragment;
			case 1:	return mRendererFragment;
			default: return null;
			}
		}

		@Override
		public int getCount() {
			return 2;
		}
	}
	
	/**
	 * Listener for the 'back' key.
	 */
	public interface OnBackPressedListener {
		public boolean onBackPressed();
	}
	
	/**
	 * Forwards back press to active Fragment (unless the fragment is 
	 * showing its root view).
	 */
	@Override
	public void onBackPressed() {
		OnBackPressedListener currentFragment = (OnBackPressedListener) 
				mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());
		if (!currentFragment.onBackPressed())
			super.onBackPressed();
	}
	
	/**
	 * Changes volume on key press.
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_VOLUME_UP:
		    if (event.getAction() == KeyEvent.ACTION_DOWN)
				mPlayer.increaseVolume();
		    return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		    if (event.getAction() == KeyEvent.ACTION_DOWN)
	    		mPlayer.decreaseVolume();
		    return true;
		default:
		    return super.dispatchKeyEvent(event);
		}
    }
	
	/**
	 * Returns shared instance of UpnpPlayer.
	 */
	public UpnpPlayer getUpnpPlayer() {
		return mPlayer;
	}
	
	/**
	 * Opens the UPNP renderer tab in the activity.
	 */
	public void switchToRendererTab() {
		Log.d("tag", "called");
        getSupportActionBar().selectTab(getSupportActionBar().getTabAt(1));
	}

}

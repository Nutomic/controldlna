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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.github.nutomic.controldlna.R;

import org.fourthline.cling.support.model.item.Item;

import java.util.List;

/**
 * Main activity, with tabs for media servers and media routes.
 *
 * @author Felix Ableitner
 *
 */
public class MainActivity extends ActionBarActivity {

	/**
	 * Interface which allows listening to "back" button presses.
	 */
	public interface OnBackPressedListener {
		boolean onBackPressed();
	}

	FragmentStatePagerAdapter mSectionsPagerAdapter =
			new FragmentStatePagerAdapter(getSupportFragmentManager()) {

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0: return mServerFragment;
			case 1: return mRouteFragment;
			default: return null;
			}
		}

		@Override
		public int getCount() {
			return 2;
		}

	};

	private ServerFragment mServerFragment;

	private RouteFragment mRouteFragment;

	ViewPager mViewPager;

	/**
	 * Initializes tab navigation. If wifi is not connected,
	 * shows a warning dialog.
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ActionBar actionBar = getSupportActionBar();

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		setContentView(R.layout.activity_main);

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		TabListener tabListener = new ActionBar.TabListener() {
			public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
				mViewPager.setCurrentItem(tab.getPosition());
			}

			@Override
			public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
			}

			@Override
			public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
			}
		};

		actionBar.addTab(actionBar.newTab()
				.setText(R.string.title_server)
				.setTabListener(tabListener));
		actionBar.addTab(actionBar.newTab()
				.setText(R.string.title_route)
				.setTabListener(tabListener));

		final WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (!wifi.isWifiEnabled()) {
			String value = PreferenceManager.getDefaultSharedPreferences(this)
					.getString(PreferencesActivity.KEY_ENABLE_WIFI_ON_START, "ask");
			if (value.equals("yes")) {
				wifi.setWifiEnabled(true);
			}
			else if (value.equals("ask")) {
				new AlertDialog.Builder(this)
						.setMessage(R.string.enable_wifi_dialog)
						.setPositiveButton(android.R.string.yes,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										wifi.setWifiEnabled(true);
									}
								})
						.setNegativeButton(android.R.string.no, null)
						.show();
			}
		}


		if (savedInstanceState != null) {
			FragmentManager fm = getSupportFragmentManager();
			mServerFragment = (ServerFragment) fm.getFragment(
					savedInstanceState, ServerFragment.class.getName());
			mRouteFragment = (RouteFragment) fm.getFragment(
					savedInstanceState, RouteFragment.class.getName());
			mViewPager.setCurrentItem(savedInstanceState.getInt("currentTab"));
		}
		else {
			mServerFragment = new ServerFragment();
			mRouteFragment = new RouteFragment();
		}
		onNewIntent(getIntent());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.preferences:
				Intent p = new Intent(this, PreferencesActivity.class);
				startActivity(p);
				return true;
			case R.id.refreshdev:
				mServerFragment.triggerSearch();
				return true;
			case R.id.clearplaylist:
				mRouteFragment.clearPlaylist();
				return true;
			case R.id.about:
				Intent a = new Intent(this, AboutActivity.class);
				startActivity(a);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Displays the RouteFragment immediately (instead of ServerFragment).
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.getAction() != null && intent.getAction().equals("showRouteFragment")) {
			mViewPager.setCurrentItem(1);
			mRouteFragment.scrollToCurrent();
		}
	}

	/**
	 * Saves fragments.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// Avoid crash if called during startup.
		if (mServerFragment != null && mRouteFragment != null) {
			FragmentManager fm = getSupportFragmentManager();
			fm.putFragment(outState, ServerFragment.class.getName(), mServerFragment);
			fm.putFragment(outState, RouteFragment.class.getName(), mRouteFragment);
			outState.putInt("currentTab", mViewPager.getCurrentItem());
		}
	}

	/**
	 * Forwards back press to active Fragment (unless the fragment is
	 * showing its root view).
	 */
	@Override
	public void onBackPressed() {
		OnBackPressedListener currentFragment = (OnBackPressedListener)
				mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());
		if (!currentFragment.onBackPressed()) {
			super.onBackPressed();
		}
	}

	/**
	 * Changes volume on key press (via RouteFragment).
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				mRouteFragment.increaseVolume();
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				mRouteFragment.decreaseVolume();
			}
			return true;
		default:
			return super.dispatchKeyEvent(event);
		}
	}

	/**
	 * Starts playing the playlist from item start (via RouteFragment).
	 */
	public void play(List<Item> playlist, int start) {
		mViewPager.setCurrentItem(1);
		mRouteFragment.play(playlist, start);
	}

	/**
	 * Appends a list of tracks to the current playlist
	 *
	 * @param playlist - the list of items to add
	 */
	public void add(List<Item> playlist) {
		mRouteFragment.append(playlist);
	}

}

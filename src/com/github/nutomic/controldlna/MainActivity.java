package com.github.nutomic.controldlna;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * Main activity, with tabs for media servers and media renderers.
 * 
 * @author Felix
 * 
 */
public class MainActivity extends SherlockFragmentActivity implements
		ActionBar.TabListener {

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
				.setText(R.string.title_renderer)
				.setTabListener(this));
		actionBar.addTab(actionBar.newTab()
				.setText(R.string.title_server)
				.setTabListener(this));
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
			case 0:	return mRendererFragment;
			case 1: return mServerFragment;
			default: return null;
			}
		}

		@Override
		public int getCount() {
			return 2;
		}
	}
	
	/**
	 * Forwards to ServerFragment if it is active.
	 */
	@Override
	public void onBackPressed() {
		if ((getSupportActionBar().getSelectedTab().getPosition() == 1) && 
				!mServerFragment.onBackPressed())
			super.onBackPressed();
	}

}

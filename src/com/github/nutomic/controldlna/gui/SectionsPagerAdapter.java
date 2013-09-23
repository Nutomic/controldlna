package com.github.nutomic.controldlna.gui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;



//Since this is an object collection, use a FragmentStatePagerAdapter,
//and NOT a FragmentPagerAdapter.
public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    /**
     * Fragment for second tab, holding media servers.
     */
    private ServerFragment mServerFragment = new ServerFragment();
    
    /**
     * Fragment for first tab, holding media renderers.
     */
    private RendererFragment mRendererFragment = new RendererFragment();
    
    
	 public SectionsPagerAdapter(FragmentManager fm) {
		 super(fm);
	 }

	@Override
	public Fragment getItem(int position) {
		switch (position) {
		case 0: return mServerFragment;
		case 1: return mRendererFragment;
		default: return null;
		}
	}

	@Override
	public int getCount() {
		return 2;
	}
	
}
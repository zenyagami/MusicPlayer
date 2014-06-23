/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zen.muscplayer;


import com.zen.muscplayer.IMediaPlaybackService;
import com.zen.muscplayer.MusicUtils.ServiceToken;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.SparseArray;
import android.view.Window;

public class MusicBrowserActivity extends FragmentActivity
    implements MusicUtils.Defs, OnPageChangeListener {

    private ServiceToken mToken;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    public MusicBrowserActivity() {
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        String shuf = getIntent().getStringExtra("autoshuffle");
        if ("true".equals(shuf)) {
            mToken = MusicUtils.bindToService(this, autoshuffle);
        }
        setContentView(R.layout.main_activity_pager);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager(),3);
        mPagerAdapter.addFragment(new AlbumBrowserActivity(),R.string.albums_title);
        mPagerAdapter.addFragment(new TrackBrowserActivity(),R.string.tracks_title);
        mPagerAdapter.addFragment(new PlaylistBrowserActivity(),R.string.playlists_title);
        
        mPager.setOffscreenPageLimit(3);
        mPager.setOnPageChangeListener(this);
        mPager.setAdapter(mPagerAdapter);
        
        
    }

    @Override
    public void onDestroy() {
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
        }
        super.onDestroy();
    }

    private ServiceConnection autoshuffle = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            // we need to be able to bind again, so unbind
            try {
                unbindService(this);
            } catch (IllegalArgumentException e) {
            }
            IMediaPlaybackService serv = IMediaPlaybackService.Stub.asInterface(obj);
            if (serv != null) {
                try {
                    serv.setShuffleMode(MediaPlaybackService.SHUFFLE_AUTO);
                } catch (RemoteException ex) {
                }
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
        }
    };
    public class PagerAdapter extends FragmentPagerAdapter {

		private final SparseArray<Fragment> mFragments= new SparseArray<Fragment>();
		private int cont=0;
		private SparseArray<String> titles = new SparseArray<String>();


		// private int NUM_PAGES =0;
		public PagerAdapter(FragmentManager manager, int num_pages) {
			super(manager);
			// this.NUM_PAGES = num_pages;
		}

		public void addFragment(Fragment fragment,int resourceId) {
			mFragments.put(cont, fragment);
			titles.put(cont, getString(resourceId));
			cont++;
			// titulos.add(title);
			notifyDataSetChanged();
		}
		@Override
		public CharSequence getPageTitle(int position) {
			return titles.get(position);
		}

		@Override
		public int getCount() {
			// return NUM_PAGES;
			return mFragments.size();
		}
		
		@Override
		public Fragment getItem(int position) {
			return mFragments.get(position);
		}

	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageSelected(int arg0) {
		Fragment f = mPagerAdapter.getItem(arg0);
		if(f!=null && f instanceof PlaylistBrowserActivity)
		{
			((PlaylistBrowserActivity)f).updateAdatper();
		}
	}

}


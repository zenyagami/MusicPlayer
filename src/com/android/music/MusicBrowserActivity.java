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

package com.android.music;

import com.android.music.MusicUtils.ServiceToken;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Window;

public class MusicBrowserActivity extends FragmentActivity
    implements MusicUtils.Defs {

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
        /*int activeTab = MusicUtils.getIntPref(this, "activetab", R.id.artisttab);
        if (activeTab != R.id.artisttab
                && activeTab != R.id.albumtab
                && activeTab != R.id.songtab
                && activeTab != R.id.playlisttab) {
            activeTab = R.id.artisttab;
        }
        //MusicUtils.activateTab(this, activeTab);*/
        //startActivity(new Intent(getApplicationContext(), MusicBrowser.class));
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        String shuf = getIntent().getStringExtra("autoshuffle");
        if ("true".equals(shuf)) {
            mToken = MusicUtils.bindToService(this, autoshuffle);
        }
        setContentView(R.layout.main_activity_pager);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setOffscreenPageLimit(3);
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
    
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
    	private static final int PAGES =4;
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
		public CharSequence getPageTitle(int position) {
        	int id=0;
			switch (position) {
			case 0:
				id = R.string.artists_title;
				break;
			case 1:
				id = R.string.albums_title;
				break;
			case 2: 
				id=R.string.tracks_title;
				break;
			case 3:
				id = R.string.playlists_title;
				break;
			default:
				id=R.string.tracks_title;
				break;
			}
			return getString(id);
		}

		@Override
        public Fragment getItem(int position) {
        	switch (position) {
			case 0:
				return new ArtistAlbumBrowserActivity();
			case 1:
				return new AlbumBrowserActivity();
			case 2:
				return new TrackBrowserActivity();
			case 3:
				return new PlaylistBrowserActivity();
			default:
				break;
			}
        	return new TrackBrowserActivity();
        }

        @Override
        public int getCount() {
            return PAGES;
        }
    }

}


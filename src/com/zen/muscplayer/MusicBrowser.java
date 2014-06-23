package com.zen.muscplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

public class MusicBrowser extends FragmentActivity{

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		Bundle extras =getIntent().getExtras();
		if(extras==null)
		{
			finish();
			return;
		}
		getActionBar().setDisplayHomeAsUpEnabled(true);
		FragmentTransaction mFragmentTransaction = getSupportFragmentManager().beginTransaction();
		//Fragment frag= new ArtistAlbumBrowserActivity();
		//Fragment frag= new AlbumBrowserActivity();
		Fragment frag= null;
		if(extras.getString("fragment").equals(TrackBrowserActivity.class.getName()))
		{
			frag = new TrackBrowserActivity();
			frag.setArguments(extras);
		}
		Bundle bundle = getIntent().getExtras();
		frag.setArguments(bundle);
		mFragmentTransaction.replace(android.R.id.content , frag);
		mFragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		mFragmentTransaction.commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}

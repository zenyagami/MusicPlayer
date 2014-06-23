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

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.zen.muscplayer.MusicUtils.ServiceToken;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.PopupMenu.OnMenuItemClickListener;


public class AlbumBrowserActivity extends ListFragment
    implements MusicUtils.Defs, ServiceConnection
{
    private String mCurrentAlbumId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    boolean mIsUnknownArtist;
    boolean mIsUnknownAlbum;
    private AlbumListAdapter mAdapter;
    private boolean mAdapterSent;
    //private final static int SEARCH = CHILD_MENU_BASE;
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    private ServiceToken mToken;

    public AlbumBrowserActivity()
    {
    }
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
    	setHasOptionsMenu(true);
        if (icicle != null) {
            mCurrentAlbumId = icicle.getString("selectedalbum");
            mArtistId = icicle.getString("artist");
        } else {
          //  mArtistId = getIntent().getStringExtra("artist");
        }
        super.onCreate(icicle);
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mToken = MusicUtils.bindToService(getActivity(), this);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addDataScheme("file");
        getActivity().registerReceiver(mScanListener, f);

    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
      ListView lv = getListView();
      //lv.setOnCreateContextMenuListener(this);
      lv.setTextFilterEnabled(true);
      mAlbumCursor = getAlbumCursor(null, null);
      if (mAdapter == null) {
          //Log.i("@@@", "starting query");
          mAdapter = new AlbumListAdapter(
                  getActivity(),
                  this,
                  R.layout.single_music_row,
                  mAlbumCursor,
                  new String[] {},
                  new int[] {});
          lv.setAdapter(mAdapter);
          //setTitle(R.string.working_albums);
          //getAlbumCursor(mAdapter.getQueryHandler(), null);
      } else {
          mAdapter.setActivity(this);
          lv.setAdapter(mAdapter);
          mAlbumCursor = mAdapter.getCursor();
          if (mAlbumCursor != null) {
              init(mAlbumCursor);
          } else {
              getAlbumCursor(mAdapter.getQueryHandler(), null);
          }
      }
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
    	  View v =inflater.inflate(R.layout.media_picker_activity,null);
         //MusicUtils.updateButtonBar(getActivity(), R.id.albumtab);
		return v;
	}

	/*@Override
    public Object onRetainNonConfigurationInstance() {
        mAdapterSent = true;
        return mAdapter;
    }*/
    
    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("selectedalbum", mCurrentAlbumId);
        outcicle.putString("artist", mArtistId);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        /*ListView lv = getListView();
        if (lv != null) {
            mLastListPosCourse = lv.getFirstVisiblePosition();
            View cv = lv.getChildAt(0);
            if (cv != null) {
                mLastListPosFine = cv.getTop();
            }
        }*/
        MusicUtils.unbindFromService(mToken);
        // If we have an adapter and didn't send it off to another activity yet, we should
        // close its cursor, which we do by assigning a null cursor to it. Doing this
        // instead of closing the cursor directly keeps the framework from accessing
        // the closed cursor later.
        if (!mAdapterSent && mAdapter != null) {
            mAdapter.changeCursor(null);
        }
        // Because we pass the adapter to the next activity, we need to make
        // sure it doesn't keep a reference to this activity. We can do this
        // by clearing its DatasetObservers, which setListAdapter(null) does.
       // lv.setAdapter(null);
        mAdapter = null;
        getActivity().unregisterReceiver(mScanListener);
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        getActivity().registerReceiver(mTrackListListener, f);
        mTrackListListener.onReceive(null, null);

        MusicUtils.setSpinnerState(getActivity());
    }

    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getListView().invalidateViews();
            MusicUtils.updateNowPlaying(getActivity());
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.setSpinnerState(getActivity());
            mReScanHandler.sendEmptyMessage(0);
            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                MusicUtils.clearAlbumArtCache();
            }
        }
    };
    
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mAdapter != null) {
                getAlbumCursor(mAdapter.getQueryHandler(), null);
            }
        }
    };

    @Override
    public void onPause() {
    	getActivity().unregisterReceiver(mTrackListListener);
        mReScanHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    public void init(Cursor c) {

        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(c); // also sets mAlbumCursor

        if (mAlbumCursor == null) {
            MusicUtils.displayDatabaseError(getActivity());
           // getActivity().closeContextMenu();
            mReScanHandler.sendEmptyMessageDelayed(0, 1000);
            return;
        }

        // restore previous position
        if (mLastListPosCourse >= 0) {
            getListView().setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
            mLastListPosCourse = -1;
        }

        MusicUtils.hideDatabaseError(getActivity());
        //MusicUtils.updateButtonBar(getActivity(), R.id.albumtab);
       // setTitle();
    }

    /*private void setTitle() {
        CharSequence fancyName = "";
        if (mAlbumCursor != null && mAlbumCursor.getCount() > 0) {
            mAlbumCursor.moveToFirst();
            fancyName = mAlbumCursor.getString(
                    mAlbumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
            if (fancyName == null || fancyName.equals(MediaStore.UNKNOWN_STRING))
                fancyName = getText(R.string.unknown_artist_name);
        }

        if (mArtistId != null && fancyName != null)
        	getActivity().setTitle(fancyName);
        else
        	getActivity(). setTitle(R.string.albums_title);
    }*/

    /*@Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(getActivity(), sub);
        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);

        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;
        mAlbumCursor.moveToPosition(mi.position);
        mCurrentAlbumId = mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
        mCurrentAlbumName = mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
        mCurrentArtistNameForAlbum = mAlbumCursor.getString(
                mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
        mIsUnknownArtist = mCurrentArtistNameForAlbum == null ||
                mCurrentArtistNameForAlbum.equals(MediaStore.UNKNOWN_STRING);
        mIsUnknownAlbum = mCurrentAlbumName == null ||
                mCurrentAlbumName.equals(MediaStore.UNKNOWN_STRING);
        if (mIsUnknownAlbum) {
            menu.setHeaderTitle(getString(R.string.unknown_album_name));
        } else {
            menu.setHeaderTitle(mCurrentAlbumName);
        }
        if (!mIsUnknownAlbum || !mIsUnknownArtist) {
            menu.add(0, SEARCH, 0, R.string.search_title);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                // play the selected album
                long [] list = MusicUtils.getSongListForAlbum(getActivity(), Long.parseLong(mCurrentAlbumId));
                MusicUtils.playAll(getActivity(), list, 0);
                return true;
            }

            case QUEUE: {
                long [] list = MusicUtils.getSongListForAlbum(getActivity(), Long.parseLong(mCurrentAlbumId));
                MusicUtils.addToCurrentPlaylist(getActivity(), list);
                return true;
            }

            case NEW_PLAYLIST: {
                Intent intent = new Intent();
                intent.setClass(getActivity(), CreatePlaylist.class);
                startActivityForResult(intent, NEW_PLAYLIST);
                return true;
            }

            case PLAYLIST_SELECTED: {
                long [] list = MusicUtils.getSongListForAlbum(getActivity(), Long.parseLong(mCurrentAlbumId));
                long playlist = item.getIntent().getLongExtra("playlist", 0);
                MusicUtils.addToPlaylist(getActivity(), list, playlist);
                return true;
            }
            case DELETE_ITEM: {
                long [] list = MusicUtils.getSongListForAlbum(getActivity(), Long.parseLong(mCurrentAlbumId));
                String f;
                if (android.os.Environment.isExternalStorageRemovable()) {
                    f = getString(R.string.delete_album_desc);
                } else {
                    f = getString(R.string.delete_album_desc_nosdcard);
                }
                String desc = String.format(f, mCurrentAlbumName);
                Bundle b = new Bundle();
                b.putString("description", desc);
                b.putLongArray("items", list);
                Intent intent = new Intent();
                intent.setClass(getActivity(), DeleteItems.class);
                intent.putExtras(b);
                startActivityForResult(intent, -1);
                return true;
            }
            case SEARCH:
                doSearch();
                return true;

        }
        return super.onContextItemSelected(item);
    }*/

    void doSearch() {
        CharSequence title = null;
        String query = "";
        
        Intent i = new Intent();
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        title = "";
        if (!mIsUnknownAlbum) {
            query = mCurrentAlbumName;
            i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
            title = mCurrentAlbumName;
        }
        if(!mIsUnknownArtist) {
            query = query + " " + mCurrentArtistNameForAlbum;
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
            title = title + " " + mCurrentArtistNameForAlbum;
        }
        // Since we hide the 'search' menu item when both album and artist are
        // unknown, the query and title strings will have at least one of those.
        i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
        title = getString(R.string.mediasearch, title);
        i.putExtra(SearchManager.QUERY, query);

        startActivity(Intent.createChooser(i, title));
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case SCAN_DONE:
                if (resultCode ==Activity.RESULT_CANCELED) {
                	getActivity().finish();
                } else {
                    getAlbumCursor(mAdapter.getQueryHandler(), null);
                }
                break;

            case NEW_PLAYLIST:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        long [] list = MusicUtils.getSongListForAlbum(getActivity(), Long.parseLong(mCurrentAlbumId));
                        MusicUtils.addToPlaylist(getActivity(), list, Long.parseLong(uri.getLastPathSegment()));
                    }
                }
                break;
        }
    }

    @Override
	public void onListItemClick(ListView l, View v, int position, long id)
    {
    	
        /*Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", Long.valueOf(id).toString());
        intent.putExtra("artist", mArtistId);
        startActivity(intent);*/
    	Intent intent = new Intent(getActivity(),MusicBrowser.class);
    	Bundle b = new Bundle();
        b.putString("album", Long.valueOf(id).toString());
        b.putString("artist", mArtistId);
        b.putBoolean("showAlbum", false);
        b.putString("fragment", TrackBrowserActivity.class.getName());
        intent.putExtras(b);
        startActivity(intent);
    }

    @Override
  	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle); // icon will be set in onPrepareOptionsMenu()
        menu.add(0, SHUFFLE_ALL, 0, R.string.shuffle_all).setIcon(R.drawable.ic_menu_shuffle);
  		super.onCreateOptionsMenu(menu, inflater);
  		
  	}
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MusicUtils.setPartyShuffleMenuIcon(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Intent intent;
        Cursor cursor;
        switch (item.getItemId()) {
            case PARTY_SHUFFLE:
                MusicUtils.togglePartyShuffle();
                break;

            case SHUFFLE_ALL:
                cursor = MusicUtils.query(getActivity(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String [] { MediaStore.Audio.Media._ID},
                        MediaStore.Audio.Media.IS_MUSIC + "=1", null,
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                if (cursor != null) {
                    MusicUtils.shuffleAll(getActivity(), cursor);
                    cursor.close();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Cursor getAlbumCursor(AsyncQueryHandler async, String filter) {
        String[] cols = new String[] {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ART
        };


        Cursor ret = null;
        if (mArtistId != null) {
            Uri uri = MediaStore.Audio.Artists.Albums.getContentUri("external",
                    Long.valueOf(mArtistId));
            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            if (async != null) {
                async.startQuery(0, null, uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            } else {
                ret = MusicUtils.query(getActivity(), uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            }
        } else {
            Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            if (async != null) {
                async.startQuery(0, null,
                        uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            } else {
                ret = MusicUtils.query(getActivity(), uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            }
        }
        return ret;
    }
    
    
    static class AlbumListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        
        private final Drawable mNowPlayingOverlay;
        //private final BitmapDrawable mDefaultAlbumIcon;
        private int mAlbumIdx;
        private int mArtistIdx;
        //private int mAlbumArtIndex;
        private Context mContext;
        private final String mUnknownAlbum;
        private final String mUnknownArtist;
        private AlphabetIndexer mIndexer;
        private static AlbumBrowserActivity mActivity;
        private AsyncQueryHandler mQueryHandler;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        protected ImageLoader imageLoader = ImageLoader.getInstance();
        private DisplayImageOptions options;
        
        static class ViewHolder {
            TextView line1;
            TextView line2;
            ImageButton overflow;
            ImageView play_indicator;
            ImageView icon;
        }
        static class popMenuHolder
        {
        	String albumName;
        	String albumId;
        }

        static class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }
            
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                //Log.i("@@@", "query complete");
                mActivity.init(cursor);
            }
        }

		AlbumListAdapter(Context context, AlbumBrowserActivity currentactivity,
                int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to,CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

            mActivity = currentactivity;
            mQueryHandler = new QueryHandler(context.getContentResolver());
            mContext=context;
            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
         //   mAlbumSongSeparator = context.getString(R.string.albumsongseparator);
            options= new DisplayImageOptions.Builder()
    		.showImageForEmptyUri(R.drawable.albumart_mp_unknown_list)
    		.showImageOnFail(R.drawable.albumart_mp_unknown_list)
    		.cacheInMemory(true)
    		.cacheOnDisc(true)
    		 .imageScaleType(ImageScaleType.NONE)
    		.considerExifParams(true)
    		.build();
            imageLoader.init(ImageLoaderConfiguration.createDefault(context));
            Resources r = context.getResources();
            mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);

           // Bitmap b = BitmapFactory.decodeResource(r, R.drawable.albumart_mp_unknown_list);
            //mDefaultAlbumIcon = new BitmapDrawable(context.getResources(), b);
            // no filter or dither, it's a lot faster and we can't tell the difference
            //mDefaultAlbumIcon.setFilterBitmap(false);
            //mDefaultAlbumIcon.setDither(false);
            getColumnIndices(cursor);
           // mResources = context.getResources();
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                mAlbumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
                mArtistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST);
              //  mAlbumArtIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART);
                
                if (mIndexer != null) {
                    mIndexer.setCursor(cursor);
                } else {
                    mIndexer = new MusicAlphabetIndexer(cursor, mAlbumIdx, mContext.getString(
                            R.string.fast_scroll_alphabet));
                }
            }
        }
        
        public void setActivity(AlbumBrowserActivity newactivity) {
            mActivity = newactivity;
        }
        
        public AsyncQueryHandler getQueryHandler() {
            return mQueryHandler;
        }

		@Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
           View v = super.newView(context, cursor, parent);
           ViewHolder vh = new ViewHolder();
           vh.line1 = (TextView) v.findViewById(R.id.playlistTitle);
           vh.line2 = (TextView) v.findViewById(R.id.playlistArtista);
           vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
           vh.icon = (ImageView) v.findViewById(R.id.imgAlbumArt);
           vh.overflow = (ImageButton)v.findViewById(R.id.btnOverFlowMenu);
           v.setTag(vh);
           return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            ViewHolder vh = (ViewHolder) view.getTag();

            String name = cursor.getString(mAlbumIdx);
            String displayname = name;
            boolean unknown = name == null || name.equals(MediaStore.UNKNOWN_STRING); 
            if (unknown) {
                displayname = mUnknownAlbum;
            }
            vh.line1.setText(displayname);
            
            name = cursor.getString(mArtistIdx);
            displayname = name;
            if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
                displayname = mUnknownArtist;
            }
            vh.line2.setText(displayname);
            long aid = cursor.getLong(0);
            imageLoader.displayImage ("content://media/external/audio/albumart/"+ aid, vh.icon,options);
            long currentalbumid = MusicUtils.getCurrentAlbumId();
            //iv = vh.play_indicator;
            if (currentalbumid == aid) {
            	vh.play_indicator.setImageDrawable(mNowPlayingOverlay);
            } else {
            	vh.play_indicator.setImageDrawable(null);
            }
            popMenuHolder hol = new popMenuHolder();
            hol.albumId=cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));;
            hol.albumName = name;
            
            vh.overflow.setTag(hol);
            vh.overflow.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					showPopup(v);
				}
			});
            
        }
        public void showPopup(View v) {
			final popMenuHolder holder = (popMenuHolder) v.getTag();
		    PopupMenu popup = new PopupMenu(mContext, v);
		    popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					mActivity.mCurrentAlbumId = holder.albumId;
					mActivity.mCurrentAlbumName = holder.albumName;
					 switch (item.getItemId()) {
			            case PLAY_SELECTION: {
			                // play the selected album
			                long [] list = MusicUtils.getSongListForAlbum(mContext, Long.parseLong(holder.albumId));
			                MusicUtils.playAll(mContext, list, 0);
			                return true;
			            }

			            case QUEUE: {
			                long [] list = MusicUtils.getSongListForAlbum(mContext, Long.parseLong(holder.albumId));
			                MusicUtils.addToCurrentPlaylist(mContext, list);
			                return true;
			            }

			            case NEW_PLAYLIST: {
			            	mActivity.mCurrentAlbumId = holder.albumId;
			                Intent intent = new Intent();
			                intent.setClass(mContext, CreatePlaylist.class);
			                mActivity.startActivityForResult(intent, NEW_PLAYLIST);
			                return true;
			            }

			            case PLAYLIST_SELECTED: {
			                long [] list = MusicUtils.getSongListForAlbum(mContext, Long.parseLong(holder.albumId));
			                long playlist = item.getIntent().getLongExtra("playlist", 0);
			                MusicUtils.addToPlaylist(mContext, list, playlist);
			                return true;
			            }
			            case DELETE_ITEM: {
			                final long [] list = MusicUtils.getSongListForAlbum(mContext, Long.parseLong(holder.albumId));
			                String f;
			                if (android.os.Environment.isExternalStorageRemovable()) {
			                    f = mContext.getString(R.string.delete_album_desc);
			                } else {
			                    f = mContext.getString(R.string.delete_album_desc_nosdcard);
			                }
			                mActivity.mAlbumCursor = getCursor();
			                String desc = String.format(f, holder.albumName);
			                new AlertDialog.Builder(mActivity.getActivity())
			                .setMessage(desc)
			                .setPositiveButton(mContext.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									MusicUtils.deleteTracks(mContext, list);
									mActivity.getAlbumCursor(mQueryHandler, null);
								}
							}).setNegativeButton(mContext.getString(android.R.string.cancel), null).create().show();
			                
			                return true;
			            }
					 }
					 return false;
			            
			}});
		    Menu menu = popup.getMenu();
		    
		    menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
	        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
	        MusicUtils.makePlaylistMenu(mContext, sub);
	        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);
	        
	       
		    popup.show();
		}
        @Override
        public void changeCursor(Cursor cursor) {
            if (!mActivity.isAdded() || mActivity.isDetached() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != mActivity.mAlbumCursor) {
                mActivity.mAlbumCursor = cursor;
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
        }
        
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (mConstraintIsValid && (
                    (s == null && mConstraint == null) ||
                    (s != null && s.equals(mConstraint)))) {
                return getCursor();
            }
            Cursor c = mActivity.getAlbumCursor(null, s);
            mConstraint = s;
            mConstraintIsValid = true;
            return c;
        }
        
        public Object[] getSections() {
            return mIndexer.getSections();
        }
        
        public int getPositionForSection(int section) {
            return mIndexer.getPositionForSection(section);
        }
        
        public int getSectionForPosition(int position) {
            return 0;
        }
    }

    private Cursor mAlbumCursor;
    private String mArtistId;

    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicUtils.updateNowPlaying(getActivity());
    }

    public void onServiceDisconnected(ComponentName name) {
    	getActivity().finish();
    }
}


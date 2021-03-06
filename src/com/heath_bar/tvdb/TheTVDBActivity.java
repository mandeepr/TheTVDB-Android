/*
│──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────│
│                                                  TERMS OF USE: MIT License                                                   │
│                                                  Copyright © 2012 Heath Paddock                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation    │ 
│files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,    │
│modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software│
│is furnished to do so, subject to the following conditions:                                                                   │
│                                                                                                                              │
│The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.│
│                                                                                                                              │
│THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE          │
│WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR         │
│COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,   │
│ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                         │
├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 */
package com.heath_bar.tvdb;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.internal.widget.IcsListPopupWindow;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.actionbarsherlock.view.Window;
import com.heath_bar.tvdb.UpdateService.LocalBinder;
import com.heath_bar.tvdb.data.FavoritesData;
import com.heath_bar.tvdb.data.adapters.PopupMenuAdapter;
import com.heath_bar.tvdb.data.adapters.SeriesAiredListAdapter;
import com.heath_bar.tvdb.data.adapters.SeriesDbAdapter;

public class TheTVDBActivity extends SherlockListActivity implements OnItemClickListener   {
	

	private FavoritesData favorites;
	private Cursor cursor;								// cursor to hold the favorites from the db
	private Cursor refreshCursor;						// replacement cursor
	private SeriesAiredListAdapter adapter;				// adapter to lookup air times
	private ResponseReceiver updateReceiver;			// listener for updates from the adapter
	private UpdateService favoritesUpdateService;
	private Intent favoritesUpdater;
	private boolean boundToService = false;
	private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
	private boolean syncFavsTVDB;
	private boolean importFavsXBMC;
	
	
	private IcsListPopupWindow sortPopupMenu;			// define a popup menu for the sort button to show
	private String sortBy = SeriesDbAdapter.KEY_TITLE;  // Sort by show title by default
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getSupportActionBar().setHomeButtonEnabled(false);
        setContentView(R.layout.favorites_list);

        // Connect to database
        favorites = new FavoritesData(this);

        if (favoritesUpdater == null)
        	favoritesUpdater = new Intent(getApplicationContext(), UpdateService.class);
        
        // Setup the ListView header
        View header = getLayoutInflater().inflate(R.layout.favorites_header, null);
        getListView().addHeaderView(header, null, false);

        // Setup the sort menu
        PopupMenuAdapter adapter = new PopupMenuAdapter(this, com.actionbarsherlock.R.layout.sherlock_spinner_dropdown_item, getResources().getStringArray(R.array.sortOptions));
        sortPopupMenu = new IcsListPopupWindow(this);
        sortPopupMenu.setAdapter(adapter);
        sortPopupMenu.setModal(true);
        sortPopupMenu.setOnItemClickListener(this);
        
        // Listen for future Preference changes
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
    	  	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    	  		ApplyPreferences(key);
    	  	}
    	};
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(prefListener);
        
        // Apply preferences now
        ApplyPreferences();
	}	

    
    /** When the task is created, or the user returns, refresh to pick up any new favorites */
    @Override
    protected void onStart(){
    	super.onStart();
    	RefreshFavoritesAsync();
	}
    
    
    /** Stop any existing refreshes, and start a new one */
	private void RefreshFavoritesAsync(){

		// Set the empty list to "Loading"
		TextView emptyList = (TextView)findViewById(android.R.id.empty);
		emptyList.setText(getResources().getString(R.string.loading));
				
		// Refresh from the db 
		new QueryDatabaseTask().execute();
		
		// Reset the empty list text
		emptyList = (TextView)findViewById(android.R.id.empty);
		emptyList.setText(getResources().getString(R.string.empty_list_favorites));
		
		// Hide refresh button and show Progress animation
		setSupportProgressBarIndeterminateVisibility(true);

		// Stop the service if it is already running
		if (boundToService)
			favoritesUpdateService.stop();

//		// Unregister for responses from the update service
//		if (updateReceiver != null)
//			unregisterReceiver(updateReceiver);
		
		if (updateReceiver == null){
		// Re-register for responses
		updateReceiver = new ResponseReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(UpdateService.ACTION_UPDATE);
		filter.addAction(UpdateService.ACTION_COMPLETE);
        registerReceiver(updateReceiver, filter);
		}
        
        // Launch the update service to sync the local favorites database with everything else
        startService(favoritesUpdater);
        bindService(favoritesUpdater, updateServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	private class QueryDatabaseTask extends AsyncTask<Void, Void, Cursor>{
	
		@Override
		protected Cursor doInBackground(Void... params) {
			
			try {
		        // Get the favorite shows from the database
		        return favorites.fetchNamedFavorites(sortBy);
		        
			}catch (Exception e){
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Cursor c){
			
			cursor = c;
			
			// Apply the cursor to the ListView
			String[] from = new String[]{SeriesDbAdapter.KEY_TITLE, SeriesDbAdapter.KEY_LAST_AIRED, SeriesDbAdapter.KEY_NEXT_AIRED};
	        int[] to = new int[]{R.id.list_item_title, R.id.last_aired, R.id.next_aired};
	      
	        try{
		        adapter = new SeriesAiredListAdapter(getApplicationContext(), R.layout.show_aired_row, cursor, from, to, 0, AppSettings.listBackgroundColors);
				setListAdapter(adapter);
				getListView().setOnItemClickListener(new ItemClickedListener());
				registerForContextMenu(getListView());
	        }catch (Exception e){
	        	if(AppSettings.LOG_ENABLED)
	        		Log.e("TheTVDBActivity","Failed to set the cursor");
	        	Toast.makeText(getApplicationContext(), "There was a problem loading your favorite shows from the database", Toast.LENGTH_SHORT).show();
	        }			
		}
	}
		
	
	private class ResponseReceiver extends BroadcastReceiver {
		 
		@Override
		public void onReceive(Context context, Intent intent) {
			   
			if (intent.getAction().equals(UpdateService.ACTION_UPDATE)){
			
				refreshCursor = favorites.fetchNamedFavorites(sortBy);
				
		        try {
			        adapter.changeCursor(refreshCursor);
		        }catch(Exception e){}

			} else if (intent.getAction().equals(UpdateService.ACTION_COMPLETE)){
				// Hide the progress animation and show the refresh button
				setSupportProgressBarIndeterminateVisibility(false);
			}
		}
	}
	
	/** Handle clicks on the TV shows */
	private class ItemClickedListener implements OnItemClickListener {
		
	    @Override
	    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
	    	
        	long seriesId = adapter.getItemId(position-1);            
        	Intent myIntent = new Intent(arg0.getContext(), SeriesOverview.class);
        	myIntent.putExtra("id", seriesId);
    		startActivityForResult(myIntent, 0);
	    }
	}
		
	/** Handle long clicks on TV shows */
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == android.R.id.list) {
        	AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        	Cursor c = (Cursor)adapter.getItem(info.position-1);
            
            menu.setHeaderTitle(c.getString(c.getColumnIndex("title")));
            menu.add("Remove");
            c = null;
        }
    }
	
	
	@Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
		// Delete the show in the background
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		new RemoveFavoriteTask().execute(info.id);

		return true;
    }
	
	private class RemoveFavoriteTask extends AsyncTask<Long, Void, Boolean>{
		@Override
		protected Boolean doInBackground(Long... params) {
			return favorites.removeSeries(params[0]);
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			Toast.makeText(getApplicationContext(), "The show has been removed from your favorites.", Toast.LENGTH_SHORT).show();
			new QueryDatabaseTask().execute();
		}
	}
	
	
	
	
	/** Defines call backs for service binding, passed to bindService() */
    private ServiceConnection updateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            favoritesUpdateService = binder.getService();
            boundToService = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        	boundToService = false;
        }
    };
	
	
	
	/** Show the popup menu when the user clicks the sort button */
	@SuppressWarnings("deprecation")
	public void showSortPopupMenu(View v){
		sortPopupMenu.setContentWidth(getWindowManager().getDefaultDisplay().getWidth()/2);
		sortPopupMenu.setAnchorView(v);
		sortPopupMenu.show();
	}
	
	/** Handle clicks on the sort popup menu */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch (position){
		case 0:
			sortBy = SeriesDbAdapter.KEY_TITLE;
			break;
		case 1:
			sortBy = SeriesDbAdapter.KEY_NEXT_AIRED;
			break;
		case 2:
			sortBy = SeriesDbAdapter.KEY_LAST_AIRED;
			break;
		}
		sortPopupMenu.dismiss();
		new QueryDatabaseTask().execute();		
	}
	
	
	
	/** Apply all preferences */
    private void ApplyPreferences() {
    	ApplyPreferences(null);
    }
    
    /** Apply a particular preference */
    private void ApplyPreferences(String key) {
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	if (key == null || key.equals("syncFavsTVDB")){
    		syncFavsTVDB = settings.getBoolean(key, false);
  			if (syncFavsTVDB){
				favorites.uploadLocalFavoritesToTheTVDB();				
  				RefreshFavoritesAsync();
			} 
  		}
    	if (key == null || key.equals("importFavsXBMC")){
  			importFavsXBMC = settings.getBoolean(key, false);
  			if (importFavsXBMC){
  				favorites.importFavoritesFromXBMC();
  				RefreshFavoritesAsync();
  			}
  		}
    	if (key == null || key.equals("textSize")){
	    	float textSize = Float.parseFloat(settings.getString("textSize", "18.0"));
	        TextView textview = (TextView)findViewById(android.R.id.empty);
	        textview.setTextSize(textSize);	
	        
	        View header = getLayoutInflater().inflate(R.layout.text, null);
	        TextView header_text = (TextView) header.findViewById(R.id.text);
	        header_text.setTextSize(textSize*1.1f);
  		}
	}
    
	
	// ACTIONBAR MENU
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

    	
		menu.add("Refresh")
	    	.setIcon(R.drawable.ic_refresh)
	    	.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					RefreshFavoritesAsync();
					return false;
				}
			})
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		menu.add("Search")
			.setIcon(R.drawable.ic_search)
            .setOnMenuItemClickListener(new OnMenuItemClickListener() {
				
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					onSearchRequested();
					return false;
				}
			})
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        
    	
    	
    	menu.add("Preferences")
    		.setIcon(R.drawable.ic_prefs)
    		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					Intent i = new Intent(getApplicationContext(), Preferences.class);
					startActivity(i);
					return false;
				}
			})
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    	
    	return true;
    }
    
	/** Close the database, we're done. */
    @Override
    protected void onDestroy(){
    	super.onDestroy();
    	if (cursor != null)
	    	cursor.close();
	    if (refreshCursor != null)
	    	refreshCursor.close();
	    if (favorites != null)
            favorites.close();
	    
	    if (boundToService){
    		unbindService(updateServiceConnection);
    		boundToService = false;
    	}
	    
	    if (updateReceiver != null)
	    	unregisterReceiver(updateReceiver);
    }
}
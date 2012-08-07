package com.heath_bar.tvdb;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.heath_bar.tvdb.types.Actor;
import com.heath_bar.tvdb.util.StringUtil;
import com.heath_bar.tvdb.xml.handlers.ActorHandler;

public class ActorDetails extends SherlockActivity {

	private long seriesId;
	private String actorName;
	
	// OnCreate... display essentially just a loading screen while we call LoadInfoTask in the background
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.actor_details);
        	
		try {
			
			Bundle extras = getIntent().getExtras();
		    if(extras != null) {
		    	seriesId = getIntent().getLongExtra("seriesId", 0);
		    	actorName = getIntent().getStringExtra("ActorName");
		    	String seriesName = getIntent().getStringExtra("seriesName");
		    	
				// Set title
				getSupportActionBar().setTitle(seriesName);
		    	
		    	// Start the asynchronous load process
		    	setSupportProgressBarIndeterminateVisibility(true);
				new LoadActorDetailsTask().execute(String.valueOf(seriesId), actorName);
		    	
    		}
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	private class LoadActorDetailsTask extends AsyncTask<String, Void, Actor>{
		@Override
		protected Actor doInBackground(String... name) {
			
			try {
				// Lookup episode info
				ActorHandler actorQuery = new ActorHandler();
				Actor theActor = actorQuery.getActor(name[0], name[1]);
				
				// Download the image while we're still in the background thread
				theActor.getImage().Load();
				
				return theActor;
			}catch (Exception e){
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Actor theActor){

			// Populate the activity with the data we just found
			PopulateStuff(theActor);
			
			// Hide the loading text
			findViewById(R.id.loading1).setVisibility(View.GONE);
			setSupportProgressBarIndeterminateVisibility(false);
		}
	}
	
	private void PopulateStuff(Actor theActor){
		
		if (theActor == null){
			Toast.makeText(getApplicationContext(), "Unable to locate any information for that actor", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		// Set Title
		TextView textview = (TextView)findViewById(R.id.title);
		textview.setVisibility(View.VISIBLE);
		textview.setText(theActor.getName());

		// Set Role
		textview = (TextView)findViewById(R.id.role);
		textview.setVisibility(View.VISIBLE);
		textview.setText(StringUtil.commafy(theActor.getRole()));
		
		// Set Image
		if (theActor.getImage().getBitmap() == null || theActor.getImage().getUrl().equals("")){
			// do nothin
		} else {
			ImageView banner = (ImageView)findViewById(R.id.actor_image);
    		banner.setImageBitmap(theActor.getImage().getBitmap());
    		banner.setVisibility(View.VISIBLE);
		}
	}
	
	
	// Home button moves back
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
	     switch (item.getItemId()) {
	         case android.R.id.home:
	        	 finish();
	        	 return true;
	     }
	     return false;
	}
}
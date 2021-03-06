package com.heath_bar.tvdb.data;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.heath_bar.tvdb.data.adapters.SeriesDbAdapter;
import com.heath_bar.tvdb.data.xmlhandlers.TVDBFavoritesHandler;
import com.heath_bar.tvdb.types.FavoriteSeriesInfo;
import com.heath_bar.tvdb.types.exceptions.InvalidAccountIdException;

public class FavoritesData {

	protected Context context;
	private SeriesDbAdapter db;
	
	
	public FavoritesData(Context ctx){
		context = ctx;
		db = new SeriesDbAdapter(ctx);
		db.open();
	}
	
	public void reopen(){
		db.open();
	}
	
	public void close(){
		db.close();
	}
	
	
	
	/** Copy our list of favorites to our account on thetvdb.com; this only happens the first time when we select the option to sync */
	public void uploadLocalFavoritesToTheTVDB() {

		try {
			String accountId = getAccountId();
			
			Cursor c = db.fetchNamedFavorites(SeriesDbAdapter.KEY_TITLE);
	
			TVDBFavoritesHandler tvdb = new TVDBFavoritesHandler();			// re-implement this here vs calling addFavoriteToTheTVDB so we don't have to re-instantiate setFavsAdapter a million times
			while (c.moveToNext()){
				tvdb.addFavorite(accountId, c.getLong(c.getColumnIndex(SeriesDbAdapter.KEY_ID)));
			}
		}catch (InvalidAccountIdException e){
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}catch (Exception e){
			Toast.makeText(context, "Something bad happend while uploading your favorites to thetvdb.com", Toast.LENGTH_LONG).show();
		}
	}

	
	
	/** Push a single new favorite to the account on thetvdb.com */
	private ArrayList<Long> addFavoriteToTheTVDB(String accountId, long seriesId){
		TVDBFavoritesHandler tvdb = new TVDBFavoritesHandler();
		return tvdb.addFavorite(accountId, seriesId);
	}
	
    /** Save the series as a favorite to the database, and if necessary, to thetvdb.com  */
    public long createFavoriteSeries(FavoriteSeriesInfo info) {

    	boolean syncFavsTVDB = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("syncFavsTVDB", false);
    	
    	if (syncFavsTVDB){
    		String accountId;
			try {
				accountId = getAccountId();
	    		addFavoriteToTheTVDB(accountId, info.getSeriesId());
			} catch (InvalidAccountIdException e) {
				Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
			}
    	}
    	return db.addFavoriteSeries(info);
    }

    /** query the tvdb for the user's favorites and put them into the db */
    public void importFavoritesFromTVDB(){
    	
    	String accountId;
		try {
			accountId = getAccountId();
			
			TVDBFavoritesHandler tvdb = new TVDBFavoritesHandler();
	    	ArrayList<Long> favoritesList = tvdb.getFavorites(accountId);
	    	
	    	importFavoritesToDatabase(favoritesList, true);
	    	
		} catch (InvalidAccountIdException e) {
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}
    }

    
    /** NOT IMPLEMENTED: query XBMC for the user's shows and put them into the db */
    public void importFavoritesFromXBMC() {
		// TODO: implement this
    	// this method will help: importFavoritesToDatabase();
	}
        
//    /** append a list of series into the local databse */
//    private void importFavoritesToDatabase(ArrayList<Long> favoritesList){
//    	importFavoritesToDatabase(favoritesList, false);
//    }
    
    /** imports a list of series into the local databse, optionally  */
    private void importFavoritesToDatabase(ArrayList<Long> favoritesList, boolean truncateOthers){
    	
    	if (truncateOthers)
    		db.truncateExcept(favoritesList);
    		
    	for (long seriesId : favoritesList){
    		if (!db.isFavoriteSeries(seriesId)){
    			db.createFavoriteSeries(seriesId);
    		}
    	}
    }
	    

    public Cursor fetchNamedFavorites() {
    	return fetchNamedFavorites(SeriesDbAdapter.KEY_TITLE);
    }
    
	public Cursor fetchNamedFavorites(String sortBy){
		return db.fetchNamedFavorites(sortBy);
	}
	
	public Cursor fetchAllFavorites(){
		return db.fetchAllFavorites();
	}

	public void updateFavorite(FavoriteSeriesInfo info) {
		db.updateFavorite(info);
	}
	
	
	public boolean removeSeries(long seriesId){
		try {
			boolean syncFavoritesTVDB = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("syncFavsTVDB", false);
			
			if (syncFavoritesTVDB){
				String accountId = getAccountId();
				TVDBFavoritesHandler tvdb = new TVDBFavoritesHandler();
				tvdb.removeFavorite(accountId, seriesId);
			}
			return db.removeFavoriteSeries(seriesId);
			
		} catch (InvalidAccountIdException e) {
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
			return false;
		}
	
	}

	
	
	/** Helper function to easily get the accountId */
	private String getAccountId() throws InvalidAccountIdException {
		String accountId = PreferenceManager.getDefaultSharedPreferences(context).getString("accountId", "");
		if (accountId.equals(""))
			throw new InvalidAccountIdException("You must specify your account identifier in the application settings before you can sync your favorite shows.");
		return accountId;
	}

	
	
}

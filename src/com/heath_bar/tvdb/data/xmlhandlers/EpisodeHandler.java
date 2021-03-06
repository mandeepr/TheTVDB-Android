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
package com.heath_bar.tvdb.data.xmlhandlers;

import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.heath_bar.tvdb.AppSettings;
import com.heath_bar.tvdb.types.TvEpisode;
import com.heath_bar.tvdb.util.DateUtil;

public class EpisodeHandler extends DefaultHandler{
	private StringBuilder sb;
    private TvEpisode theEpisode;
    private Context context;
    
    
    public EpisodeHandler(Context ctx){
    	context = ctx;
    }
    
    @Override
	public void startElement(String uri, String name, String qName, Attributes atts) {
	    name = name.trim().toLowerCase();				// format the current element name
	    sb = new StringBuilder();						// Reset the string builder
	    
	    if (name.equals("episode"))
	    	theEpisode = new TvEpisode();
    }
    
    // SAX parsers may return all contiguous character data in a single chunk, or they may split it into several chunks
    // Therefore we must aggregate the data here, and set it in endElement() function
	@Override
	public void characters(char ch[], int start, int length) {
		String chars = (new String(ch).substring(start, start + length));
		sb.append(chars);
	}


    @Override
	public void endElement(String uri, String name, String qName) throws SAXException {
		try {
			name = name.trim().toLowerCase();
			
			if (name.equals("id")){
				theEpisode.setId(Integer.valueOf(sb.toString()));
				theEpisode.getImage().setId("E" + sb.toString());
			} else if (name.equals("episodenumber")){
				theEpisode.setNumber(Integer.valueOf(sb.toString()));
			} else if (name.equals("seasonnumber")) {
				theEpisode.setSeason(Integer.valueOf(sb.toString()));
			} else if (name.equals("episodename")) {
				theEpisode.setName(sb.toString());
			} else if (name.equals("firstaired")){
				long date = DateUtil.parseDate(sb.toString()).getTime()/1000L;
				theEpisode.setAirDate(date);
			} else if (name.equals("director")){
				theEpisode.setDirector(sb.toString());
			} else if (name.equals("writer")){
				theEpisode.setWriter(sb.toString());
			} else if (name.equals("overview")){
				theEpisode.setOverview(sb.toString());
			} else if (name.equals("filename")){
				theEpisode.getImage().setUrl(AppSettings.BANNER_URL + sb.toString());	// IDs are not globally unique. Prefix an "E" to indicate this ID is an episode
			} else if (name.equals("rating")){
				theEpisode.setRating(sb.toString());
			} else if (name.equals("gueststars")){
				theEpisode.setGuestStars(sb.toString());
			} else if (name.equals("imdb_id")){
				theEpisode.setIMDB(sb.toString());
			}
		    
		} catch (Exception e) {
			if (AppSettings.LOG_ENABLED)
				Log.e("xml.handlers.EpisodeHandler", e.toString());
		}
	}
    
	public TvEpisode getEpisode(long episodeId) {
	    try {
	    	
	    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
	    	String languageCode = settings.getString("language", "en");
	    	
			URL url = new URL(AppSettings.EPISODE_FULL_URL + String.valueOf(episodeId) + "/" + languageCode + ".xml");	//http://thetvdb.com/api/0A41C0DEA5531762/episodes/398671/en.xml	

		    SAXParserFactory spf = SAXParserFactory.newInstance();
		    SAXParser sp = spf.newSAXParser();
		    XMLReader xr = sp.getXMLReader();
		    xr.setContentHandler(this);
		    xr.parse(new InputSource(url.openStream()));
		    
		    return theEpisode; 
		} catch (Exception e) {
			if (AppSettings.LOG_ENABLED)
				Log.e("xml.handlers.EpisodeHandler", e.toString());
			return new TvEpisode();
		}
	}
}



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
package com.heath_bar.tvdb.types;

public class FavoriteSeriesInfo {

	public long seriesId;
	public String seriesName;
	public long lastAired;
	public long nextAired;
	
	public FavoriteSeriesInfo(long id, String name, long lastAired, long nextAired){
		seriesId = id;
		seriesName = name;
		this.lastAired = lastAired;
		this.nextAired = nextAired;
	}

	public long getSeriesId() {
		return seriesId;
	}

	public void setSeriesId(long _seriesId) {
		seriesId = _seriesId;
	}

	public String getSeriesName() {
		return seriesName;
	}

	public void setSeriesName(String _seriesName) {
		seriesName = _seriesName;
	}

	public long getLastAired() {
		return lastAired;
	}

	public void setLastAired(long _lastAired) {
		lastAired = _lastAired;
	}

	public long getNextAired() {
		return nextAired;
	}

	public void setNextAired(long _nextAired) {
		nextAired = _nextAired;
	}
	
		
}

package com.sentaroh.android.MediaStoreSync;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import java.io.Serializable;

public class MediaStoreListItem implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String file_path="";
	private String media_type="";
	static final public String MEDIA_STORE_LIST_ITEM_MEDIA_TYPE_IMAGES="Images";
	static final public String MEDIA_STORE_LIST_ITEM_MEDIA_TYPE_AUDIO="Audio";
	static final public String MEDIA_STORE_LIST_ITEM_MEDIA_TYPE_VIDEO="Video";
	static final public String MEDIA_STORE_LIST_ITEM_MEDIA_TYPE_DOCUMENT="Document";
	private String display_name="";
	private boolean media_file_different=false;
	private boolean media_size_different=false;
	private boolean media_mod_different=false;
	private boolean media_file_exists=false;
	private long date_added=0, date_modified=0, media_size=0;
	private long file_modified=0, file_size=0;
	
	public MediaStoreListItem(String fp, String mt, boolean df,
			boolean me, boolean msd, boolean mmd, 
			String dn, long da, long dm, long ms, long flm, long fsz) {
		file_path=fp;
		media_type=mt;
		media_file_different=df;
		media_file_exists=me;
		media_size_different=msd;
		media_mod_different=mmd;
		display_name=dn;
		date_added=da;
		date_modified=dm;
		media_size=ms;
		file_modified=flm;
		file_size=fsz;
	}
	
	public String getFilePath() {return file_path;}
	public String getMediaType() {return media_type;}
	public boolean isFileDifferent() {return media_file_different;}
	public boolean isFileNotExist() {return media_file_exists;}
	public boolean isFileSizeDifferent() {return media_size_different;}
	public boolean isFileLastModifiedDateDifferent() {return media_mod_different;}
	public String getDisplayName() {return display_name;}
	public long getDateAdded() {return date_added;}
	public long getDateModified() {return date_modified;}
	public long getMediaSize() {return media_size;}

	public long getFileSize() {return file_size;}
	public long getFileModififed() {return file_modified;}
	public void setFileSize(long p) {file_size=p;}
	public void setFileModififed(long p) {file_modified=p;}
}

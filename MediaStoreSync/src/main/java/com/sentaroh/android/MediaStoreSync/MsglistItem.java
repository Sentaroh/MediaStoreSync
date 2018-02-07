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

public class MsglistItem implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String msgCat, msgBody, msgDate, msgTime,msgIssuer;
	
	public MsglistItem(String cat, String mdate, String mtime, 
			String issuer, String msg)
	{
		msgCat=cat;
		msgBody= msg;
		msgDate=mdate;
		msgTime=mtime;
		msgIssuer=issuer;
	}
	public String getCat()
	{
		return msgCat;
	}
	public String getMdate()
	{
		return msgDate;
	}
	public String getMtime()
	{
		return msgTime;
	}
	public String getIssuer()
	{
		return msgIssuer;
	}
	public String getMsg()
	{
		return msgBody;
	}
	public String toString() {
		return msgCat+" "+msgDate+" "+msgTime+" "+
				(msgIssuer+"          ").substring(0,9)+msgBody;
	}
}

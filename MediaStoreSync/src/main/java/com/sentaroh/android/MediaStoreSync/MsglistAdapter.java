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

import java.util.ArrayList;
import java.util.List;

import com.sentaroh.android.Utilities.ThemeUtil;
import com.sentaroh.android.Utilities.ThemeColorList;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MsglistAdapter extends ArrayAdapter<MsglistItem> {

	private Context c;
	private int id;
	private ArrayList<MsglistItem>items;
	private boolean msgDataChanged=false;
	@SuppressWarnings("unused")
	private int wsz_w, wsz_h;
	private Activity activity;
	
	private ThemeColorList mThhemeColorList=null;
	
	public MsglistAdapter(Activity act,Context context, int textViewResourceId,
			ArrayList<MsglistItem> objects) {
		super(context, textViewResourceId, objects);
		c = context;
		id = textViewResourceId;
		items = objects;
		activity=act;
		
		mThhemeColorList=ThemeUtil.getThemeColorList(act);
	}

	public void remove(int i) {
		items.remove(i);
		msgDataChanged=true;
	}
	
	@Override
	public void add(MsglistItem mli) {
		items.add(mli);
		msgDataChanged=true;
		notifyDataSetChanged();
	}
	
	public boolean resetDataChanged() {
		boolean tmp=msgDataChanged;
		msgDataChanged=false;
		return tmp;
	};
	
	public ArrayList<MsglistItem> getAllItem() {return items;}
	
	public void setAllItem(List<MsglistItem> p) {
		items.clear();
		if (p!=null) items.addAll(p);
		notifyDataSetChanged();
	}
	
//	@Override
//	public boolean isEnabled(int idx) {
//		 return getItem(idx).getActive().equals("A");
//	}

	@SuppressWarnings("deprecation")
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder=new ViewHolder();
//            holder.tv_row_cat= (TextView) v.findViewById(R.id.msg_list_view_item_cat);
            holder.tv_row_msg= (TextView) v.findViewById(R.id.msg_list_view_item_msg);
            holder.tv_row_time= (TextView) v.findViewById(R.id.msg_list_view_item_time);
            holder.config=v.getResources().getConfiguration();
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        MsglistItem o = getItem(position);
        if (o != null) {
       		wsz_w=activity.getWindow()
    					.getWindowManager().getDefaultDisplay().getWidth();
   			wsz_h=activity.getWindow()
    					.getWindowManager().getDefaultDisplay().getHeight();
    		
    		if (wsz_w>=700) 
        		holder.tv_row_time.setVisibility(TextView.VISIBLE);
        	else holder.tv_row_time.setVisibility(TextView.GONE);

        	if (o.getCat().equals("W")) {
        		holder.tv_row_time.setTextColor(mThhemeColorList.text_color_warning);
        		holder.tv_row_msg.setTextColor(mThhemeColorList.text_color_warning);
            	holder.tv_row_time.setText(o.getMtime());
            	holder.tv_row_msg.setText(o.getMsg());
        	} else if (o.getCat().equals("E")) {
        		holder.tv_row_time.setTextColor(mThhemeColorList.text_color_error);
        		holder.tv_row_msg.setTextColor(mThhemeColorList.text_color_error);
        		holder.tv_row_time.setText(o.getMtime());
            	holder.tv_row_msg.setText(o.getMsg());
        	} else {
        		holder.tv_row_time.setTextColor(mThhemeColorList.text_color_primary);
        		holder.tv_row_msg.setTextColor(mThhemeColorList.text_color_primary);
        		holder.tv_row_time.setText(o.getMtime());
            	holder.tv_row_msg.setText(o.getMsg());
        	}
       	}
        return v;
	};
	
	static class ViewHolder {
//		TextView tv_row_cat;
		TextView tv_row_time;
		TextView tv_row_msg;
		Configuration config;
	}
}


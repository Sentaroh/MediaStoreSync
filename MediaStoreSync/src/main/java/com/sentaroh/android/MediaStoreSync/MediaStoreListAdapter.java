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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MediaStoreListAdapter extends BaseAdapter {
	private Context c;
	private ArrayList<MediaStoreListItem> msl;
	private Resources resources=null;
	private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
	
	public MediaStoreListAdapter(Context context) {
		c=context;
		msl=new ArrayList<MediaStoreListItem>();
		resources=context.getResources();
	}

	public void sort() {
		Collections.sort(msl, new Comparator<MediaStoreListItem>() {
			@Override
			public int compare(MediaStoreListItem lhs,
					MediaStoreListItem rhs) {
				String l_key=lhs.getMediaType().substring(0,1)+lhs.getDisplayName();
				String r_key=rhs.getMediaType().substring(0,1)+rhs.getDisplayName();
				return l_key.compareToIgnoreCase(r_key);
			}
		});
	}
	
	public MediaStoreListItem getItem(int i) {
		 return msl.get(i);
	}
	
	public void setArrayList(ArrayList<MediaStoreListItem> p) {
		msl=p;
		notifyDataSetChanged();
	}
	
	public void add(MediaStoreListItem p) {
		msl.add(p);
		notifyDataSetChanged();
	}

	public void remove(MediaStoreListItem p) {
		msl.remove(p);
		notifyDataSetChanged();
	}

	public void replace(int pos, MediaStoreListItem p) {
		msl.set(pos,p);
		notifyDataSetChanged();
	}

	public void remove(int i) {
		msl.remove(i);
		notifyDataSetChanged();
	}
	public void clear() {
		msl.clear();
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return msl.size();
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            holder=new ViewHolder();
            LayoutInflater vi=(LayoutInflater)
        			c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.media_store_list_item, null);
       	
            holder.tv_name=(TextView)v.findViewById(R.id.media_store_list_name);
            holder.tv_filepath=(TextView)v.findViewById(R.id.media_store_list_path);
            holder.tv_diff=(TextView)v.findViewById(R.id.media_store_list_diff);
            holder.tv_hdr_name=(TextView)v.findViewById(R.id.media_store_list_name_hdr);
            holder.tv_hdr_filepath=(TextView)v.findViewById(R.id.media_store_list_path_hdr);
            holder.tv_hdr_diff=(TextView)v.findViewById(R.id.media_store_list_diff_hdr);
            holder.iv_cat=(ImageView)v.findViewById(R.id.media_store_list_category);
            holder.tv_hdr_media=(TextView)v.findViewById(R.id.media_store_list_media_hdr);
            holder.tv_hdr_file=(TextView)v.findViewById(R.id.media_store_list_file_hdr);
            holder.tv_media=(TextView)v.findViewById(R.id.media_store_list_media);
            holder.tv_file=(TextView)v.findViewById(R.id.media_store_list_file);
            
            holder.file_size=c.getString(R.string.msgs_media_store_adapter_file_size);
            holder.last_mod=c.getString(R.string.msgs_media_store_adapter_last_mod);
            holder.file_size_unmatch=c.getString(R.string.msgs_media_store_adapter_file_size_unmatch);
            holder.last_mod_unmatch=c.getString(R.string.msgs_media_store_adapter_last_mod_unmatch);
            holder.file_not_found=c.getString(R.string.msgs_media_store_adapter_file_not_found);
            
            v.setTag(holder); 
        } else {
     	   holder= (ViewHolder)v.getTag();
        }

        if (getItem(position) != null ) {
        	final int p = position;
        	
        	holder.tv_media.setText(
        			holder.last_mod+sdfDate.format(getItem(position).getDateModified())+
        			" "+sdfTime.format(getItem(position).getDateModified())+
        			holder.file_size+getItem(position).getMediaSize());
        	holder.tv_file.setText(
        			holder.last_mod+sdfDate.format(getItem(position).getFileModififed())+
        			" "+sdfTime.format(getItem(position).getFileModififed())+
        			holder.file_size+getItem(position).getFileSize());

    		if (getItem(p).isFileDifferent()) {
    			if (getItem(position).isFileLastModifiedDateDifferent()) {
        			holder.tv_hdr_file.setVisibility(TextView.VISIBLE);
        			holder.tv_file.setVisibility(TextView.VISIBLE);
    				holder.tv_diff.setText(holder.last_mod_unmatch);
    			} else if (getItem(position).isFileSizeDifferent()) {
        			holder.tv_hdr_file.setVisibility(TextView.VISIBLE);
        			holder.tv_file.setVisibility(TextView.VISIBLE);
    				holder.tv_diff.setText(holder.file_size_unmatch);
    			} else {
        			holder.tv_hdr_file.setVisibility(TextView.GONE);
        			holder.tv_file.setVisibility(TextView.GONE);
        			holder.tv_diff.setText(holder.file_not_found);
    			}
    		}
    		
    		if((getItem(position).getMediaType().startsWith("D"))) {
    			holder.tv_name.setVisibility(TextView.GONE);
    			holder.tv_hdr_name.setVisibility(TextView.GONE);
    		} else {
    			holder.tv_name.setVisibility(TextView.VISIBLE);
    			holder.tv_hdr_name.setVisibility(TextView.VISIBLE);
    		}
    		
        	holder.tv_name.setText(getItem(p).getDisplayName());
        	holder.tv_filepath.setText(getItem(p).getFilePath());
        	if (getItem(position).getMediaType().startsWith("I"))
        		holder.iv_cat.setImageResource(R.drawable.images);
        	else if((getItem(position).getMediaType().startsWith("A")))
        			holder.iv_cat.setImageResource(R.drawable.music);
        	else if((getItem(position).getMediaType().startsWith("V")))
    			holder.iv_cat.setImageResource(R.drawable.movies);
        	else if((getItem(position).getMediaType().startsWith("D")))
    			holder.iv_cat.setImageResource(R.drawable.sheet);
       	}

   		return v;
	};

	@SuppressWarnings("unused")
	private float toPixel(int dip) {
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				dip, resources.getDisplayMetrics());
		return px;
	};

	static class ViewHolder {
		 TextView tv_name,tv_filepath, tv_diff,tv_cat;
		 TextView tv_hdr_name,tv_hdr_filepath, tv_hdr_diff;
		 TextView tv_hdr_media, tv_media, tv_file, tv_hdr_file;
		 ImageView iv_cat;
		 LinearLayout ll_view;
		 String last_mod, file_size,last_mod_unmatch, file_size_unmatch,
		 		file_not_found;
	};
}
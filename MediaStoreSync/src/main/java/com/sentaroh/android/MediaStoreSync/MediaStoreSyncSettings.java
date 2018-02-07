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

import com.sentaroh.android.Utilities.LocalMountPoint;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class MediaStoreSyncSettings extends PreferenceActivity {
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
    	if (!LocalMountPoint.isExternalStorageAvailable()) {
    		findPreference(getString(R.string.settings_log_option).toString())
    			.setEnabled(false);
    		findPreference(getString(R.string.settings_log_dir).toString())
    			.setEnabled(false);
    	}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		initSettingParms(prefs,getString(R.string.settings_debug_msg_diplay));
		initSettingParms(prefs,getString(R.string.settings_log_option));
		initSettingParms(prefs,getString(R.string.settings_log_dir));
		initSettingParms(prefs,getString(R.string.settings_log_level));
		initSettingParms(prefs,getString(R.string.settings_exit_clean));
    	
	}

	private void initSettingParms(SharedPreferences prefs, String key) {
		if (!checkBasicSettings(prefs, key)) 
	    	if (!checkLogSettings(prefs, key))
		    	checkOtherSettings(prefs, key);
	}
		
		
	@SuppressWarnings("deprecation")
	@Override  
	protected void onResume() {  
	    super.onResume();  
	    getPreferenceScreen().getSharedPreferences()
	    	.registerOnSharedPreferenceChangeListener(listener);  
	}  
	   
	@SuppressWarnings("deprecation")
	@Override  
	protected void onPause() {  
	    super.onPause();  
	    getPreferenceScreen().getSharedPreferences()
	    	.unregisterOnSharedPreferenceChangeListener(listener);  
	}
	
	private SharedPreferences.OnSharedPreferenceChangeListener listener =   
		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
		    public void onSharedPreferenceChanged(SharedPreferences prefs, 
		    		String key) {
		    	if (!checkBasicSettings(prefs, key)) 
		    	if (!checkLogSettings(prefs, key))
			    	checkOtherSettings(prefs, key);
		    }
	};
	
	@SuppressWarnings("deprecation")
	private boolean checkBasicSettings(SharedPreferences prefs, String key) {
		boolean isChecked = false;
		if (key.equals(getString(R.string.settings_exit_clean))) {
    		isChecked=true;
    		if (prefs.getBoolean(key, false)) {
    			findPreference(key)
    				.setSummary(getString(R.string.settings_exit_clean_summary_ena));
    		} else {
    			findPreference(key)
    				.setSummary(getString(R.string.settings_exit_clean_summary_dis));
    		}
    	}
    	return isChecked;
	};
	@SuppressWarnings("deprecation")
	private boolean checkLogSettings(SharedPreferences prefs, String key) {
//		Log.v("","key="+key);
		boolean isChecked = false;
    	if (key.equals(getString(R.string.settings_debug_msg_diplay))) {
    		isChecked=true;
    		if (prefs.getBoolean(key, false)) {
    			findPreference(key)
    				.setSummary(getString(R.string.settings_debug_msg_diplay_summary_ena));
    		} else {
    			findPreference(key)
    			.setSummary(getString(R.string.settings_debug_msg_diplay_summary_dis));
    		}
    	} else if (key.equals(getString(R.string.settings_log_option))) {
    		isChecked=true;
    		if (prefs.getString(key, "0").equals("0")) {
				//option=0
				findPreference(key)
					.setSummary(getString(R.string.settings_log_option_summary_no));
			} else if (prefs.getString(key, "0").equals("1")) {
				//option=1
				findPreference(key)
					.setSummary(getString(R.string.settings_log_option_summary_repl));
			} else if (prefs.getString(key, "0").equals("2")) {
				//option=2
				findPreference(key)
					.setSummary(getString(R.string.settings_log_option_summary_append));
			}
    	} else if (key.equals(getString(R.string.settings_log_dir))) {
    		isChecked=true;
			findPreference(key)
				.setSummary(prefs.getString(key,
						Environment.getExternalStorageDirectory().toString()+
						"/MediaStoreSync/"));
    	}

    	return isChecked;
	};

	@SuppressWarnings("deprecation")
	private boolean checkOtherSettings(SharedPreferences prefs, String key) {
		boolean isChecked = true;
    	findPreference(key).setSummary(
	    		getString(R.string.settings_default_current_setting)+
	    		prefs.getString(key, "0"));    	
    	return isChecked;
	};
}

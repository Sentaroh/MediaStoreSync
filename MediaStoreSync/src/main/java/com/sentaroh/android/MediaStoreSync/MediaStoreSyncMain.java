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

import java.io.BufferedWriter;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.ThemeUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.SerializeUtil;
import com.sentaroh.android.Utilities.ThemeColorList;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenuItem.CustomContextMenuOnClickListener;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;
import com.sentaroh.android.Utilities.Widget.CustomTextView;
import com.sentaroh.android.Utilities.TreeFilelist.TreeFilelistAdapter;
import com.sentaroh.android.Utilities.TreeFilelist.TreeFilelistItem;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;

@SuppressLint("DefaultLocale")
public class MediaStoreSyncMain extends AppCompatActivity{ 

	private static final String DEBUG_TAG="MediaStoreSync";
	
	private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
	private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss",Locale.getDefault());
	private SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault());
	private PrintWriter logPrintWriter=null;
	private static final boolean DEBUG_ENABLE=true;
	private int debug_level=1;
	
	private boolean syncWasUseDeleteMethod=false;

	private ArrayList<MediaStoreListItem> mediaStoreSyncList=null;
	
	private ListView scanListview=null, msgListView=null;
	
	private MsglistAdapter msglistAdapter=null;
	
	private TabHost tabHost;
	private TabSpec tabSpec;
	private String currentTabName="";
	
	private CustomContextMenu ccMenu = null;
	
	private MediaScannerConnection mediaScanner;
	private boolean mediaScanCompleted=false;
	private String mediaScanLock="Lock";
	private TreeFilelistAdapter treeFilelistAdapter;
	private boolean result_createFileListView=true;

	private boolean terminateApplication=false;

	private boolean defaultSettingDebugMsgDisplay, defaultSettingExitClean;
	private String defaultSettingLogMsgDir="",defaultSettingLogOption="0",
		defaultSeiingLogMsgFilename="log.txt";
	private boolean defaultSettingScanDocFiles=true;
	private static int restartStatus=0;
	
	private Handler uiHandler=null;
	
	private String MediaStoreSyncRootDir="";
	private String currentLocalMountPoint;
	
	private Spinner scanSpinner;
	private CustomSpinnerAdapter spinnerAdapter;

//	private SharedPreferences prefWork=null;
//	private static final String SETTINGS_WORK_KEY="work";
	
	private CommonDialog commonDlg=null;
	private Context context;

	private ThemeColorList mThemeColorList;
	@Override  
	protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","onSaveInstanceState entered");
		outState.putString("currentTabName", currentTabName);
	};  
	  
	@Override  
	protected void onRestoreInstanceState(Bundle savedState) {  
	  super.onRestoreInstanceState(savedState);
	  if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","onRestoreInstanceState entered");
	  restartStatus=2;
	  currentTabName=savedState.getString("currentTabName");
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		prefWork=getSharedPreferences(SETTINGS_WORK_KEY,MODE_PRIVATE);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        uiHandler=new Handler();
        
        MediaStoreSyncRootDir =LocalMountPoint.getExternalStorageDir();
        currentLocalMountPoint=MediaStoreSyncRootDir;
        restartStatus=0;
        context=this;
        commonDlg=new CommonDialog(this, getSupportFragmentManager());
        
        initSettingParms();

        createTabView();

        deleteLogFile();
        openLogFile();
        if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","onCreate entered");
        
        if (ccMenu ==null) ccMenu = new CustomContextMenu(getResources(),getSupportFragmentManager());
        
    	scanListview=(ListView)findViewById(R.id.main_scan_listview);
    	
    	connectMediaScanner();
    	
    	createSpinner();
    };

	private String getApplVersionName() {
		String ver="";
	    String packegeName = getPackageName();
	    PackageInfo packageInfo;
		try {
			packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    ver=packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return ver;
	};

	@Override
	public void onStart() {
		super.onStart();
		if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","onStart entered");
	};

	@Override
	public void onRestart() {
		super.onStart();
		if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","onRestart entered");
	};
	
	@Override
	public void onResume() {
		super.onResume();
		if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","onResume entered");

		refreshOptionMenu();
		
		LinearLayout ll=(LinearLayout)findViewById(R.id.main_files_stat);
		if(Build.VERSION.SDK_INT >= 11) {
			ll.setVisibility(LinearLayout.VISIBLE);
		} else ll.setVisibility(LinearLayout.GONE);

		if (restartStatus==0) {
			buildMediaStoreStatList();
			if (LocalMountPoint.isExternalStorageAvailable()) {
				buildSdcardListView();
				
				setLocalFilelistItemClickListener();
				setLocalFilelistLongClickListener();
				setSpinnerListener();

				tabHost.getTabWidget().getChildTabViewAt(1).setEnabled(true);
			} else tabHost.getTabWidget().getChildTabViewAt(1).setEnabled(false);
			refreshOptionMenu();
			addLogMsg("I",getString(R.string.msgs_main_started));
		} else if (restartStatus==2) {
			restoreTaskData();
			buildMediaStoreStatList();
			refreshOptionMenu();
			scanListview.setAdapter(treeFilelistAdapter);
			addLogMsg("I",getString(R.string.msgs_main_restarted));
			
			setLocalFilelistItemClickListener();
			setLocalFilelistLongClickListener();
			setSpinnerListener();

			tabHost.setCurrentTabByTag(currentTabName);
		}
		restartStatus=1;
		deleteTaskData();
		
		setMsglistLongClickListener();
	};
	
	@Override
	public void onPause() {
		super.onPause();
		if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","onPause entered");
		saveTaskData();
	};

	@Override
	public void onStop() {
		super.onStop();
		if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","onStop entered");
		saveTaskData();
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","onDestroy entered");
		mediaScanner.disconnect();
		if (terminateApplication) {
			closeLogFile();
			msglistAdapter=null;
			terminateApplication=false;
			if (defaultSettingExitClean) {
				System.gc();
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		} else {
			saveTaskData();
			closeLogFile();
		}
	};
	
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    sendDebugLogMsg(1,"I","onConfigurationChanged Entered");
		if (Build.VERSION.SDK_INT >= 11){
		    invalidateOptionsMenu();
			sendDebugLogMsg(1,"I","refreshOptionMenu Entered");
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		sendDebugLogMsg(1,"I","onCreateOptionsMenu Entered");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_top, menu);
		return super.onCreateOptionsMenu(menu);
	};
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		sendDebugLogMsg(1,"I","onPrepareOptionsMenu Entered");
		if (mediaStoreSyncList!=null && mediaStoreSyncList.size()!=0) 
			menu.findItem(R.id.menu_top_scan).setEnabled(true);
		else menu.findItem(R.id.menu_top_scan).setEnabled(false);
		if (LocalMountPoint.isExternalStorageAvailable())
			menu.findItem(R.id.menu_top_scan).setEnabled(true);
		else menu.findItem(R.id.menu_top_scan).setEnabled(false);
		
        return super.onPrepareOptionsMenu(menu);
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_top_sync:
			final CheckedTextView ctv_use_delete=(CheckedTextView)findViewById(R.id.main_stat_ctv_use_delete);
			confirmSyncMediaStoreLibrary(mediaStoreSyncList, ctv_use_delete.isChecked());
			return true;
		case R.id.menu_top_scan:
//			scanSdcard(true,(String)scanSpinner.getSelectedItem());
			selectScanMountPoint();
			return true;
		case R.id.menu_top_browse_logfile:
			invokeLogFileBrowser();
			return true;
		case R.id.menu_top_refresh:
			buildMediaStoreStatList();
			buildSdcardListView();
			return true;
		case R.id.menu_top_about:
			aboutMediaStoreSync();
			return true;
		case R.id.menu_top_settings:
			invokeSettingsActivity();
			return true;			
//		case R.id.menu_top_quit:
//			finish();
//			return true;			
		}
		return false;
	};
	
	private void aboutMediaStoreSync() {
		commonDlg.showCommonDialog(false, "I",
				getString(R.string.app_name)+"Version "+getApplVersionName(), 
				getString(R.string.msgs_about_text), null);
	};
	private void refreshOptionMenu() {
		if (Build.VERSION.SDK_INT >= 11){
			invalidateOptionsMenu();
			sendDebugLogMsg(1,"I","refreshOptionMenu Entered");
		}
	};

    private void createSpinner() {
        scanSpinner = (Spinner) findViewById(R.id.main_scan_mount_point);
        spinnerAdapter = new CustomSpinnerAdapter(this, R.layout.custom_simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scanSpinner.setPrompt(getString(R.string.msgs_scan_spinner_title));
        scanSpinner.setAdapter(spinnerAdapter);
    };
    
	private void initSettingParms() {
		SharedPreferences prefs = 
				PreferenceManager.getDefaultSharedPreferences(this);
		defaultSettingLogMsgDir=
				prefs.getString(getString(R.string.settings_log_dir),"");
		if (defaultSettingLogMsgDir.equals("")) {
			prefs.edit().putString(getString(R.string.settings_log_dir),
					Environment.getExternalStorageDirectory().toString()+
					"/MediaStoreSync/").commit();
			defaultSettingLogMsgDir=
					prefs.getString(getString(R.string.settings_log_dir),"");
    	}
		debug_level=Integer.parseInt(
				prefs.getString(getString(R.string.settings_log_level),"0"));
		defaultSettingDebugMsgDisplay=
				prefs.getBoolean(getString(R.string.settings_debug_msg_diplay),false);
		defaultSettingLogOption=
				prefs.getString(getString(R.string.settings_log_option),"0");
		defaultSettingExitClean=
				prefs.getBoolean(getString(R.string.settings_exit_clean),false);

	};

	private void applySettingParms() {
		SharedPreferences prefs = 
				PreferenceManager.getDefaultSharedPreferences(this);
		String dl = 
				prefs.getString(getString(R.string.settings_log_level), "0");
		if (dl.compareTo("0")>=0 && dl.compareTo("9")<=0) 
			debug_level=Integer.parseInt(dl);
		
		String t_dir =
				prefs.getString(getString(R.string.settings_log_dir),
						MediaStoreSyncRootDir+"/MediaStoreSync/");
		defaultSettingLogMsgDir=t_dir;
		
		defaultSettingDebugMsgDisplay=
				prefs.getBoolean(getString(R.string.settings_debug_msg_diplay), false);
		
		defaultSettingLogOption=prefs.getString(getString(R.string.settings_log_option), "0");
		
		defaultSettingExitClean=
				prefs.getBoolean(getString(R.string.settings_exit_clean),false);
	};

	private int mRequestCodeFileBrowser=1;
	private int mRequestCodeSettings=0;
	private void invokeLogFileBrowser() {
		if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","Invoke log file browser.");
		if (logPrintWriter!=null) {

			closeLogFile();
			
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setDataAndType(Uri.parse("file://"+
					defaultSettingLogMsgDir+defaultSeiingLogMsgFilename),
					"text/plain");
			startActivityForResult(intent,mRequestCodeFileBrowser);
		}
	};
	
	private void invokeSettingsActivity() {
		if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","Invoke Settings.");
		closeLogFile();
		Intent intent = new Intent(this, MediaStoreSyncSettings.class);
		startActivityForResult(intent,mRequestCodeSettings);
	};
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!defaultSettingLogOption.equals("0")) openLogFile();
		if (requestCode==mRequestCodeSettings) {
			if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","Return from Settings.");
			applySettingParms();
		} else if (requestCode==mRequestCodeFileBrowser) {
			if (DEBUG_ENABLE) sendDebugLogMsg(1,"I","Return from browse log file.");
		}
	};

	private CheckedTextView mCtvUseDelete=null;
	private TabWidget mTabWidget;
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void createTabView() {
		mThemeColorList=ThemeUtil.getThemeColorList(this);
		
		tabHost=(TabHost)findViewById(android.R.id.tabhost);
		tabHost.setup();

		mTabWidget = (TabWidget) findViewById(android.R.id.tabs);
		 
		if (Build.VERSION.SDK_INT>=11) {
		    mTabWidget.setStripEnabled(false);  
		    mTabWidget.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);  
		}

		View childview2 = new CustomTabContentView(this,
				getString(R.string.msgs_main_tab_name_main));
		tabSpec=tabHost.newTabSpec("stat")
				.setIndicator(childview2).setContent(R.id.main_statistics_tab);
		tabHost.addTab(tabSpec);
		
		View childview4 = new CustomTabContentView(this,
				getString(R.string.msgs_main_tab_name_scan));
			tabSpec=tabHost.newTabSpec("scan")
					.setIndicator(childview4).setContent(R.id.main_scan_tab);
			tabHost.addTab(tabSpec);
			
		View childview5 = new CustomTabContentView(this,
				getString(R.string.msgs_main_tab_name_message));
		tabSpec=tabHost.newTabSpec("message")
				.setIndicator(childview5).setContent(R.id.main_message_tab);
		tabHost.addTab(tabSpec);

//		if (isFirstStart) tabHost.setCurrentTab(0);
		tabHost.setOnTabChangedListener(new OnTabChange());
		
        msgListView=(ListView)findViewById(R.id.main_message_listview);
        if (msglistAdapter==null) {
        	msglistAdapter=new MsglistAdapter(this,this,
        		R.layout.msg_list_view_item,new ArrayList<MsglistItem>());
        }
        msgListView.setAdapter(msglistAdapter);
        msgListView.setFastScrollEnabled(true);
        
        mCtvUseDelete=(CheckedTextView)findViewById(R.id.main_stat_ctv_use_delete);
        mCtvUseDelete.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				((CheckedTextView)v).toggle();
			}
        });
	};
	
	class OnTabChange implements OnTabChangeListener {
		@Override
		public void onTabChanged(String tabId){
			if (DEBUG_ENABLE) 
				sendDebugLogMsg(1,"I","onTabchanged entered. tab="+tabId);
			currentTabName=tabId;
		};
	};

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			confirmTerminateApplication();
			return true;
			// break;
		default:
			return super.onKeyDown(keyCode, event);
			// break;
		}
	};

	private void confirmTerminateApplication() {
		NotifyEvent ntfy=new NotifyEvent(this);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {}

			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				addLogMsg("I",getString(R.string.msgs_main_termination));
				terminateApplication=true;
				finish();
			}
			
		});
		
		commonDlg.showCommonDialog(true,"W",getString(R.string.msgs_main_terminate_appl),"",ntfy);
	}
	
	private void connectMediaScanner() {
		mediaScanner = new MediaScannerConnection(this,
				new MediaScannerConnectionClient() {
			@Override
			public void onMediaScannerConnected() {
				if (DEBUG_ENABLE) 
					sendDebugLogMsg(2,"I","MediaScanner connected.");
			};

			@Override
			public void onScanCompleted(String path, Uri uri) {
//				getMediaStoreInfo(uri);
				if (DEBUG_ENABLE) 
					sendDebugLogMsg(2,"I","MediaScanner scan completed. fn="+
							path+", Uri="+uri);
				synchronized(mediaScanLock) {
					mediaScanCompleted=true;
				}
			};
		});
		mediaScanner.connect();
	};
	
	private void setMsglistLongClickListener() {
		msgListView
			.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				createMsglistContextMenu(arg1, arg2);
				return true;
			}
		});
	};

	private void createMsglistContextMenu(View view, int idx) {

		ccMenu.addMenuItem(getString(R.string.msgs_main_move_to_top),
				R.drawable.menu_top)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					msgListView.setSelection(0);
				}
		});
		
		ccMenu.addMenuItem(getString(R.string.msgs_main_move_to_bottom),
				R.drawable.menu_bottom)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					msgListView.setSelection(msgListView.getCount());
				}
		});

		ccMenu.addMenuItem(getString(R.string.msgs_main_clear_log_message),
				R.drawable.menu_clear)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					msgListView.setSelection(0);
					msglistAdapter.clear();
					addLogMsg("W",getString(R.string.msgs_main_log_msg_cleared));
				}
		});

		ccMenu.createMenu();
	};

	
	private void buildSdcardListView() {

        int a_no=0;
        ArrayList<String>ml=LocalMountPoint.getLocalMountPointList(context);
        spinnerAdapter.clear();
        for (int i=0;i<ml.size();i++) { 
				spinnerAdapter.add(ml.get(i));
				if (ml.get(i).equals(currentLocalMountPoint))
			        scanSpinner.setSelection(a_no);
				a_no++;
		}

		TreeFilelistAdapter tadpt = createLocalFileList(currentLocalMountPoint);
		if (!result_createFileListView) return;
		treeFilelistAdapter=tadpt;
		scanListview.setAdapter(treeFilelistAdapter);
	};

	private void setSpinnerListener() {
        scanSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                Spinner spinner = (Spinner) parent;
				String turl=(String) spinner.getSelectedItem();
				currentLocalMountPoint=turl;
				TreeFilelistAdapter tadpt =createLocalFileList(turl);
				if (!result_createFileListView) return;
				treeFilelistAdapter=tadpt;
				scanListview.setAdapter(treeFilelistAdapter);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

    };
	
	private void setLocalFilelistItemClickListener() {
		if (scanListview==null) return;
		scanListview
		.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				int pos=treeFilelistAdapter.getItem(position);
				TreeFilelistItem item = treeFilelistAdapter.getDataItem(pos);
//				Log.v("","name="+item.getName()+", dir="+item.isDir()+", sub="+item.getSubDirItemCount()+",　lurl="+currentLocalMountPoint);
				if (item.isDir()) {
					if (item.getSubDirItemCount()==0) {
//						if (item.isChecked()) item.setChecked(false);
//						else item.setChecked(true);
//						treeFilelistAdapter.replaceDataItem(pos, item);
						return;
					}
//					if (hasNoMediaFile(lurl+item.getPath()+item.getName())) 
//						return;
					if(item.isChildListExpanded()) {
//						Log.v("","exp");
						int lv_fpos=scanListview.getFirstVisiblePosition();
						treeFilelistAdapter.hideChildItem(item,pos);
						scanListview.setSelection(lv_fpos);
					} else {
//						Log.v("","!exp1 load="+item.isSubDirLoaded()+", error="+result_createFileListView);
						if (item.isSubDirLoaded()) 
							treeFilelistAdapter.reshowChildItem(item,pos);
						else {
							TreeFilelistAdapter temp=
								createLocalFileList(currentLocalMountPoint+item.getPath()+item.getName());
//							Log.v("","!exp2 load="+item.isSubDirLoaded()+", error="+result_createFileListView);
							if (!result_createFileListView) return;
							treeFilelistAdapter.addChildItem(item,temp,pos);
						}
					}
				} else {
					if (item.isChecked()) item.setChecked(false);
					else item.setChecked(true);
//					treeFilelistAdapter.replaceDataItem(pos, item);
				}
			}
		});
	};
	
	private void setLocalFilelistLongClickListener() {
		if (scanListview==null) return;
		scanListview.setOnItemLongClickListener(
				new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				int idx = treeFilelistAdapter.getItem(arg2);
				createFilelistContextMenu(arg1, idx,treeFilelistAdapter);
				return true;
			}
		});
	};

	private void createFilelistContextMenu(View view, int idx, 
			TreeFilelistAdapter fla) {
		TreeFilelistItem item ;
		int j=0;
		for (int i=0;i<fla.getDataItemCount();i++) {
			item = fla.getDataItem(i);
			if (item.isChecked()) {
				j++; 
			}
		}
		if (j<=1) {
			for (int i=0;i<fla.getDataItemCount();i++) {
				item = fla.getDataItem(i);
				if (item.isEnableItem()) {
					if (idx==i) {
						item.setChecked(true);
//						fla.replaceDataItem(i,item);
						j=i;//set new index no
					} else {
						if (item.isChecked()) {
							item.setChecked(false);
//							fla.replaceDataItem(i,item);
						}
					}
				}
			}
			fla.notifyDataSetChanged();
			createFilelistContextMenu_Single(view,idx,j,fla) ;
		} else createFilelistContextMenu_Multiple(view, idx,fla) ;
		
	};

	private void createFilelistContextMenu_Multiple(View view, int idx,
			final TreeFilelistAdapter fla) {

		ccMenu.addMenuItem(getString(R.string.msgs_context_menu_scan),
				R.drawable.media_scan).setOnClickListener(
				new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  scanSdcard(false,(String)scanSpinner.getSelectedItem());
		  	}
	  	});
		ccMenu.addMenuItem(getString(R.string.msgs_context_menu_select_all, R.drawable.select_all),
				R.drawable.blank).setOnClickListener(
				new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setAllFilelistItemChecked(fla);
		  	}
	  	});
		ccMenu.addMenuItem(getString(R.string.msgs_context_menu_unselect_all, R.drawable.unselect_all),
				R.drawable.blank).setOnClickListener(
				new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				setAllFilelistItemUnChecked(fla);
		  	}
	  	});
		ccMenu.createMenu();
	};
	
	private void createFilelistContextMenu_Single(View view, int idx, int cin,
			final TreeFilelistAdapter fla) {

		ccMenu.addMenuItem(getString(R.string.msgs_context_menu_scan),
				R.drawable.media_scan).setOnClickListener(
				new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  scanSdcard(false,(String)scanSpinner.getSelectedItem());
		  	}
	  	});
		ccMenu.addMenuItem(getString(R.string.msgs_context_menu_select_all, R.drawable.select_all),
				R.drawable.blank).setOnClickListener(
				new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
			  setAllFilelistItemChecked(fla);
		  	}
	  	});
		ccMenu.addMenuItem(getString(R.string.msgs_context_menu_unselect_all, R.drawable.unselect_all),
				R.drawable.blank).setOnClickListener(
				new CustomContextMenuOnClickListener() {
		  @Override
		  public void onClick(CharSequence menuTitle) {
				setAllFilelistItemUnChecked(fla);
		  	}
	  	});
		ccMenu.createMenu();
	};
	
	private void setAllFilelistItemUnChecked(TreeFilelistAdapter fla) {
		TreeFilelistItem item;
		for (int i=0;i<fla.getDataItemCount();i++) {
			if (fla.getDataItem(i).isChecked()) { 
				item=fla.getDataItem(i);
				item.setChecked(false);
//				fla.replaceDataItem(i,item);
			}
		}
		fla.notifyDataSetChanged();
	};

	private void setAllFilelistItemChecked(TreeFilelistAdapter fla) {
		TreeFilelistItem item;
		for (int i=0;i<fla.getDataItemCount();i++) {
			item=fla.getDataItem(i);
			if (item.isEnableItem()) {
				item.setChecked(true);
//				fla.replaceDataItem(i,item);
			}
		}
		fla.notifyDataSetChanged();
	};

	@SuppressLint("SimpleDateFormat")
	private TreeFilelistAdapter createLocalFileList(String url) {
//		Log.v("","url="+url);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		TreeFilelistAdapter filelist;
		result_createFileListView=true;
		ArrayList<TreeFilelistItem> dir = new ArrayList<TreeFilelistItem>();
		List<TreeFilelistItem> fls = new ArrayList<TreeFilelistItem>();
		File sf = new File(url);
		// File List
		File[] dirs = sf.listFiles();
		if (dirs!=null) {
			try {
				for (File ff : dirs) {
					if (ff.canRead()) {
						String tfs=convertFileSize(ff.length());
						String curr_dir="";
//						curr_dir=ff.getParent().replace(MediaStoreSyncRootDir, "")+"/";
						curr_dir=ff.getParent().replace(currentLocalMountPoint, "")+"/";
//						Log.v("","cdir="+curr_dir+", parent="+ff.getParent()+", clmp="+currentLocalMountPoint);
						
						if (ff.isDirectory()) {
							if (
//									!hasNoMediaFile(url+"/"+ff.getName()) &&
//									!ff.getName().startsWith(".") &&
									!ff.getName().equals("LOST.DIR")) {
								File tlf=new File(url+"/"+ff.getName());
								String[] tfl=tlf.list();
								TreeFilelistItem tfi=
										new TreeFilelistItem(ff.getName(),
										sdf.format(ff.lastModified())+", ",
										true, 0,0,false,ff.canRead(),ff.canWrite(),
										ff.isHidden(),curr_dir,0);
								if (tfl!=null) tfi.setSubDirItemCount(tfl.length);
								else tfi.setSubDirItemCount(0);
//								if (ff.getName().startsWith(".") || hasNoMediaFile(url+"/"+ff.getName()))
//									tfi.setEnableItem(false);
								dir.add(tfi);
							}
						} else {
							fls.add(new TreeFilelistItem(ff.getName(), 
									sdf.format(ff.lastModified())+","+tfs,
									false, ff.length(), ff.lastModified(),false,
									ff.canRead(),ff.canWrite(),
									ff.isHidden(),curr_dir,0));
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				commonDlg.showCommonDialog(false,"E",getString(R.string.msgs_create_local_filelist_create_error),
						e.getMessage(),null);
				result_createFileListView=false;
				return null;
			}
		}
		Collections.sort(dir);
		Collections.sort(fls);
		dir.addAll(fls);

		filelist = new TreeFilelistAdapter(this);
		filelist.setDataList(dir);
		return filelist;
	};

	private static String convertFileSize(long fs) {
	    String tfs="";
		if (fs>(1024*1024*1024)) {//GB
		    BigDecimal dfs1 = new BigDecimal(fs);
		    BigDecimal dfs2 = new BigDecimal(1024*1024*1024);
		    BigDecimal dfs3 = new BigDecimal("0.00");
		    dfs3=dfs1.divide(dfs2,1, BigDecimal.ROUND_HALF_UP);
			tfs=dfs3+" GBytes";
		} else if (fs>(1024*1024)) {//MB
		    BigDecimal dfs1 = new BigDecimal(fs*1.00);
		    BigDecimal dfs2 = new BigDecimal(1024*1024*1.00);
		    BigDecimal dfs3 = new BigDecimal("0.00");
		    dfs3=dfs1.divide(dfs2,1, BigDecimal.ROUND_HALF_UP);
			tfs=dfs3+" MBytes";
		} else if (fs>(1024)) {//KB
		    BigDecimal dfs1 = new BigDecimal(fs);
		    BigDecimal dfs2 = new BigDecimal(1024);
		    BigDecimal dfs3 = new BigDecimal("0.00");
		    dfs3=dfs1.divide(dfs2,1, BigDecimal.ROUND_HALF_UP);
			tfs=dfs3+" KBytes";
		} else tfs=""+fs+" Bytes";
		return tfs;
	};

	private static boolean hasNoMediaFile(String local_dir) {
		boolean result=false;
		File lf = new File(local_dir) ;
		File[] fl= lf.listFiles();
		if (fl==null) return false;
		for (File cl:fl) {
			if (cl.isFile()) 
				if (cl.getName().equalsIgnoreCase(".nomedia")) {
					result=true;
					break;
				}
		}
		return result;
	};

	private ArrayList<MediaStoreListItem> mediaStoreListImage = null;
	private ArrayList<MediaStoreListItem> mediaStoreListAudio = null;
	private ArrayList<MediaStoreListItem> mediaStoreListVideo = null;
	private ArrayList<MediaStoreListItem> mediaStoreListFiles = null; 

    private void buildMediaStoreStatList() {
    	sendDebugLogMsg(1,"I","BuildMediaStoreStatList entered");
		final ThreadCtrl threadCtl=new ThreadCtrl();
		threadCtl.initThreadCtrl();
		threadCtl.setEnabled();//enableAsyncTask();
		
//		commonDlg.setFixedOrientation(true);
       	
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.progress_spin_dlg);
		final TextView title = (TextView)dialog.findViewById(R.id.progress_spin_dlg_title);
		title.setText(getString(R.string.msgs_media_store_stats_spin_title));
		final Button btnCancel = (Button) dialog.findViewById(R.id.progress_spin_dlg_btn_cancel);
		CommonDialog.setDlgBoxSizeCompact(dialog);
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnCancel.setText(getString(R.string.msgs_local_file_scan_cancelling));
				btnCancel.setEnabled(false);
				threadCtl.setDisabled();//disableAsyncTask();
				threadCtl.setThreadResultCancelled();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		dialog.setCancelable(false);
       	dialog.show();
		
		NotifyEvent ntfy=new NotifyEvent(this);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
//				commonDlg.setFixedOrientation(false);
				setMediaStoreStatistics();
				refreshOptionMenu();
				sendDebugLogMsg(1,"I","BuildMediaStoreStatList positive response received");
			}

			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {
				commonDlg.showCommonDialog(false, "W",
						getString(R.string.msgs_media_store_stats_cancelled),
						"", null);
				setMediaStoreStatistics();
				refreshOptionMenu();

//				commonDlg.setFixedOrientation(false);
				sendDebugLogMsg(1,"I","BuildMediaStoreStatList negative response received");
			}
			
		});
		startGetMediaStoreStatThread(dialog,threadCtl, ntfy);
    };
    
	private void setMediaStoreStatistics() {
	        setMediaStoreImageStatText(mediaStoreListImage);
	        setMediaStoreAudioStatText(mediaStoreListAudio);
	        setMediaStoreVideoStatText(mediaStoreListVideo);
	        if(Build.VERSION.SDK_INT >= 13) 
	        	setMediaStoreDocStatText(mediaStoreListFiles);
	        
	        mediaStoreSyncList=new ArrayList<MediaStoreListItem>();
	        
	        for (int i=0;i<mediaStoreListImage.size();i++)
	        	if (mediaStoreListImage.get(i).isFileDifferent()) 
	        		mediaStoreSyncList.add(mediaStoreListImage.get(i));
	        for (int i=0;i<mediaStoreListAudio.size();i++)
	        	if (mediaStoreListAudio.get(i).isFileDifferent()) 
	        		mediaStoreSyncList.add(mediaStoreListAudio.get(i));
	        for (int i=0;i<mediaStoreListVideo.size();i++)
	        	if (mediaStoreListVideo.get(i).isFileDifferent()) 
	        		mediaStoreSyncList.add(mediaStoreListVideo.get(i));
	        if(Build.VERSION.SDK_INT >= 13)  {
		        for (int i=0;i<mediaStoreListFiles.size();i++)
		        	if (mediaStoreListFiles.get(i).isFileDifferent()) 
		        		mediaStoreSyncList.add(mediaStoreListFiles.get(i));
	        }
	        Collections.sort(mediaStoreSyncList, 
	        		new Comparator<MediaStoreListItem>() {
				@Override
				public int compare(MediaStoreListItem lhs,
						MediaStoreListItem rhs) {
					String l_key=lhs.getMediaType().substring(0,1)+lhs.getFilePath();
					String r_key=rhs.getMediaType().substring(0,1)+rhs.getFilePath();
					return l_key.compareToIgnoreCase(r_key);
				}
	        });
			setImageDetailListener();
			setAudioDetailListener();
			setVideoDetailListener();
			setDocDetailListener();
	};

	private void startGetMediaStoreStatThread(final Dialog dialog, 
			final ThreadCtrl threadCtl, 
			final NotifyEvent at_ne) {
		numberOfFileScanSuccessfull=numberOfFileScanFailed=0;
       	new Thread(new Runnable() {
			@Override
			public void run() {//non UI thread
				String msg_txt=getString(R.string.msgs_media_store_stat_create_images);
				setDlgMsgText(dialog,"I",msg_txt);
				mediaStoreListImage=createMediaStoreImageList(threadCtl,dialog, msg_txt);
				msg_txt=getString(R.string.msgs_media_store_stat_create_images_comp)+"\n"+
						getString(R.string.msgs_media_store_stat_create_audio);
				setDlgMsgText(dialog,"I",msg_txt);
				mediaStoreListAudio=createMediaStoreAudioList(threadCtl,dialog, msg_txt);
				msg_txt=getString(R.string.msgs_media_store_stat_create_images_comp)+"\n"+
						getString(R.string.msgs_media_store_stat_create_audio_comp)+"\n"+
						getString(R.string.msgs_media_store_stat_create_video);
				setDlgMsgText(dialog,"I",msg_txt);
				mediaStoreListVideo=createMediaStoreVideoList(threadCtl, dialog, msg_txt);
		        if(Build.VERSION.SDK_INT >= 11) {
		        	msg_txt=getString(R.string.msgs_media_store_stat_create_images_comp)+"\n"+
							getString(R.string.msgs_media_store_stat_create_audio_comp)+"\n"+
							getString(R.string.msgs_media_store_stat_create_video_comp)+"\n"+								
							getString(R.string.msgs_media_store_stat_create_files);
					setDlgMsgText(dialog,"I",msg_txt);
		        	mediaStoreListFiles=createMediaStoreDocList(threadCtl, dialog, msg_txt);
		        }
				// dismiss progress bar dialog
				uiHandler.post(new Runnable() {// UI thread
					@Override
					public void run() {
						if (dialog!=null) dialog.dismiss();
						if(threadCtl.isEnabled())at_ne.notifyToListener(true, null);
						else at_ne.notifyToListener(false, null);
					}
				});
			}
		})
       	.start();
	};

    private void setMediaStoreImageStatText(ArrayList<MediaStoreListItem> mll) {
    	int  d_f_s=0, d_f_m=0, d_f_n=0;
    	for (int i=0;i<mll.size();i++) 
    		if (mll.get(i).isFileDifferent()) {
    			if (mll.get(i).isFileNotExist()) d_f_n++;
    			else {
    				if (mll.get(i).isFileLastModifiedDateDifferent()) d_f_m++;
    				if (mll.get(i).isFileSizeDifferent()) d_f_s++;
    			}
    		}
    	TextView t_cat=(TextView)findViewById(R.id.main_image_stat_cat);
    	TextView t_lmod=(TextView)findViewById(R.id.main_image_stat_lastmod_unmatch);
    	TextView t_nfd=(TextView)findViewById(R.id.main_image_stat_notfound);
    	TextView t_size=(TextView)findViewById(R.id.main_image_stat_size_unmatch);
    	TextView t_tot=(TextView)findViewById(R.id.main_image_stat_total);
    	
    	t_cat.setText(getString(R.string.msgs_media_store_category_image));
    	t_lmod.setText(""+d_f_m);
    	t_nfd.setText(""+d_f_n);
    	t_size.setText(""+d_f_s);
    	t_tot.setText(""+mll.size());
    	
    	return ;
    };

    private void setMediaStoreAudioStatText(ArrayList<MediaStoreListItem> mll) {
    	int d_f_s=0, d_f_m=0, d_f_n=0;
    	for (int i=0;i<mll.size();i++) 
    		if (mll.get(i).isFileDifferent()) {
    			if (mll.get(i).isFileNotExist()) d_f_n++;
    			else {
    				if (mll.get(i).isFileLastModifiedDateDifferent()) d_f_m++;
    				if (mll.get(i).isFileSizeDifferent()) d_f_s++;
    			}
    		}
    	TextView t_cat=(TextView)findViewById(R.id.main_audio_stat_cat);
    	TextView t_lmod=(TextView)findViewById(R.id.main_audio_stat_lastmod_unmatch);
    	TextView t_nfd=(TextView)findViewById(R.id.main_audio_stat_notfound);
    	TextView t_size=(TextView)findViewById(R.id.main_audio_stat_size_unmatch);
    	TextView t_tot=(TextView)findViewById(R.id.main_audio_stat_total);
    	
    	t_cat.setText(getString(R.string.msgs_media_store_category_audio));
    	t_lmod.setText(""+d_f_m);
    	t_nfd.setText(""+d_f_n);
    	t_size.setText(""+d_f_s);
    	t_tot.setText(""+mll.size());

    	return ;
    }

    private void setMediaStoreVideoStatText(ArrayList<MediaStoreListItem> mll) {
    	int d_f_s=0, d_f_m=0, d_f_n=0;
    	for (int i=0;i<mll.size();i++) 
    		if (mll.get(i).isFileDifferent()) {
    			if (mll.get(i).isFileNotExist()) d_f_n++;
    			else {
    				if (mll.get(i).isFileLastModifiedDateDifferent()) d_f_m++;
    				if (mll.get(i).isFileSizeDifferent()) d_f_s++;
    			}
    		}
    	TextView t_cat=(TextView)findViewById(R.id.main_video_stat_cat);
    	TextView t_lmod=(TextView)findViewById(R.id.main_video_stat_lastmod_unmatch);
    	TextView t_nfd=(TextView)findViewById(R.id.main_video_stat_notfound);
    	TextView t_size=(TextView)findViewById(R.id.main_video_stat_size_unmatch);
    	TextView t_tot=(TextView)findViewById(R.id.main_video_stat_total);
    	
    	t_cat.setText(getString(R.string.msgs_media_store_category_video));
    	t_lmod.setText(""+d_f_m);
    	t_nfd.setText(""+d_f_n);
    	t_size.setText(""+d_f_s);
    	t_tot.setText(""+mll.size());

    	return ;
    };

    private void setMediaStoreDocStatText(ArrayList<MediaStoreListItem> mll) {
    	int d_f_s=0, d_f_m=0, d_f_n=0;
    	for (int i=0;i<mll.size();i++) 
    		if (mll.get(i).isFileDifferent()) {
    			if (mll.get(i).isFileNotExist()) d_f_n++;
    			else {
    				if (mll.get(i).isFileLastModifiedDateDifferent()) d_f_m++;
    				if (mll.get(i).isFileSizeDifferent()) d_f_s++;
    			}
    		}
    	TextView t_cat=(TextView)findViewById(R.id.main_files_stat_cat);
    	TextView t_lmod=(TextView)findViewById(R.id.main_files_stat_lastmod_unmatch);
    	TextView t_nfd=(TextView)findViewById(R.id.main_files_stat_notfound);
    	TextView t_size=(TextView)findViewById(R.id.main_files_stat_size_unmatch);
    	TextView t_tot=(TextView)findViewById(R.id.main_files_stat_total);
    	
    	t_cat.setText(getString(R.string.msgs_media_store_category_files));
    	t_lmod.setText(""+d_f_m);
    	t_nfd.setText(""+d_f_n);
    	t_size.setText(""+d_f_s);
    	t_tot.setText(""+mll.size());
    	
    	return ;
    };

    private void setImageDetailListener() {
		final ArrayList<MediaStoreListItem> mll=new ArrayList<MediaStoreListItem>();
		for (int i=0;i<mediaStoreSyncList.size();i++) {
			if (mediaStoreSyncList.get(i).getMediaType().startsWith("I"))
				mll.add(mediaStoreSyncList.get(i));
		}
    	Button show_btn=(Button)findViewById(R.id.main_image_stat_detail);
    	Button sync_btn=(Button)findViewById(R.id.main_image_sync_detail);

		if (mll.size()!=0) {
			show_btn.setEnabled(true);
			sync_btn.setEnabled(true);
			show_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showMediaStoreDetail(
							getString(R.string.msgs_media_store_detail_title_image),mll);
				}
	    	});
			
			sync_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					confirmSyncMediaStoreLibrary(mll, mCtvUseDelete.isChecked());
				}
	    	});

		} else {
			show_btn.setEnabled(false); 
			sync_btn.setEnabled(false);
		}
    };
    
    private void setAudioDetailListener() {
		final ArrayList<MediaStoreListItem> mll=new ArrayList<MediaStoreListItem>();
		for (int i=0;i<mediaStoreSyncList.size();i++) {
			if (mediaStoreSyncList.get(i).getMediaType().startsWith("A"))
				mll.add(mediaStoreSyncList.get(i));
		}
    	Button show_btn=(Button)findViewById(R.id.main_audio_stat_detail);
    	Button sync_btn=(Button)findViewById(R.id.main_audio_sync_detail);

		if (mll.size()!=0) {
			show_btn.setEnabled(true);
			sync_btn.setEnabled(true);
			show_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showMediaStoreDetail(
							getString(R.string.msgs_media_store_detail_title_audio),mll);
				}
	    	});
			sync_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					confirmSyncMediaStoreLibrary(mll, mCtvUseDelete.isChecked());
				}
	    	});
		} else {
			show_btn.setEnabled(false); 
			sync_btn.setEnabled(false);
		}
    };

    private void setVideoDetailListener() {
		final ArrayList<MediaStoreListItem> mll=new ArrayList<MediaStoreListItem>();
		for (int i=0;i<mediaStoreSyncList.size();i++) {
			if (mediaStoreSyncList.get(i).getMediaType().startsWith("V"))
				mll.add(mediaStoreSyncList.get(i));
		}
    	Button show_btn=(Button)findViewById(R.id.main_video_stat_detail);
    	Button sync_btn=(Button)findViewById(R.id.main_video_sync_detail);

		if (mll.size()!=0) {
			show_btn.setEnabled(true);
			sync_btn.setEnabled(true);
			show_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showMediaStoreDetail(
							getString(R.string.msgs_media_store_detail_title_video),mll);
				}
	    	});
			sync_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					confirmSyncMediaStoreLibrary(mll, mCtvUseDelete.isChecked());
				}
	    	});

		} else {
			show_btn.setEnabled(false);
			sync_btn.setEnabled(false);
		}
    };

    private void setDocDetailListener() {
		final ArrayList<MediaStoreListItem> mll=new ArrayList<MediaStoreListItem>();
		for (int i=0;i<mediaStoreSyncList.size();i++) {
			if (mediaStoreSyncList.get(i).getMediaType().startsWith("D"))
				mll.add(mediaStoreSyncList.get(i));
		}
    	Button show_btn=(Button)findViewById(R.id.main_files_stat_detail);
    	Button sync_btn=(Button)findViewById(R.id.main_files_sync_detail);

		if (mll.size()!=0) {
			show_btn.setEnabled(true);
			sync_btn.setEnabled(true);
			show_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showMediaStoreDetail(
						getString(R.string.msgs_media_store_detail_title_files),mll);
				}
	    	});
			sync_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					confirmSyncMediaStoreLibrary(mll, mCtvUseDelete.isChecked());
				}
	    	});

		} else {
			show_btn.setEnabled(false);
			sync_btn.setEnabled(false);
		}
    };

    private void showMediaStoreDetail(String ms_title,
    		ArrayList<MediaStoreListItem> mll) {
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.file_select_list_dlg);
		((TextView) dialog.findViewById(R.id.file_select_list_dlg_title))
			.setText(ms_title);
		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.file_select_list_dlg_msg);
		dlg_msg.setVisibility(TextView.GONE);
		
		final TextView subtitle=
				(TextView) dialog.findViewById(R.id.file_select_list_dlg_subtitle);
		subtitle.setVisibility(TextView.GONE);
//		final TextView v_spacer=(TextView)dialog.findViewById(R.id.file_select_list_dlg_spacer);
		final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
//        if (dirs.size()<=2)	v_spacer.setVisibility(TextView.VISIBLE);
		
        final MediaStoreListAdapter mla= 
        		new MediaStoreListAdapter(this);
        mla.setArrayList(mll);
        lv.setAdapter(mla);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);
        lv.setFastScrollEnabled(true);

        CommonDialog.setDlgBoxSizeLimit(dialog, false);
        
        final Button ok_btn=(Button)dialog.findViewById(R.id.file_select_list_dlg_ok_btn);
        Button cancel_btn=(Button)dialog.findViewById(R.id.file_select_list_dlg_cancel_btn);
        ok_btn.setVisibility(Button.VISIBLE);
        cancel_btn.setVisibility(Button.GONE);
        
        ok_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				commonDlg.setFixedOrientation(false);
				dialog.dismiss();
			}
        });
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				ok_btn.performClick();
			}
		});
//        commonDlg.setFixedOrientation(true);
//        dialog.setCancelable(false);
        dialog.show();
    };
    
    private void selectScanMountPoint() {
    	ArrayList<String> mpl=new ArrayList<String>();
    	if (MediaStoreSyncRootDir.equals("/sdcard")) {
    		mpl.add("/sdcard");
    	} else {
    		mpl.addAll(LocalMountPoint.getLocalMountPointList(context));
    	}

		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.file_select_list_dlg);
		((TextView) dialog.findViewById(R.id.file_select_list_dlg_title))
			.setText(getString(R.string.msgs_scan_spinner_select_mp));
		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.file_select_list_dlg_msg);
		dlg_msg.setVisibility(TextView.VISIBLE);
		
		final TextView subtitle=
				(TextView) dialog.findViewById(R.id.file_select_list_dlg_subtitle);
		subtitle.setVisibility(TextView.GONE);
//		final TextView v_spacer=(TextView)dialog.findViewById(R.id.file_select_list_dlg_spacer);
		final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
//        if (dirs.size()<=2)	v_spacer.setVisibility(TextView.VISIBLE);
		
        final TreeFilelistAdapter tfla= new TreeFilelistAdapter(this);
        for (int i=0;i<mpl.size();i++) {
        	TreeFilelistItem tfli = new TreeFilelistItem(mpl.get(i),
        			"",true,0,0,false,true,true,false,"",0);
        	tfla.addDataItem(tfli);
        }
        CommonDialog.setDlgBoxSizeLimit(dialog, true);
        tfla.createShowList();
        lv.setAdapter(tfla);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);
        lv.setFastScrollEnabled(true);
        
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				int pos=tfla.getItem(arg2);
				TreeFilelistItem item = tfla.getDataItem(pos);
				if (item.isChecked()) item.setChecked(false);
				else item.setChecked(true);
				tfla.notifyDataSetChanged();
//				tfla.replaceDataItem(pos, item);
			}
        });

        Button ok_btn=(Button)dialog.findViewById(R.id.file_select_list_dlg_ok_btn);
        final Button cancel_btn=(Button)dialog.findViewById(R.id.file_select_list_dlg_cancel_btn);
        ok_btn.setVisibility(Button.VISIBLE);
        cancel_btn.setVisibility(Button.VISIBLE);
        
        ok_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int sel_cnt=0;
				for (int i=0;i<tfla.getDataItemCount();i++) {
					if (tfla.getDataItem(i).isChecked()) sel_cnt++; 
				}
				if (sel_cnt==0) {
					dlg_msg.setText(getString(R.string.msgs_scan_spinner_not_select_mp));
					return;
				}
				ArrayList<String>fl=new ArrayList<String>();
				for (int i=0;i<tfla.getDataItemCount();i++) {
					if (tfla.getDataItem(i).isChecked()) {
						fl.addAll(buildSdcardScanFileList(true, 
								tfla.getDataItem(i).getName()));
					}
				}
				if (fl.size()!=0) scanSdcardDirectory(fl);
		    	else {
		    		addLogMsg("W",
		    			getString(R.string.msgs_local_file_scan_no_scan_file));
		    		commonDlg.showCommonDialog(false,"W",
		    			getString(R.string.msgs_local_file_scan_no_scan_file),"",null);
		    	}		
//				commonDlg.setFixedOrientation(false);
				dialog.dismiss();
			}
        });
        cancel_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				commonDlg.setFixedOrientation(false);
				dialog.dismiss();
			}
        });
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				cancel_btn.performClick();
			}
		});
//        commonDlg.setFixedOrientation(true);
//        dialog.setCancelable(false);
        dialog.show();
    };
    
    private void scanSdcard(boolean all_item, String lurl) {
    	
    	addLogMsg("I",getString(R.string.msgs_local_file_scan_started));

    	ArrayList<String> fl = buildSdcardScanFileList(all_item, lurl);
    	
//    	for (int i=0;i<fl.size();i++) Log.v("","fl="+fl.get(i));
    	
    	if (fl.size()!=0) scanSdcardDirectory(fl);
    	else {
    		addLogMsg("W",
    			getString(R.string.msgs_local_file_scan_no_scan_file));
    		commonDlg.showCommonDialog(false,"W",
    			getString(R.string.msgs_local_file_scan_no_scan_file),"",null);
    	}
    	setAllFilelistItemUnChecked(treeFilelistAdapter);
    };
    
    private ArrayList<String> buildSdcardScanFileList(boolean all_item, String lurl) {
    	ArrayList<String> fl = new ArrayList<String>();
    	
    	for (int i=0;i<treeFilelistAdapter.getCount();i++) {
    		if (treeFilelistAdapter.getDataItem(i).isEnableItem() &&
    				(treeFilelistAdapter.getDataItem(i).isChecked() || all_item)) {
    			if (treeFilelistAdapter.getDataItem(i).isDir()) {
        			addSdcardScanDirectory(fl, 
        					lurl+treeFilelistAdapter.getDataItem(i).getPath()+
            				treeFilelistAdapter.getDataItem(i).getName());
        		} else {
        			if (isValidFileId(treeFilelistAdapter.getDataItem(i).getName())) {
	        			fl.add(lurl+treeFilelistAdapter.getDataItem(i).getPath()+
	        				treeFilelistAdapter.getDataItem(i).getName());
        			}
        		}
    		}
    	}
    	return fl;
    };
    
    private void addSdcardScanDirectory(ArrayList<String> fl, String fp) {
    	if (!hasNoMediaFile(fp)) {
    		File lf = new File(fp) ;
    		File[] dfl=lf.listFiles() ;
    		if (dfl!=null) {
    			for (File cfl:dfl) {
    				if (cfl.isDirectory()) {
    					if (!cfl.getName().startsWith(".") &&
    							!cfl.getName().equals("LOST.DIR")) {
	    					if (!hasNoMediaFile(cfl.getPath())) {
    	        				addSdcardScanDirectory(fl, cfl.getPath());
	    					}
    					}
    				} else {
    					if (isValidFileId(cfl.getName())) 
	        				fl.add(cfl.getPath());
    				}
    			}
    		}
    	}
    };
    
    private boolean isValidFileId(String fid) {
    	boolean result=false;
    	if (!fid.startsWith(".")) {
			result=true; 
    	} else result=false;
    	return result;
    };
    
    private void scanSdcardDirectory(ArrayList<String> sfl) {
		final ThreadCtrl threadCtl=new ThreadCtrl();
		threadCtl.initThreadCtrl();
		threadCtl.setEnabled();//enableAsyncTask();
		
//		commonDlg.setFixedOrientation(true);
       	
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.progress_spin_dlg);
		final TextView title = (TextView)dialog.findViewById(R.id.progress_spin_dlg_title);
		title.setText(getString(R.string.msgs_local_file_scan_title));
		final Button btnCancel = (Button) dialog.findViewById(R.id.progress_spin_dlg_btn_cancel);
		CommonDialog.setDlgBoxSizeCompact(dialog);
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnCancel.setText(getString(R.string.msgs_local_file_scan_cancelling));
				btnCancel.setEnabled(false);
				threadCtl.setDisabled();//disableAsyncTask();
				threadCtl.setThreadResultCancelled();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		dialog.setCancelable(false);
       	dialog.show();
		
		final NotifyEvent at_ne=new NotifyEvent(this);
		at_ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				commonDlg.showCommonDialog(false,"I",
					getString(R.string.msgs_local_file_scan_completed),"",null);
				buildMediaStoreStatList();
				addLogMsg("I",String.format(
						getString(R.string.msgs_local_file_scan_result_count),
						numberOfFileScanSuccessfull,numberOfFileScanFailed));
				addLogMsg("I",getString(R.string.msgs_local_file_scan_completed));
//				commonDlg.setFixedOrientation(false);
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
				addLogMsg("I",String.format(
						getString(R.string.msgs_local_file_scan_result_count),
						numberOfFileScanSuccessfull,numberOfFileScanFailed));
				if (threadCtl.isThreadResultCancelled()) {
					commonDlg.showCommonDialog(false,"W",getString(R.string.msgs_local_file_scan_cancelled),"",null);
					addLogMsg("W",getString(R.string.msgs_local_file_scan_cancelled));
//					commonDlg.setFixedOrientation(false);
				} else {
					commonDlg.showCommonDialog(false,"E",getString(R.string.msgs_local_file_scan_error_occured),
						threadCtl.getThreadMessage(),null);
					addLogMsg("E",getString(R.string.msgs_local_file_scan_error_occured));
//					commonDlg.setFixedOrientation(false);
				}
				buildMediaStoreStatList();
			}
		});
		startSdcardScanThread(dialog, threadCtl, at_ne, sfl);
	};

	private int numberOfFileScanSuccessfull, numberOfFileScanFailed;
	private void startSdcardScanThread(final Dialog dialog, 
			final ThreadCtrl threadCtl, final NotifyEvent at_ne, 
			final ArrayList<String> sfl) {
		numberOfFileScanSuccessfull=numberOfFileScanFailed=0;
       	new Thread(new Runnable() {
			@Override
			public void run() {//non UI thread
				ContentResolver cr=getContentResolver();
	    		for (int i=0;i<sfl.size();i++) {
		    		if(threadCtl.isEnabled()) {
		    			if (mediaScanner.isConnected()) {
		    				if (scanMediaStoreLibraryFile(cr,sfl.get(i))) {
			    				if (waitScanCompleted()) {
					        		setDlgMsgText(dialog,"I",
					        				getString(R.string.msgs_local_file_scan_was_completed)+
					        				sfl.get(i));
					        		numberOfFileScanSuccessfull++;
			    				} else {
					        		setDlgMsgText(dialog,"E",
					        				getString(R.string.msgs_local_file_scan_was_failed)+
					        				sfl.get(i));
					        		numberOfFileScanFailed++;
					        		threadCtl.setThreadResultError();
					        		threadCtl.setThreadMessage(getString(R.string.msgs_local_file_scan_was_failed)+
					        				sfl.get(i));
					        		threadCtl.setDisabled();
					        		break;
			    				}
		    				}
		    			} else {
		    				addThreadLogMsg("E","MediaScanner is not connected, try later");
		    				break;
		    			}
					} else {
						break;
					}
		    	}

				// dismiss progress bar dialog
				uiHandler.post(new Runnable() {// UI thread
					@Override
					public void run() {
						if (dialog!=null) dialog.dismiss();
						if(threadCtl.isEnabled())at_ne.notifyToListener(true, null);
						else at_ne.notifyToListener(false, null);
					}
				});
			}
		})
       	.start();
	};
	
	@SuppressLint("DefaultLocale")
	private boolean scanMediaStoreLibraryFile(ContentResolver cr,
			String fp) {
		boolean scanned=false;
		if (!mediaScanner.isConnected()) {
			addLogMsg("I","Media Scan bypassed because mediaScanner connection corrupted, name="+fp);
			return false;
		}
		File cf=null, nf=null;
		if (syncWasUseDeleteMethod) {
			cf=new File(fp);
			nf=new File(fp+".msstmp");
			nf.delete();
			boolean rfr=cf.renameTo(nf);
			if (rfr) {
				deleteMediaStoreAudioItem(cr, fp);
				deleteMediaStoreImageItem(cr, fp);
				deleteMediaStoreVideoItem(cr, fp);
				if(Build.VERSION.SDK_INT >= 11) deleteMediaStoreDocItem(cr, fp);
				
				cf.delete();
				nf.renameTo(cf);
			} else {
				syncWasUseDeleteMethod=false;
				addLogMsg("E","Rename was failed fp="+fp);
				return false;
			}
		}

		if(Build.VERSION.SDK_INT >= 11) {
			synchronized(mediaScanLock) {mediaScanCompleted=false;}
			mediaScanner.scanFile(fp, null);
			scanned=true;
		} else {
			String fid="";
			if (fp.lastIndexOf(".")>0) {
				fid=fp.substring(fp.lastIndexOf(".")+1,fp.length());
				fid=fid.toLowerCase(); 
			}
			String mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
			if (mt!=null) {
				sendDebugLogMsg(1,"I",
						"scanMediaStoreLibrary scan was invoked. mt="+mt+", fn="+fp);
				synchronized(mediaScanLock) {mediaScanCompleted=false;}
				mediaScanner.scanFile(fp, mt);
				scanned=true;
			} else {
				sendDebugLogMsg(1,"I","scanMediaStoreLibrary scan was not invoked(Mime_type is null), fn="+fp);
			}
		}
		return scanned;
	};
	
    private void confirmSyncMediaStoreLibrary(final ArrayList<MediaStoreListItem> mll,
    		boolean use_delete) {
    	syncWasUseDeleteMethod=false;
		NotifyEvent ntfy=new NotifyEvent(context);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				syncMediaStoreLibrary(mll);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				syncWasUseDeleteMethod=false;
			}
			
		});
    	if (!use_delete) ntfy.notifyToListener(true, null);
    	else {
    		syncWasUseDeleteMethod=true;
    		String msg=context.getString(R.string.msgs_main_stat_use_delete_warn);
    		commonDlg.showCommonDialog(true, "W", msg, "", ntfy);
    	};
    };
    private void syncMediaStoreLibrary(ArrayList<MediaStoreListItem> mll) {
    	
		final ThreadCtrl threadCtl=new ThreadCtrl();
		threadCtl.initThreadCtrl();
		threadCtl.setEnabled();//enableAsyncTask();
		
//		commonDlg.setFixedOrientation(true);
       	
		addLogMsg("I",getString(R.string.msgs_media_store_sync_started));

		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.progress_spin_dlg);
		final TextView title = (TextView)dialog.findViewById(R.id.progress_spin_dlg_title);
		title.setText(getString(R.string.msgs_media_store_sync_title));
		final Button btnCancel = (Button)dialog.findViewById(R.id.progress_spin_dlg_btn_cancel);
		CommonDialog.setDlgBoxSizeCompact(dialog);
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btnCancel.setText(getString(R.string.msgs_media_store_sync_cancelling));
				btnCancel.setEnabled(false);
				threadCtl.setDisabled();//disableAsyncTask();
				threadCtl.setThreadResultCancelled();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
//		dialog.setCancelable(false);
       	dialog.show();
		
		final NotifyEvent at_ne=new NotifyEvent(this);
		at_ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				addLogMsg("I",String.format(
						getString(R.string.msgs_media_store_sync_result_count),
						numberOfDelete,numberOfDeleteFailed));
				addLogMsg("I",String.format(
						getString(R.string.msgs_local_file_scan_result_count),
						numberOfFileScanSuccessfull,numberOfFileScanFailed));
				
				NotifyEvent ntfy=new NotifyEvent(c);
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						buildMediaStoreStatList();
					}
					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {}
				});
				commonDlg.showCommonDialog(false,"I",getString(R.string.msgs_media_store_sync_completed),
						"",ntfy);
				addLogMsg("I",getString(R.string.msgs_media_store_sync_completed));
//				commonDlg.setFixedOrientation(false);
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
//				commonDlg.setFixedOrientation(false);
				addLogMsg("I",String.format(
						getString(R.string.msgs_media_store_sync_result_count),
						numberOfDelete,numberOfDeleteFailed));
				addLogMsg("I",String.format(
						getString(R.string.msgs_local_file_scan_result_count),
						numberOfFileScanSuccessfull,numberOfFileScanFailed));
				String err_msg="", err_detail="", err_lvl="";
				if (threadCtl.isThreadResultCancelled()) {
					err_msg=getString(R.string.msgs_media_store_sync_cancelled);
					err_lvl="W";
					addLogMsg("W",getString(R.string.msgs_media_store_sync_cancelled));
				} else {
					err_msg=getString(R.string.msgs_media_store_sync_error_occured);
					err_detail=threadCtl.getThreadMessage();
					err_lvl="E";
					addLogMsg("E",getString(R.string.msgs_media_store_sync_error_occured));
				}
				NotifyEvent ntfy=new NotifyEvent(context);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						buildMediaStoreStatList();		
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {}
				});
				commonDlg.showCommonDialog(false,
						err_lvl,err_msg,err_detail,ntfy);
			}
		});
		startMediaStoreSyncThread(dialog, threadCtl, at_ne, mll);
	};

	private int numberOfDelete, numberOfDeleteFailed;  
	private void startMediaStoreSyncThread(final Dialog dialog, 
			final ThreadCtrl threadCtl, final NotifyEvent at_ne, 
			final ArrayList<MediaStoreListItem> mll) {
		numberOfDelete=numberOfDeleteFailed=0;
		numberOfFileScanSuccessfull=numberOfFileScanFailed=0;
		new Thread(new Runnable() {
			@Override
			public void run() {//non UI thread
		    	MediaStoreListItem mli;
		    	ContentResolver cr = getContentResolver();
		    	String scan_msg=getString(R.string.msgs_media_store_sync_scan_processing);
		    	String msg_txt="";
		    	int prev_pct=-1, proc_pct=0;
		    	int tot_cnt=mll.size();
	    		for (int i=0;i<mll.size();i++) {
		    		if(threadCtl.isEnabled()) {
		    			proc_pct=(i*100)/tot_cnt;
		    			if (prev_pct!=proc_pct) {
		    				prev_pct=proc_pct;
		    				msg_txt=String.format(scan_msg, prev_pct);
		    			}
		    			mli=mll.get(i);
			    		if (mli.isFileNotExist()) {
			    			int dc=0;
			    			if (mli.getMediaType().startsWith("I")) {
			    				dc=deleteMediaStoreImageItem(cr, mli.getFilePath());
			    			} else if (mli.getMediaType().startsWith("A")) {
			    				dc=deleteMediaStoreAudioItem(cr, mli.getFilePath());
			    	    	} else if (mli.getMediaType().startsWith("V")) {
			    	    		dc=deleteMediaStoreVideoItem(cr, mli.getFilePath());
			    	    	} else if (mli.getMediaType().startsWith("D")) {
			    	    		dc=deleteMediaStoreDocItem(cr, mli.getFilePath());
			    	    	}
			    			if (dc!=0) {
			    				numberOfDelete=numberOfDelete+dc;
			    				setDlgMsgText(dialog,"I",msg_txt+"\n"+
			    						getString(R.string.msgs_media_store_sync_delete_successfull)+
			    						mli.getDisplayName());
			    			} else {
			    				numberOfDeleteFailed++;
			    				setDlgMsgText(dialog,"E",
			    						getString(R.string.msgs_media_store_sync_delete_error_occured)+
			    						mli.getDisplayName());
				        		threadCtl.setThreadResultError();
				        		threadCtl.setThreadMessage(getString(R.string.msgs_media_store_sync_delete_error_occured)+
				        				mli.getDisplayName());
			    				threadCtl.setDisabled();
			    				break;
			    			}
			    		} else if (mli.isFileDifferent()) {
			    			if (mediaScanner.isConnected()) {
			    				if (scanMediaStoreLibraryFile(cr, mli.getFilePath())) {
				    				if (waitScanCompleted()) {
				    					numberOfFileScanSuccessfull++;
						        		setDlgMsgText(dialog,"I",msg_txt+"\n"+
						        				getString(R.string.msgs_media_store_sync_scan_completed)+
						        						mli.getFilePath());
				    				} else {
				    					numberOfFileScanFailed++;
						        		setDlgMsgText(dialog,"E",
						        				getString(R.string.msgs_media_store_sync_scan_failed)+
						        						mli.getFilePath());
						        		threadCtl.setThreadResultError();
						        		threadCtl.setThreadMessage(getString(R.string.msgs_media_store_sync_scan_failed)+
						        				mli.getFilePath());
						        		threadCtl.setDisabled();
						        		break;
				    				}
			    				}
			    			} else {
			    				if (DEBUG_ENABLE) 
			    					sendDebugLogMsg(1,"E","MediaScanner is not connected, try later");
			    				break;
			    			}
			    		}
					} else {
						break;
					}
		    	}

				// dismiss progress bar dialog
				uiHandler.post(new Runnable() {// UI thread
					@Override
					public void run() {
						if (dialog!=null) dialog.dismiss();
						if(threadCtl.isEnabled())at_ne.notifyToListener(true, null);
						else at_ne.notifyToListener(false, null);
					}
				});
			}
		})
       	.start();
	};

	private boolean waitScanCompleted() {
		boolean comp=false;
		try {
			for (int i=0;i<1000;i++) {
				synchronized(mediaScanLock) {
					if (mediaScanCompleted) {
						comp=true;
						break;
					}
				}
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		synchronized(mediaScanLock) {
			mediaScanCompleted=false;
		}
		return comp;
	};
	
	private int deleteMediaStoreImageItem(ContentResolver cr,String fp) {
		
		int dc=cr.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          		MediaStore.Images.Media.DATA + "=?", new String[]{fp} );
		if (DEBUG_ENABLE && dc>0) 
			sendDebugLogMsg(1,"I","Image item deleted, name="+fp);
    	return dc;
	};
	
	private int deleteMediaStoreAudioItem(ContentResolver cr,String fp) {
		int dc=cr.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
          		MediaStore.Images.Media.DATA + "=?", new String[]{fp} );
		if (DEBUG_ENABLE && dc>0) 
			sendDebugLogMsg(1,"I","Audio item deleted, name="+fp);
    	return dc;
	};

	private int deleteMediaStoreVideoItem(ContentResolver cr,String fp) {
    		int dc=cr.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 
              		MediaStore.Images.Media.DATA + "=?", new String[]{fp} );
    		if (DEBUG_ENABLE && dc>0) 
    			sendDebugLogMsg(1,"I","Video item deleted, name="+fp);
        	return dc;
	};

	@SuppressLint("InlinedApi")
	private int deleteMediaStoreDocItem(ContentResolver cr,String fp) {
		int dc=cr.delete(MediaStore.Files.getContentUri("external"), 
          		MediaStore.Files.FileColumns.DATA + "=?", new String[]{fp} );
		if (DEBUG_ENABLE && dc>0) 
			sendDebugLogMsg(1,"I","Doc item deleted, name="+fp);
    	return dc;
	};

	@SuppressWarnings("unused")
	private void getMediaStoreInfo(Uri uri) {
		String[] queryProj=new String[] {
				MediaStore.MediaColumns.DATA,MediaStore.MediaColumns.DATE_ADDED,
				MediaStore.MediaColumns.DATE_MODIFIED,MediaStore.MediaColumns._ID,
				MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE};
		ContentResolver resolver = getContentResolver();
        Cursor ci = resolver.query(uri,queryProj ,null ,null ,"_data");
//		Log.v("","ci="+ci);
		if (ci!=null) {
			ci.moveToNext();
        	String file_path=ci.getString(ci.getColumnIndex( MediaStore.Images.Media.DATA));
        	String display_name=ci.getString(ci.getColumnIndex( MediaStore.Images.Media.DISPLAY_NAME));
        	long date_added=ci.getLong(ci.getColumnIndex( MediaStore.Images.Media.DATE_ADDED));
        	long date_modified=ci.getLong(ci.getColumnIndex( MediaStore.Images.Media.DATE_MODIFIED));
        	long media_size=ci.getLong(ci.getColumnIndex( MediaStore.Images.Media.SIZE));

//        	Log.v("","file_path="+file_path);
//        	Log.v("","display_name="+display_name);
//        	Log.v("","date_added="+sdfDateTime.format(date_added));
//        	Log.v("","date_modified="+sdfDateTime.format(date_modified));
		}
	};
	
	private static final String[] msQueryProj=new String[] {
		MediaStore.MediaColumns.DATA,MediaStore.MediaColumns.DATE_ADDED,
		MediaStore.MediaColumns.DATE_MODIFIED,MediaStore.MediaColumns._ID,
		MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE};
    private ArrayList<MediaStoreListItem> createMediaStoreImageList(
    		ThreadCtrl tctl, Dialog dialog, final String msg_txt) {
    	ArrayList<MediaStoreListItem> msl=new ArrayList<MediaStoreListItem>();
		final CustomTextView pm=(CustomTextView)dialog.findViewById(R.id.progress_spin_dlg_msg);
		mediaStorePrevProgress=-1;
    	ContentResolver resolver = getContentResolver();
    	//build image
        Cursor ci = resolver.query(
        	MediaStore.Images.Media.EXTERNAL_CONTENT_URI ,msQueryProj ,null ,null ,"_data");
        
        if (ci!=null ) {
        	int proc_count=0;
        	int tot_count=ci.getCount();
	        while( ci.moveToNext() ){
	        	if (!tctl.isEnabled()) break;
//	        	Log.v("","id="+ci.getString(ci.getColumnIndex(MediaStore.MediaColumns._ID)));
	        	boolean media_file_different=false, m_s_d=false, m_m_d=false, f_n_e=false;
	        	String file_path=ci.getString(ci.getColumnIndex( MediaStore.Images.Media.DATA));
	        	String display_name=ci.getString(ci.getColumnIndex( MediaStore.Images.Media.DISPLAY_NAME));
	        	long date_added=ci.getLong(ci.getColumnIndex( MediaStore.Images.Media.DATE_ADDED));
	        	long date_modifiedx=ci.getLong(ci.getColumnIndex( MediaStore.Images.Media.DATE_MODIFIED));
	        	long media_size=ci.getLong(ci.getColumnIndex( MediaStore.Images.Media.SIZE));
	        	long date_modified=0;
	        	if (Build.VERSION.SDK_INT<=10) {//Android 3.0以下のバグ回避
		        	if (date_modifiedx>=1000000000000L) date_modified=date_modifiedx/1000;
		        	else date_modified=date_modifiedx;
	        	} else date_modified=date_modifiedx;
	        	
	        	proc_count++;
	        	int proc_pct=(proc_count*100)/tot_count;
	        	if (mediaStorePrevProgress!=proc_pct) {
	        		mediaStorePrevProgress=proc_pct;
	        		uiHandler.post(new Runnable() {
	        			//UI thread
	        			@Override
	        			public void run() {
	        				pm.setText(msg_txt+"("+mediaStorePrevProgress+"%)");
	        		}});

	        	}

	        	sendThreadDebugLogMsg(2,"I","Image list item" +
	        			", Name="+display_name+
	        			", Path="+file_path+
	        			", Date_added="+sdfDateTime.format(date_added*1000)+
	        			", Date_modified="+sdfDateTime.format(date_modified*1000));
	        	
	        	File lf=new File(file_path);
	        	long f_l_m=0, f_size=0;
	        	if (lf.exists()) {
	        		if (lf.length()==media_size) {
	        			if (Math.abs(lf.lastModified()/1000-date_modified)<=2) 
	        				media_file_different=false;
	        			else m_m_d=media_file_different=true;
	        		} else m_s_d=media_file_different=true;
	        		f_l_m=lf.lastModified()/1000;
	        		f_size=lf.length();
	        	} else f_n_e=media_file_different=true;
	        	
	    		msl.add(new MediaStoreListItem(file_path, 
	    				MediaStoreListItem.MEDIA_STORE_LIST_ITEM_MEDIA_TYPE_IMAGES, 
	        			media_file_different, f_n_e, m_s_d, m_m_d, 
	        			display_name, date_added, 
	        			date_modified*1000, media_size, f_l_m*1000, f_size));
	        }
	        ci.close();
        }
    	return msl;
    };
    
    private ArrayList<MediaStoreListItem> createMediaStoreAudioList(
    		ThreadCtrl tctl, Dialog dialog, final String msg_txt) {
    	ArrayList<MediaStoreListItem> msl=new ArrayList<MediaStoreListItem>();
		final CustomTextView pm=(CustomTextView)dialog.findViewById(R.id.progress_spin_dlg_msg);
		mediaStorePrevProgress=-1;
    	ContentResolver resolver = getContentResolver();
    	//build image
        Cursor ci = resolver.query(
        		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ,
        		msQueryProj ,null ,null ,"_data");
        if (ci!=null ) {
        	int proc_count=0;
        	int tot_count=ci.getCount();
	        while(ci.moveToNext() ){
	        	if (!tctl.isEnabled()) break;
	        	boolean media_file_different=false, m_s_d=false, m_m_d=false, f_n_e=false;
	        	String file_path=ci.getString( ci.getColumnIndex( MediaStore.Audio.Media.DATA ));
	        	String display_name=ci.getString( ci.getColumnIndex( MediaStore.Audio.Media.DISPLAY_NAME));
	        	long date_added=ci.getLong( ci.getColumnIndex( MediaStore.Audio.Media.DATE_ADDED));
	        	long date_modifiedx=ci.getLong( ci.getColumnIndex( MediaStore.Audio.Media.DATE_MODIFIED));
	        	long media_size=ci.getLong( ci.getColumnIndex( MediaStore.Audio.Media.SIZE));
	        	long date_modified=0;
	        	if (Build.VERSION.SDK_INT<=10) {//Android 3.0以下のバグ回避
		        	if (date_modifiedx>=1000000000000L) date_modified=date_modifiedx/1000;
		        	else date_modified=date_modifiedx;
	        	} else date_modified=date_modifiedx;

	        	proc_count++;
	        	int proc_pct=(proc_count*100)/tot_count;
	        	if (mediaStorePrevProgress!=proc_pct) {
	        		mediaStorePrevProgress=proc_pct;
	        		uiHandler.post(new Runnable() {
	        			//UI thread
	        			@Override
	        			public void run() {
	        				pm.setText(msg_txt+"("+mediaStorePrevProgress+"%)");
	        		}});

	        	}

	        	sendThreadDebugLogMsg(2,"I","Audio list item" +
	        			", Name="+display_name+
	        			", Path="+file_path+
	        			", Date_added="+sdfDateTime.format(date_added*1000)+
	        			", Date_modified="+sdfDateTime.format(date_modified*1000));
	        	
	        	File lf=new File(file_path);
	        	long f_l_m=0, f_size=0;
	        	if (lf.exists()) {
	        		if (lf.length()==media_size) {
	        			if (Math.abs(lf.lastModified()/1000-date_modified)<=2) 
	        				media_file_different=false;
	        			else m_m_d=media_file_different=true;
	        		} else m_s_d=media_file_different=true;
	        		f_l_m=lf.lastModified()/1000;
	        		f_size=lf.length();
	        	} else f_n_e=media_file_different=true;
	        	
	    		msl.add(new MediaStoreListItem(file_path, 
	    				MediaStoreListItem.MEDIA_STORE_LIST_ITEM_MEDIA_TYPE_AUDIO, 
	        			media_file_different, f_n_e, m_s_d, m_m_d, 
	        			display_name, date_added, 
	        			date_modified*1000, media_size, f_l_m*1000, f_size));
	        }
	        ci.close();
        }
    	return msl;
    };
    
    private ArrayList<MediaStoreListItem> createMediaStoreVideoList(
    		ThreadCtrl tctl, Dialog dialog, final String msg_txt) {
    	ArrayList<MediaStoreListItem> msl=new ArrayList<MediaStoreListItem>();
		final CustomTextView pm=(CustomTextView)dialog.findViewById(R.id.progress_spin_dlg_msg);
		mediaStorePrevProgress=-1;
    	ContentResolver resolver = getContentResolver();
    	//build image
        Cursor ci = resolver.query(
        	MediaStore.Video.Media.EXTERNAL_CONTENT_URI ,
        	msQueryProj ,null ,null ,"_data");
        if (ci!=null ) {
        	int proc_count=0;
        	int tot_count=ci.getCount();
	        while(ci.moveToNext() ){
	        	if (!tctl.isEnabled()) break;
	        	boolean media_file_different=false, m_s_d=false, m_m_d=false, f_n_e=false;
	        	String file_path=ci.getString( ci.getColumnIndex( MediaStore.Video.Media.DATA ));
	        	String display_name=ci.getString( ci.getColumnIndex( MediaStore.Video.Media.DISPLAY_NAME));
	        	long date_added=ci.getLong( ci.getColumnIndex( MediaStore.Video.Media.DATE_ADDED));
	        	long date_modifiedx=ci.getLong( ci.getColumnIndex( MediaStore.Video.Media.DATE_MODIFIED));
	        	long media_size=ci.getLong( ci.getColumnIndex( MediaStore.Video.Media.SIZE));
	        	long date_modified=0;
	        	if (Build.VERSION.SDK_INT<=10) {//Android 3.0以下のバグ回避
		        	if (date_modifiedx>=1000000000000L) date_modified=date_modifiedx/1000;
		        	else date_modified=date_modifiedx;
	        	} else date_modified=date_modifiedx;

	        	proc_count++;
	        	int proc_pct=(proc_count*100)/tot_count;
	        	if (mediaStorePrevProgress!=proc_pct) {
	        		mediaStorePrevProgress=proc_pct;
	        		uiHandler.post(new Runnable() {
	        			//UI thread
	        			@Override
	        			public void run() {
	        				pm.setText(msg_txt+"("+mediaStorePrevProgress+"%)");
	        		}});

	        	}

	        	sendThreadDebugLogMsg(2,"I","Video list item" +
	        			", Name="+display_name+
	        			", Path="+file_path+
	        			", Date_added="+sdfDateTime.format(date_added*1000)+
	        			", Date_modified="+sdfDateTime.format(date_modified*1000));
	        	
	        	File lf=new File(file_path);
	        	long f_l_m=0, f_size=0;
	        	if (lf.exists()) {
	        		if (lf.length()==media_size) {
	        			if (Math.abs(lf.lastModified()/1000-date_modified)<=2) 
	        				media_file_different=false;
	        			else m_m_d=media_file_different=true;
	        		} else m_s_d=media_file_different=true;
	        		f_l_m=lf.lastModified()/1000;
	        		f_size=lf.length();
	        	} else f_n_e=media_file_different=true;
	        	
	    		msl.add(new MediaStoreListItem(file_path, 
	    				MediaStoreListItem.MEDIA_STORE_LIST_ITEM_MEDIA_TYPE_VIDEO, 
	        			media_file_different, f_n_e, m_s_d, m_m_d, 
	        			display_name, date_added, 
	        			date_modified*1000, media_size, f_l_m*1000, f_size));
	        }
	        ci.close();
        }
    	return msl;
    };

	int mediaStorePrevProgress=-1;
    @SuppressLint("InlinedApi")
	private ArrayList<MediaStoreListItem> createMediaStoreDocList(
			ThreadCtrl tctl, Dialog dialog, final String msg_txt) {
    	mediaStorePrevProgress=-1;
    	ArrayList<MediaStoreListItem> msl=new ArrayList<MediaStoreListItem>();
		final CustomTextView pm=(CustomTextView)dialog.findViewById(R.id.progress_spin_dlg_msg);
    	if (defaultSettingScanDocFiles) {
        	ContentResolver resolver = getContentResolver();
        	//build image
            Cursor ci = resolver.query(
            	MediaStore.Files.getContentUri("external") ,
            	msQueryProj ,null ,null ,"_data");
            if (ci!=null ) {
            	int proc_count=0;
            	int tot_count=ci.getCount();
    	        while(ci.moveToNext() ){
    	        	if (!tctl.isEnabled()) break;
    	        	boolean media_file_different=false, m_s_d=false, m_m_d=false, f_n_e=false;
    	        	String file_path=ci.getString( ci.getColumnIndex( MediaStore.Files.FileColumns.DATA));
    	        	String display_name=ci.getString( ci.getColumnIndex( MediaStore.Files.FileColumns.DATA));
    	        	long date_added=ci.getLong( ci.getColumnIndex( MediaStore.Files.FileColumns.DATE_ADDED));
    	        	long date_modifiedx=ci.getLong( ci.getColumnIndex( MediaStore.Files.FileColumns.DATE_MODIFIED));
    	        	long media_size=ci.getLong( ci.getColumnIndex( MediaStore.Files.FileColumns.SIZE));
    	        	long date_modified=0;
    	        	if (Build.VERSION.SDK_INT<=10) {//Android 3.0以下のバグ回避
    		        	if (date_modifiedx>=1000000000000L) date_modified=date_modifiedx/1000;
    		        	else date_modified=date_modifiedx;
    	        	} else date_modified=date_modifiedx;
    	        	
    	        	proc_count++;
    	        	int proc_pct=(proc_count*100)/tot_count;
    	        	if (mediaStorePrevProgress!=proc_pct) {
    	        		mediaStorePrevProgress=proc_pct;
    	        		uiHandler.post(new Runnable() {
    	        			//UI thread
    	        			@Override
    	        			public void run() {
    	        				pm.setText(msg_txt+"("+mediaStorePrevProgress+"%)");
//    	        				pm.postInvalidate();
    	        		}});

    	        	}
    	        	sendThreadDebugLogMsg(2,"I","Files list item" +
    	        			", Name="+display_name+
    	        			", Path="+file_path+
    	        			", Date_added="+sdfDateTime.format(date_added*1000)+
    	        			", Date_modified="+sdfDateTime.format(date_modified*1000));
    	        	
    	        	File lf=new File(file_path);
    	        	long f_l_m=0, f_size=0;
    	        	if (!lf.isDirectory() && lf.canRead()) {
    		        	if (lf.exists()) {
    		        		if (lf.length()==media_size) {
    		        			if (Math.abs(lf.lastModified()/1000-date_modified)<=2) 
    		        				media_file_different=false;
    		        			else m_m_d=media_file_different=true;
    		        		} else m_s_d=media_file_different=true;
    		        		f_l_m=lf.lastModified()/1000;
    		        		f_size=lf.length();
    		        	} else f_n_e=media_file_different=true;
    		    		msl.add(new MediaStoreListItem(file_path, 
    		    				MediaStoreListItem.MEDIA_STORE_LIST_ITEM_MEDIA_TYPE_DOCUMENT, 
    		        			media_file_different, f_n_e, m_s_d, m_m_d, 
    		        			display_name, date_added, 
    		        			date_modified*1000, media_size, f_l_m*1000, f_size));
    	        	}
    	        }
    	        ci.close();
            }
    	}
    	return msl;
    };
	
	private void setDlgMsgText(Dialog dialog, 
			final String msgcat, final String msgtext) {
		final CustomTextView pm=(CustomTextView)dialog.findViewById(R.id.progress_spin_dlg_msg);
		pm.setTextColor(mThemeColorList.text_color_primary);
		uiHandler.post(new Runnable() {
			//UI thread
			@Override
			public void run() {
				pm.setText(msgtext);
				addLogMsg(msgcat,msgtext);
		}});
	};

	private void sendThreadDebugLogMsg(final int lvl, final String cat, final String logmsg) {
		uiHandler.post(new Runnable() {
			//UI thread
			@Override
			public void run() {
				sendDebugLogMsg(lvl,cat,logmsg);
		}});

	};

	private void addThreadLogMsg(final String cat, final String logmsg) {
		uiHandler.post(new Runnable() {
			//UI thread
			@Override
			public void run() {
				addLogMsg(cat,logmsg);
		}});

	};


	public void addLogMsg(String cat, String logmsg) {
		msglistAdapter.add(
			  	new MsglistItem(cat,sdfDate.format(System.currentTimeMillis()),
					sdfTime.format(System.currentTimeMillis()),"MAIN",logmsg));
		msgListView.setSelection(msgListView.getCount()-1);
		
		if (logPrintWriter!=null) {
			logPrintWriter.println(cat+" "+
				sdfDate.format(System.currentTimeMillis())+" "+
				sdfTime.format(System.currentTimeMillis())+" "+
				("MAIN"+"          ").substring(0,10)+logmsg);
			logPrintWriter.flush();
		}
		if (DEBUG_ENABLE ) {
			if (debug_level>0) Log.v(DEBUG_TAG,cat+" "+logmsg);
		}
	};

	public void sendDebugLogMsg(int lvl, String cat, String logmsg) {
		if (debug_level>=lvl ) {
			if (logPrintWriter!=null) {
				logPrintWriter.println(cat+" "+
					sdfDate.format(System.currentTimeMillis())+" "+
					sdfTime.format(System.currentTimeMillis())+" "+
					("DEBUG"+"          ").substring(0,10)+logmsg);
				logPrintWriter.flush();
			}
			if (DEBUG_ENABLE) Log.v(DEBUG_TAG,cat+" "+logmsg);
			if (defaultSettingDebugMsgDisplay) {
				
				if (msglistAdapter.getCount()>=5000) {
					for (int i=0;i<=1000;i++) msglistAdapter.remove(0);
				}
				
				msglistAdapter.add(
			  		 new MsglistItem(cat,sdfDate.format(System.currentTimeMillis()),
							sdfTime.format(System.currentTimeMillis()),"DEBUG",logmsg));
				msgListView.setSelection(msgListView.getCount()-1);
			}
		}
	};

	@SuppressLint("SimpleDateFormat")
	private void openLogFile() {
		if (DEBUG_ENABLE) sendDebugLogMsg(2,"I","open log file entered.");
		if (logPrintWriter!=null || !LocalMountPoint.isExternalStorageAvailable()) return;

		File lf=new File(defaultSettingLogMsgDir);
		if(!lf.exists()) lf.mkdirs();
		
		try {
			BufferedWriter bw;
			FileWriter fw ;	
			fw=new FileWriter(defaultSettingLogMsgDir+
					defaultSeiingLogMsgFilename,true);
			bw = new BufferedWriter(fw,8192);
			logPrintWriter = new PrintWriter(bw);
			if (debug_level>=2) {
				Calendar cd = Calendar.getInstance();
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String logmsg="I "+df.format(cd.getTime()) + " LOGT      " + "Log file opened.";
				logPrintWriter.println(logmsg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	};
	
	@SuppressLint("SimpleDateFormat")
	private void closeLogFile() {
		if (DEBUG_ENABLE) sendDebugLogMsg(2,"I","close log file entered.");
		if (logPrintWriter!=null) {
			synchronized(logPrintWriter) {
				if (debug_level>=2) {
					Calendar cd = Calendar.getInstance();
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String logmsg="I "+df.format(cd.getTime()) + " LOGT      " + "Log file closed.";
					logPrintWriter.println(logmsg);
				}
				logPrintWriter.close();
				logPrintWriter=null;
			}
		}
	};
	
	private boolean deleteLogFile() {
		if (DEBUG_ENABLE) 
			sendDebugLogMsg(2,"I","delete log file entered.");
		if (logPrintWriter==null) {
			File lf = new File(defaultSettingLogMsgDir+
					defaultSeiingLogMsgFilename);
			return lf.delete();
		} else return false;
	};

	private void saveTaskData() {
		if (terminateApplication) return ;
		
		sendDebugLogMsg(1,"I", "saveTaskData entered");
		
		if (isTaskDataExisted() && !msglistAdapter.resetDataChanged()) return; 
		
		TaskDataHolder data = new TaskDataHolder();
		data.ml=msglistAdapter.getAllItem();
		
		data.tfl=treeFilelistAdapter.getDataList();
		
		ArrayList<String>spl=new ArrayList<String>();
		for (int i=0;i<spinnerAdapter.getCount();i++) {
			spl.add(spinnerAdapter.getItem(i));
		}
		data.spList=spl;
		data.spSelected=scanSpinner.getSelectedItemPosition();
		
		data.mslImage=new ArrayList<MediaStoreListItem>();
		data.mslAudio=new ArrayList<MediaStoreListItem>();
		data.mslVideo=new ArrayList<MediaStoreListItem>();
		data.mslFiles=new ArrayList<MediaStoreListItem>();
		data.mslSync=new ArrayList<MediaStoreListItem>();
//		data.mslImage.addAll(mediaStoreListImage);
//		data.mslAudio.addAll(mediaStoreListAudio);
//		data.mslVideo.addAll(mediaStoreListVideo);
//		data.mslFiles.addAll(mediaStoreListFiles);
//		data.mslSync.addAll(mediaStoreSyncList);
		
		try {
		    FileOutputStream fos = openFileOutput(SERIALIZABLE_FILE_NAME, MODE_PRIVATE);
		    ObjectOutputStream oos = new ObjectOutputStream(fos);
		    oos.writeObject(data);
		    oos.close();
		    sendDebugLogMsg(1,"I", "TaskData was saved.");
		} catch (Exception e) {
			e.printStackTrace();
		    sendDebugLogMsg(1,"E", 
		    		"saveTaskData error, "+e.toString());
		}
	};
	
	private static final String SERIALIZABLE_FILE_NAME="Serial.dat";
	private void restoreTaskData() {
		sendDebugLogMsg(1,"I", "restoreTaskData entered");
		try {
		    File lf =
		    	new File(getFilesDir()+"/"+SERIALIZABLE_FILE_NAME);
//		    FileInputStream fis = openFileInput(SMBSYNC_SERIALIZABLE_FILE_NAME);
		    FileInputStream fis = new FileInputStream(lf); 
		    ObjectInputStream ois = new ObjectInputStream(fis);
		    TaskDataHolder data = (TaskDataHolder) ois.readObject();
		    ois.close();
		    lf.delete();
		    
		    ArrayList<MsglistItem> o_ml=new ArrayList<MsglistItem>(); 
			for (int i=0;i<msglistAdapter.getCount();i++)
				o_ml.add(msglistAdapter.getItem(i));
		    
			msglistAdapter.clear();
			
			msglistAdapter.setAllItem(data.ml);

			for (int i=0;i<o_ml.size();i++) msglistAdapter.add(o_ml.get(i));
			msglistAdapter.notifyDataSetChanged();
			msglistAdapter.resetDataChanged();
			
			treeFilelistAdapter = new TreeFilelistAdapter(this);
			treeFilelistAdapter.setDataList(data.tfl);
			
			for (int i=0;i<data.spList.size();i++) 
				spinnerAdapter.add(data.spList.get(i));
			scanSpinner.setSelection(data.spSelected);

			mediaStoreListImage=new ArrayList<MediaStoreListItem>();
			mediaStoreListAudio=new ArrayList<MediaStoreListItem>();
			mediaStoreListVideo=new ArrayList<MediaStoreListItem>();
			mediaStoreListFiles=new ArrayList<MediaStoreListItem>();
			mediaStoreSyncList=new ArrayList<MediaStoreListItem>();
			
//			mediaStoreListImage.addAll(data.mslImage);
//			mediaStoreListAudio.addAll(data.mslAudio);
//			mediaStoreListVideo.addAll(data.mslVideo);
//			mediaStoreListFiles.addAll(data.mslFiles);
//			mediaStoreSyncList.addAll(data.mslSync);
			
			sendDebugLogMsg(1,"I", "TaskData was restored.");
		} catch (Exception e) {
			e.printStackTrace();
		    sendDebugLogMsg(1,"E", "restoreTaskData error, "+e.toString());
		}
	};

	private boolean isTaskDataExisted() {
    	File lf =new File(getFilesDir()+"/"+SERIALIZABLE_FILE_NAME);
	    return lf.exists();
	};

	private void deleteTaskData() {
	    	File lf =new File(getFilesDir()+"/"+SERIALIZABLE_FILE_NAME);
		    lf.delete();
		    sendDebugLogMsg(1,"I", "TaskData was deleted.");
	};

	public class CustomTabContentView extends FrameLayout {  
        LayoutInflater inflater = (LayoutInflater) getApplicationContext()  
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
      
        public CustomTabContentView(Context context) {  
            super(context);  
        }  
        public CustomTabContentView(Context context, String title) {  
            this(context);  
            View childview1 = inflater.inflate(R.layout.tab_widget1, null);  
            TextView tv1 = (TextView) childview1.findViewById(R.id.tab_widget1_textview);  
            tv1.setText(title);  
            addView(childview1);  
       }  
    };
    
    class TaskDataHolder implements Externalizable  {

    	private static final long serialVersionUID = 1L;

    	ArrayList<MsglistItem> ml;
    	
    	ArrayList<TreeFilelistItem> tfl;
    	
        ArrayList<MediaStoreListItem> mslImage;
        ArrayList<MediaStoreListItem> mslAudio;
        ArrayList<MediaStoreListItem> mslVideo;
        ArrayList<MediaStoreListItem> mslFiles;
        ArrayList<MediaStoreListItem> mslSync;

    	ArrayList<String> spList;
    	int spSelected;
    	
    	public TaskDataHolder() {};
    	
		@SuppressWarnings("unchecked")
		public void readExternal(ObjectInput objin) throws IOException,
		ClassNotFoundException {
			long sid=objin.readLong();
			if (serialVersionUID!=sid) {
				throw new IOException("serialVersionUID was not matched by saved UID");
			}
		
			ml=(ArrayList<MsglistItem>) SerializeUtil.readArrayList(objin);
			tfl=(ArrayList<TreeFilelistItem>) SerializeUtil.readArrayList(objin);
			mslImage=(ArrayList<MediaStoreListItem>) SerializeUtil.readArrayList(objin);
			mslAudio=(ArrayList<MediaStoreListItem>) SerializeUtil.readArrayList(objin);
			mslVideo=(ArrayList<MediaStoreListItem>) SerializeUtil.readArrayList(objin);
			mslFiles=(ArrayList<MediaStoreListItem>) SerializeUtil.readArrayList(objin);
			mslSync=(ArrayList<MediaStoreListItem>) SerializeUtil.readArrayList(objin);
			spList=(ArrayList<String>) SerializeUtil.readArrayList(objin);
			spSelected=objin.readInt();
		}
		
		@Override
		public void writeExternal(ObjectOutput objout) throws IOException {
			objout.writeLong(serialVersionUID);
			SerializeUtil.writeArrayList(objout, ml);
			SerializeUtil.writeArrayList(objout, tfl);
			SerializeUtil.writeArrayList(objout, mslImage);
			SerializeUtil.writeArrayList(objout, mslAudio);
			SerializeUtil.writeArrayList(objout, mslVideo);
			SerializeUtil.writeArrayList(objout, mslFiles);
			SerializeUtil.writeArrayList(objout, mslSync);
			objout.writeInt(spSelected);
		}

    };


}
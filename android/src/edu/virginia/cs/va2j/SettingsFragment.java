package edu.virginia.cs.va2j;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.*;
import android.net.Uri;
import android.os.Bundle;
import android.preference.*;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;

public class SettingsFragment extends PreferenceFragment {
	
	private View rootview;
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static SettingsFragment newInstance(int sectionNumber) {
		SettingsFragment fragment = new SettingsFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	public SettingsFragment() {
	}

	/*
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.settings_fragment, container,
				false);
		this.rootview = rootView;
		//rootview.findViewById(R.string.pref_cat_gen).setVisibility(View.VISIBLE);
    	//rootview.findViewById(R.id.spMode).setVisibility(View.VISIBLE);
    	//rootview.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
    	//rootview.findViewById(R.id.listPackages).setEnabled(true);
    	
		return rootView;
	}
	*/
	 @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Constants.LOG_TAG, getActivity().MODE_MULTI_PROCESS | getActivity().MODE_PRIVATE);
	        //if old preferences exist, convert them.
	        if(sharedPreferences.contains(Constants.LOG_TAG + ".mode")){
	            SharedPreferences sharedPref = getActivity().getSharedPreferences(Constants.LOG_TAG + "_preferences", getActivity().MODE_MULTI_PROCESS | getActivity().MODE_PRIVATE);
	            SharedPreferences.Editor editor = sharedPref.edit();
	            editor.putInt(Constants.PREFERENCE_MODE, sharedPreferences.getInt(Constants.LOG_TAG + ".mode", Constants.Mode.OFF.ordinal()));
	            editor.putString(Constants.PREFERENCE_PACKAGE_LIST, sharedPreferences.getString(Constants.LOG_TAG + ".packageList", ""));
	            editor.putBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, sharedPreferences.getBoolean(Constants.LOG_TAG + ".notificationsOnly", true));
	            editor.putBoolean(Constants.PREFERENCE_NOTIFICATION_EXTRA, sharedPreferences.getBoolean(Constants.LOG_TAG + ".fetchNotificationExtras", false));
	            editor.commit();

	            //clear out all old preferences
	            editor = sharedPreferences.edit();
	            editor.clear();
	            editor.commit();
	            Toast.makeText(getActivity(), "Converted your old settings", Toast.LENGTH_SHORT).show();
	        }
	        this.addPreferencesFromResource(R.xml.preferences);
	        
	 }
	        
	        

	        
	    @Override
	    public void onPause(){
	        File watchFile = new File(getActivity().getFilesDir() + "PrefsChanged.none");
	        if (!watchFile.exists()) {
	            try {
	                watchFile.createNewFile();
	            } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
	            watchFile.setLastModified(System.currentTimeMillis());
	        }
	        super.onPause();
	    }
	
}

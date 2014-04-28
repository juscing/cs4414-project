package edu.virginia.cs.va2j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import edu.virginia.cs.va2j.Constants.Mode;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;



public class PickListFragment extends Fragment {
	
	ListView lvpackages;
	//TextView          tvTaskerNotice; don't know if need this yet 
    Constants.Mode    mMode;
    Spinner           spMode;
    SharedPreferences sharedPreferences;
    Handler           mHandler;
   	
	
	private View rootview;
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static PickListFragment newInstance(int sectionNumber) {
		PickListFragment fragment = new PickListFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	public PickListFragment() {
	}
	
	@Override
	public void onResume() {
		super.onResume();
		sharedPreferences = getActivity().getSharedPreferences(Constants.LOG_TAG+"_preferences", getActivity().MODE_MULTI_PROCESS | getActivity().MODE_PRIVATE);
    	
    }
	
	@Override
    public void onPause() {
		super.onPause();
        save();
    }
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        save();
        super.onSaveInstanceState(outState);
    }
	
	public void save() {
        String selectedPackages = "";
        ArrayList<String> tmpArray = new ArrayList<String>();
        if (lvpackages == null || lvpackages.getAdapter() == null) {
            return;
        }
        for (String strPackage : ((packageAdapter) lvpackages.getAdapter()).selected) {
            if (!strPackage.isEmpty()) {
                if (!tmpArray.contains(strPackage)) {
                    tmpArray.add(strPackage);
                    selectedPackages += strPackage + ",";
                }
            }
        }
        tmpArray.clear();
        tmpArray = null;
        if (!selectedPackages.isEmpty()) {
            selectedPackages = selectedPackages.substring(0, selectedPackages.length() - 1);
        }
        /*
        if (Constants.IS_LOGGABLE) {
            switch (mMode) {
            case OFF:
                Log.i(Constants.LOG_TAG, "Mode is: off");
                break;
            case EXCLUDE:
                Log.i(Constants.LOG_TAG, "Mode is: exclude");
                break;
            case INCLUDE:
                Log.i(Constants.LOG_TAG, "Mode is: include");
                break;
            }

            Log.i(Constants.LOG_TAG, "Package list is: " + selectedPackages);
        }
		*/
        //if (mode == Mode.STANDARD) {
        if(true) {
            Editor editor = sharedPreferences.edit();
            //editor.putInt(Constants.PREFERENCE_MODE, mMode.ordinal());
            editor.putString(Constants.PREFERENCE_PACKAGE_LIST, selectedPackages);
            //editor.putString(Constants.PREFERENCE_PKG_RENAMES, arrayRenames.toString());

            // we saved via the application, reset the variable if it exists
            editor.remove(Constants.PREFERENCE_TASKER_SET);

            // clear out legacy preference, if it exists
            editor.remove(Constants.PREFERENCE_EXCLUDE_MODE);

            // save!
            editor.commit();

            // notify service via file that it needs to reload the preferences
            File watchFile = new File(getActivity().getFilesDir() + "PrefsChanged.none");
            if (!watchFile.exists()) {
                try {
                    watchFile.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            watchFile.setLastModified(System.currentTimeMillis());
        } /* else if (mode == Mode.LOCALE) {
            if (!isCanceled()) {
                final Intent resultIntent = new Intent();
                final Bundle resultBundle = new Bundle();

                // set the version, title and body
                resultBundle.putInt(Constants.BUNDLE_EXTRA_INT_VERSION_CODE,
                        Constants.getVersionCode(getApplicationContext()));
                resultBundle.putInt(Constants.BUNDLE_EXTRA_INT_TYPE, Constants.Type.SETTINGS.ordinal());
                resultBundle.putInt(Constants.BUNDLE_EXTRA_INT_MODE, mMode.ordinal());
                resultBundle.putString(Constants.BUNDLE_EXTRA_STRING_PACKAGE_LIST, selectedPackages);
                resultBundle.putString(Constants.BUNDLE_EXTRA_STRING_PKG_RENAMES, arrayRenames.toString());
                String blurb = "";
                switch (mMode) {
                case OFF:
                    blurb = getResources().getStringArray(R.array.mode_choices)[0];
                    break;
                case INCLUDE:
                    blurb = getResources().getStringArray(R.array.mode_choices)[2];
                    break;
                case EXCLUDE:
                    blurb = getResources().getStringArray(R.array.mode_choices)[1];
                }
                Log.i(Constants.LOG_TAG, resultBundle.toString());

                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);
                setResult(RESULT_OK, resultIntent);
            }
        }*/

    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.pick_apps_fragment, container,
				false);
		this.rootview = rootView;
		lvpackages = (ListView) rootView.findViewById(R.id.listPackages); 
		//loadApps
		checkAccessibilityService();
		
		if (rootview.findViewById(R.id.listPackages).isEnabled()) {
            new LoadAppsTask().execute();
        }
		
		rootView.findViewById(R.id.tvAccessibilityError).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
		return rootView;
	}
	
	
	public void checkAccessibilityService() {
        int accessibilityEnabled = 0;
        boolean accessibilityFound = false;
        try {
            accessibilityEnabled = Settings.Secure.getInt(this.getActivity().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (SettingNotFoundException e) {
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(this.getActivity().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();
                    if (accessabilityService.equalsIgnoreCase(Constants.ACCESSIBILITY_SERVICE)) {
                        accessibilityFound = true;
                        break;
                    }
                }
            }
        }
        if (!accessibilityFound) {
            rootview.findViewById(R.id.tvAccessibilityError).setVisibility(View.VISIBLE);
            //rootview.findViewById(R.id.spMode).setVisibility(View.GONE);
            rootview.findViewById(android.R.id.empty).setVisibility(View.GONE);
            rootview.findViewById(R.id.listPackages).setEnabled(false);
            /*
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "The accessibility service is NOT on!");
            }
            */

        } else {
        	rootview.findViewById(R.id.tvAccessibilityError).setVisibility(View.GONE);
        	//rootview.findViewById(R.id.spMode).setVisibility(View.VISIBLE);
        	rootview.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        	rootview.findViewById(R.id.listPackages).setEnabled(true);
            /*
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "The accessibility service is on!");
            }
            */
        }
    }
		
	private class LoadAppsTask extends AsyncTask<Void, Integer, Void> {
        public ArrayList<String> selected;
        List<PackageInfo> pkgAppsList;
        List<ApplicationInfo> appsList;
        JSONArray jsonRenames;
        

        @Override
        protected void onPreExecute(){
            PackageManager pm = getActivity().getPackageManager();
            try{
                pkgAppsList = pm.getInstalledPackages(0);
            } catch (RuntimeException e){
                //this is usually thrown when people have too many things installed (or bloatware in the case of Samsung devices)
                pm = getActivity().getPackageManager();
                appsList = pm.getInstalledApplications(0);
            }
        }
        
        @Override
        protected Void doInBackground(Void... unused) {
            if (pkgAppsList == null && appsList == null) {
                //something went really bad here
                return null;
            }
            if (appsList == null) {
                appsList = new ArrayList<ApplicationInfo>();
                for(PackageInfo pkg : pkgAppsList){
                    appsList.add(pkg.applicationInfo);
                }
            }
            AppComparator comparer = new AppComparator(getActivity());
            Collections.sort(appsList, comparer);
            selected = new ArrayList<String>();
            String packageList;
            
                if(Constants.IS_LOGGABLE){
                    Log.i(Constants.LOG_TAG, "I am pulling from sharedPrefs");
                }
                packageList = sharedPreferences.getString(Constants.PREFERENCE_PACKAGE_LIST, "");
                //packageRenames = sharedPreferences.getString(Constants.PREFERENCE_PKG_RENAMES, "[]");
            /*
            if(Constants.IS_LOGGABLE){
                Log.i(Constants.LOG_TAG, "Package list is: " + packageList);
            }
            */
            
            for (String strPackage : packageList.split(",")) {
                // only add the ones that are still installed, providing cleanup
                // and faster speeds all in one!
                for (ApplicationInfo info : appsList) {
                    if (info.packageName.equalsIgnoreCase(strPackage)) {
                        selected.add(strPackage);
                    }
                }
            }
            
           return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if (appsList == null) {
                //something went wrong
                return;
            }
            rootview.findViewById(android.R.id.empty).setVisibility(View.GONE);
            lvpackages.setAdapter(new packageAdapter(getActivity(), appsList
                    .toArray(new ApplicationInfo[appsList.size()]), selected));

            lvpackages.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
                    AdapterView.AdapterContextMenuInfo contextInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
                    int position = contextInfo.position;
                    long id = contextInfo.id;
                    // the child view who's info we're viewing (should be equal to v)
                    View v = contextInfo.targetView;
                    MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.list_application_menu, menu);
                    ListViewHolder viewHolder = (ListViewHolder) v.getTag();

                }
            });

        }
    }
	
	
	
	
	
	
	private class packageAdapter extends ArrayAdapter<ApplicationInfo> implements OnCheckedChangeListener, OnClickListener {
        private final Context       context;
        private final PackageManager pm;
        private final ApplicationInfo[] packages;
        public ArrayList<String>    selected;

        public packageAdapter(Context context, ApplicationInfo[] packages, ArrayList<String> selected) {
            super(context, R.layout.list_application_item, packages);
            this.context = context;
            this.pm = context.getPackageManager();
            this.packages = packages;
            this.selected = selected;
        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent) {
            ListViewHolder viewHolder = null;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.list_application_item, null, false);

                viewHolder = new ListViewHolder();

                viewHolder.textView = (TextView) rowView.findViewById(R.id.tvPackage);
                viewHolder.imageView = (ImageView) rowView.findViewById(R.id.ivIcon);
                viewHolder.chkEnabled = (CheckBox) rowView.findViewById(R.id.chkEnabled);
                viewHolder.chkEnabled.setOnCheckedChangeListener(this);


                rowView.setOnClickListener(this);
                //really wish we didn't have to do this, but if we don't the rowview will gobble this event up.
                rowView.setOnCreateContextMenuListener(null);
                rowView.setTag(viewHolder);
            } else {
                viewHolder = (ListViewHolder) rowView.getTag();
            }
            ApplicationInfo info = packages[position];

            String appName = null;
            //viewHolder.renamed = false;
            
            try {
            	/*
                for(int i = 0; i < arrayRenames.length(); i++){
                    if(arrayRenames.getJSONObject(i).getString("pkg").equalsIgnoreCase(info.packageName)){
                        viewHolder.renamed = true;
                        appName = arrayRenames.getJSONObject(i).getString("to");
                        viewHolder.textView.setTag(appName);
                        break;
                    }
                }*/
                //if(!viewHolder.renamed){
                    appName = info.loadLabel(pm).toString();
                //}
            } catch (NullPointerException e ){
                appName = null;
            }
			
            if(appName != null){
                viewHolder.textView.setText(appName);
            } else {
                viewHolder.textView.setText("");
            }
            Drawable icon;
            try {
                icon = info.loadIcon(pm);
            } catch (NullPointerException e){
                icon = null;
            }
            if(icon != null){
                viewHolder.imageView.setImageDrawable(icon);
            }
            viewHolder.chkEnabled.setTag(info.packageName);

            boolean boolSelected = false;

            for (String strPackage : selected) {
                if (info.packageName.equalsIgnoreCase(strPackage)) {

                    boolSelected = true;
                    break;
                }
            }
            viewHolder.chkEnabled.setChecked(boolSelected);

            return rowView;
        }

        @Override
        public void onCheckedChanged(CompoundButton chkEnabled, boolean newState) {

            String strPackage = (String) chkEnabled.getTag();

            if (strPackage.isEmpty()) {
                return;
            }
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Check changed on " + strPackage);
            }
            if (newState) {
                if (!selected.contains(strPackage)) {
                    selected.add(strPackage);
                }
            } else {
                while (selected.contains(strPackage)) {
                    selected.remove(strPackage);
                }
            }
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Selected count is: " + String.valueOf(selected.size()));
            }

        }

        @Override
        public void onClick(View rowView) {
            ((CheckBox) rowView.findViewById(R.id.chkEnabled)).performClick();

        }

    }
	
	
	
	
	
	
	
	
	
	public static class ListViewHolder {
        public boolean renamed;
        public TextView  textView;
        public CheckBox  chkEnabled;
        public ImageView imageView;
        public ListViewHolder(){
            renamed = false;
        }
    }
	
	public class AppComparator implements Comparator<ApplicationInfo> {
        final PackageManager pm;
        public AppComparator(Context context){
            this.pm = context.getPackageManager();
        }

        @Override
        public int compare(ApplicationInfo leftPackage, ApplicationInfo rightPackage) {

            String leftName = leftPackage.loadLabel(pm).toString();
            String rightName = rightPackage.loadLabel(pm).toString();

            return leftName.compareToIgnoreCase(rightName);
        }
    }
}


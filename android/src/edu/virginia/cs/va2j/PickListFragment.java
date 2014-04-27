package edu.virginia.cs.va2j;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class PickListFragment extends Fragment {
	
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.pick_apps_fragment, container,
				false);
		this.rootview = rootView;
		checkAccessibilityService();
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
            rootview.findViewById(R.id.spMode).setVisibility(View.GONE);
            rootview.findViewById(R.id.tvMode).setVisibility(View.GONE);
            rootview.findViewById(android.R.id.empty).setVisibility(View.GONE);
            rootview.findViewById(R.id.listPackages).setEnabled(false);
            /*
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "The accessibility service is NOT on!");
            }
            */

        } else {
        	rootview.findViewById(R.id.tvAccessibilityError).setVisibility(View.GONE);
        	rootview.findViewById(R.id.spMode).setVisibility(View.VISIBLE);
        	rootview.findViewById(R.id.tvMode).setVisibility(View.VISIBLE);
        	rootview.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        	rootview.findViewById(R.id.listPackages).setEnabled(true);
            /*
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "The accessibility service is on!");
            }
            */
        }
    }
}

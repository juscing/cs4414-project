package edu.virginia.cs.va2j;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcelable;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.TextView;


public class SprebbleService extends AccessibilityService {

	// Intents
	public static final String INTENT_SEND_PEBBLE_NOTIFICATION = "com.getpebble.action.SEND_NOTIFICATION";

	// Pebble specific items
	public static final String PEBBLE_MESSAGE_TYPE_ALERT = "PEBBLE_ALERT";

	// LOGGING TAG
	private static final String TAG = "SprebbleService";
	
	private boolean  notifications_only     = false;
    private boolean  no_ongoing_notifs      = false;
    private boolean  notifScreenOn          = true;
	
    private long     min_notification_wait  = 0 * 1000;
    private long     notification_last_sent = 0;
    
    private JSONArray ignores               = new JSONArray();
    
    private File     watchFile;
    private Long     lastChange;

    private String[] packages               = null;
    
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		
		if (watchFile.lastModified() > lastChange) {
            loadPrefs();
        }
        
		if (event == null) {
			return;
		}
		
		// handle if they only want notifications
		
        if (notifications_only) {
            if (event != null) {
                Parcelable parcelable = event.getParcelableData();
                if (!(parcelable instanceof Notification)) {

                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG,
                                "Event is not a notification and notifications only is enabled. Returning.");
                    }
                    return;
                }
            }
        }
        if (no_ongoing_notifs){
            Parcelable parcelable = event.getParcelableData();
            if (parcelable instanceof Notification) {
                Notification notif = (Notification) parcelable;
                if ((notif.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT){
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG,
                                "Event is a notification, notification flag contains ongoing, and no ongoing notification is true. Returning.");
                    }
                    return;
                }
            } else {
                if (Constants.IS_LOGGABLE) {
                    Log.i(Constants.LOG_TAG,
                            "Event is not a notification.");
                }
            }
        }
		
     // Handle the do not disturb screen on settings
        PowerManager powMan = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (Constants.IS_LOGGABLE) {
            Log.d(Constants.LOG_TAG, "NotificationService.onAccessibilityEvent: notifScreenOn=" + notifScreenOn
                    + "  screen=" + powMan.isScreenOn());
        }
        if (!notifScreenOn && powMan.isScreenOn()) {
            return;
        }
        
		// main logic
		PackageManager pm = getPackageManager();

		String eventPackageName;
		if (event.getPackageName() != null){
			eventPackageName = event.getPackageName().toString();
		} else {
			eventPackageName = "";
		}

		// include only functionality
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "Mode is set to include only");
        }
        
        boolean found = false;
        for (String packageName : packages) {
            if (packageName.equalsIgnoreCase(eventPackageName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            Log.i(Constants.LOG_TAG, eventPackageName + " was not found in the include list. Returning.");
            return;
        }
		
		// get the title
		String title = "";
		try {
			boolean renamed = false;
			/*
            for(int i = 0; i < pkg_renames.length(); i++){
                if(pkg_renames.getJSONObject(i).getString("pkg").equalsIgnoreCase(eventPackageName)){
                    renamed = true;
                    title = pkg_renames.getJSONObject(i).getString("to");
                }
            }
			 */
			if(!renamed){
				title = pm.getApplicationLabel(pm.getApplicationInfo(eventPackageName, 0)).toString();
			}
		} catch (Exception e) {
			title = eventPackageName;
		}


		// get the notification text
		String notificationText = event.getText().toString();
		// strip the first and last characters which are [ and ]
		notificationText = notificationText.substring(1, notificationText.length() - 1);

		// Normally set from prefs
		boolean notification_extras = true;

		if (notification_extras) {

			Parcelable parcelable = event.getParcelableData();
			if (parcelable instanceof Notification) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					notificationText += "\n" + getExtraBigData((Notification) parcelable, notificationText.trim());
				} else {
					notificationText += "\n" + getExtraData((Notification) parcelable, notificationText.trim());
				}

			}
		}
		
		
		/*
		
		// Check ignore lists
        for(int i = 0; i < ignores.length(); i++){
            try{
                JSONObject ignore = ignores.getJSONObject(i);
                String app = ignore.getString("app");
                boolean exclude = ignore.optBoolean("exclude", true);
                boolean case_insensitive = ignore.optBoolean("insensitive", true);
                if((!app.equals("-1")) && (!eventPackageName.equalsIgnoreCase(app))){
                    //this rule doesn't apply to all apps and this isn't the app we're looking for.
                    continue;
                }
                String regex = "";
                if(case_insensitive){
                    regex += "(?i)";
                }
                if(!ignore.getBoolean("raw")){
                    regex += Pattern.quote(ignore.getString("match"));
                } else {
                    regex += ignore.getString("match");
                }
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(notificationText);
                if(m.find()){
                    if(exclude){
                        if (Constants.IS_LOGGABLE) {
                            Log.i(Constants.LOG_TAG, "Notification text of '" + notificationText + "' matches: '" + regex +"' and exclude is on. Returning");
                        }
                        return;
                    }
                } else {
                    if(!exclude){
                        if(Constants.IS_LOGGABLE){
                            Log.i(Constants.LOG_TAG, "Notification text of '" + notificationText + "' does not match: '" + regex +"' and include is on. Returning");
                        }
                        return;
                    }

                }
            } catch (JSONException e){
                continue;
            }
        }
		
		*/
		
		
		
		
		// Build an intent and send it to the other service
		Intent intent = new Intent(this, SendToPebbleService.class);
		intent.putExtra("notify", notificationText);
		startService(intent);
	}
	
	
	
	@Override
	public void onInterrupt() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onServiceConnected() {
		
		watchFile = new File(getFilesDir() + "PrefsChanged.none");
        if (!watchFile.exists()) {
            try {
                watchFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            watchFile.setLastModified(System.currentTimeMillis());
        }
        loadPrefs();
		
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
		info.notificationTimeout = 100;
		setServiceInfo(info);
	}

	private String getExtraData(Notification notification, String existing_text) {
		RemoteViews views = notification.contentView;
		if (views == null) {
			return "";
		}

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		try {
			ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
			views.reapply(getApplicationContext(), localView);
			return dumpViewGroup(0, localView, existing_text);
		} catch (android.content.res.Resources.NotFoundException e) {
			return "";
		} catch (RemoteViews.ActionException e) {
			return "";
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private String getExtraBigData(Notification notification, String existing_text) {
		RemoteViews views = null;
		try {
			views = notification.bigContentView;
		} catch (NoSuchFieldError e) {
			return getExtraData(notification, existing_text);
		}
		if (views == null) {
			return getExtraData(notification, existing_text);
		}
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		try {
			ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
			views.reapply(getApplicationContext(), localView);
			return dumpViewGroup(0, localView, existing_text);
		} catch (android.content.res.Resources.NotFoundException e) {
			return "";
		}
	}

	private String dumpViewGroup(int depth, ViewGroup vg, String existing_text) {
		String text = "";
		for (int i = 0; i < vg.getChildCount(); ++i) {
			View v = vg.getChildAt(i);
			if (v.getId() == android.R.id.title || v instanceof android.widget.Button
					|| v.getClass().toString().contains("android.widget.DateTimeView")) {
				if (existing_text.isEmpty() && v.getId() == android.R.id.title) {

				} else {
					continue;
				}
			}

			if (v instanceof TextView) {
				TextView tv = (TextView) v;
				if (tv.getText().toString() == "..." || tv.getText().toString() == "�"
						|| isInteger(tv.getText().toString())
						|| tv.getText().toString().trim().equalsIgnoreCase(existing_text)) {
					continue;
				}
				text += tv.getText().toString() + "\n";
			}
			if (v instanceof ViewGroup) {
				text += dumpViewGroup(depth + 1, (ViewGroup) v, existing_text);
			}
		}
		return text;
	}

	public boolean isInteger(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void loadPrefs() {
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "I am loading preferences");
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
        //if old preferences exist, convert them.
        if(sharedPreferences.contains(Constants.LOG_TAG + ".mode")){
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
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Converted preferences to new format. Old ones should be completely gone.");
            }

        }

        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG,
                    "Service package list is: " + sharedPref.getString(Constants.PREFERENCE_PACKAGE_LIST, ""));
        }

        packages = sharedPref.getString(Constants.PREFERENCE_PACKAGE_LIST, "").split(",");
        notifications_only = sharedPref.getBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, true);
        no_ongoing_notifs = sharedPref.getBoolean(Constants.PREFERENCE_NO_ONGOING_NOTIF, false);
        min_notification_wait = sharedPref.getInt(Constants.PREFERENCE_MIN_NOTIFICATION_WAIT, 0) * 1000;
        
        notifScreenOn = sharedPref.getBoolean(Constants.PREFERENCE_NOTIF_SCREEN_ON, true);
        
        /*try{
            converts = new JSONArray(sharedPref.getString(Constants.PREFERENCE_CONVERTS, "[]"));
        } catch (JSONException e){
            converts = new JSONArray();
        }*/
        try{
            ignores = new JSONArray(sharedPref.getString(Constants.PREFERENCE_IGNORE, "[]"));
        } catch (JSONException e){
            ignores = new JSONArray();
        }
        /*
        try{
            pkg_renames = new JSONArray(sharedPref.getString(Constants.PREFERENCE_PKG_RENAMES, "[]"));
        } catch (JSONException e){
            pkg_renames = new JSONArray();
        }
        */
        //we only need to pull this if quiet hours are enabled. Save the cycles for the cpu! (haha)
        /*
        if(quiet_hours){
            String[] pieces = sharedPref.getString(Constants.PREFERENCE_QUIET_HOURS_BEFORE, "00:00").split(":");
            quiet_hours_before= new Date(0, 0, 0, Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
            pieces = sharedPref.getString(Constants.PREFERENCE_QUIET_HOURS_AFTER, "23:59").split(":");
            quiet_hours_after = new Date(0, 0, 0, Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
        }
		*/
        lastChange = watchFile.lastModified();
    }
}

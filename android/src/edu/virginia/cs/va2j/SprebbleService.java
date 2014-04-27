package edu.virginia.cs.va2j;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcelable;
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
	
	
	
	

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (event == null) {
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

	/*
    private void sendToPebble(String title, String notificationText) {
        title = title.trim();
        notificationText = notificationText.trim();
        if (title.trim().isEmpty() || notificationText.isEmpty()) {
            return;
        }
        for(int i = 0; i < converts.length(); i++){
            String from;
            String to;
            try{
                JSONObject convert = converts.getJSONObject(i);
                from = "(?i)" + Pattern.quote(convert.getString("from"));
                to = convert.getString("to");
            } catch (JSONException e){
                continue;
            }
            //not sure if the title should be replaced as well or not. I'm guessing not
            //title = title.replaceAll(from, to);
            notificationText = notificationText.replaceAll(from, to);
        }

        // Create json object to be sent to Pebble
        final Map<String, Object> data = new HashMap<String, Object>();

        data.put("title", title);

        data.put("body", notificationText);
        final JSONObject jsonData = new JSONObject(data);
        final String notificationData = new JSONArray().put(jsonData).toString();

        // Create the intent to house the Pebble notification
        final Intent i = new Intent(INTENT_SEND_PEBBLE_NOTIFICATION);
        i.putExtra("messageType", PEBBLE_MESSAGE_TYPE_ALERT);
        i.putExtra("sender", getString(R.string.app_name));
        i.putExtra("notificationData", notificationData);


        sendBroadcast(i);
        notification_last_sent = System.currentTimeMillis();

    }
	 */
}

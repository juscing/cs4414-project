package edu.virginia.cs.va2j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

import com.getpebble.android.kit.*;
import com.getpebble.android.kit.util.*;

public class SprebbleService extends AccessibilityService {

	// Intents
	public static final String INTENT_SEND_PEBBLE_NOTIFICATION = "com.getpebble.action.SEND_NOTIFICATION";

	// Pebble specific items
	public static final String PEBBLE_MESSAGE_TYPE_ALERT = "PEBBLE_ALERT";

	// Our UUID
	public static final UUID app_uuid = UUID.fromString("5901b974-bea9-45c5-bcba-81658d112f01");

	// LOGGING TAG
	private static final String TAG = "SprebbleService";
	
	private static final int PEBBLE_MESSAGE_notificationText = 0x10;
	private static final int PEBBLE_MESSAGE_END = 0x40;
	private static final int PEBBLE_OUTGOING_BYTES = 64;
	
	private int ind = 0;
    private int max = 0;
    private boolean fin = false;

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
		
		final String notify = notificationText;
		
		if(PebbleKit.isWatchConnected(getApplicationContext())) {
			// Launch our pebble app
			//PebbleKit.startAppOnPebble(this.getApplicationContext(), app_uuid);

			// Build the Pebble Dictionary
			/*
			PebbleDictionary data = new PebbleDictionary();
			data.addString(0, title);
			data.addString(1, notificationText);
			*/
			
			max = notificationText.length() / PEBBLE_OUTGOING_BYTES + (notificationText.length() % PEBBLE_OUTGOING_BYTES > 0 ? 1 : 0);
			PebbleKit.registerReceivedAckHandler(getApplicationContext(), new PebbleKit.PebbleAckReceiver(app_uuid) {
				@Override
				public void receiveAck(Context context, int transactionId) {
					Log.i(TAG, "Received ack for transaction " + transactionId);
					if (!fin) {
						ind++;
						if (ind == max) {
							Log.i(TAG, "Finished sending messages.");
							fin = true;
							ind = 0;
							max = 0;
							sendPebbleData(PEBBLE_MESSAGE_END, "End.");
						}
						else {
							sendPebbleString(PEBBLE_MESSAGE_notificationText, notify);
						}
					}
				}
			});

			PebbleKit.registerReceivedNackHandler(getApplicationContext(), new PebbleKit.PebbleNackReceiver(app_uuid) {
				@Override
				public void receiveNack(Context context, int transactionId) {
					Log.i(TAG, "Received nack for transaction " + transactionId);
					if (!fin) {
						sendPebbleString(PEBBLE_MESSAGE_notificationText, notify);
					}
				}
			});

			// Send that on to the Pebble
			//PebbleKit.sendDataToPebble(getApplicationContext(), app_uuid, data);

		}
		/*
		sendToPebble(title, notificationText);
		 */

	}
	
	private void sendPebbleString(int upperByte, String data) {
        int begin = ind*PEBBLE_OUTGOING_BYTES;
        int end = (ind+1)*PEBBLE_OUTGOING_BYTES;
        if(end > data.length())
            end = data.length();
        String str = data.substring(begin, end);
        PebbleDictionary dataDict = new PebbleDictionary();
        dataDict.addString(upperByte << 24 | ind, str);
        Log.d("SpritzPebble", "Starting Pebble app.");
        PebbleKit.startAppOnPebble(getApplicationContext(), app_uuid);
        Log.d("SpritzPebble", "Sending data to Pebble app.");
        PebbleKit.sendDataToPebble(getApplicationContext(), app_uuid, dataDict);
    }

    private void sendPebbleData(int upperByte, String data)
    {
        int nummsgs = data.length() / PEBBLE_OUTGOING_BYTES + (data.length() % PEBBLE_OUTGOING_BYTES > 0 ? 1 : 0);
        for(int i = 0; i < nummsgs; ++i) {
            int begin = i*PEBBLE_OUTGOING_BYTES;
            int end = (i+1)*PEBBLE_OUTGOING_BYTES;
            if(end > data.length())
                end = data.length();
            String str = data.substring(begin, end);
            PebbleDictionary dataDict = new PebbleDictionary();
            dataDict.addString(upperByte << 24 | i, str);
            Log.d("SpritzPebble", "Starting Pebble app.");
            PebbleKit.startAppOnPebble(getApplicationContext(), app_uuid);
            Log.d("SpritzPebble", "Sending data to Pebble app.");
            PebbleKit.sendDataToPebble(getApplicationContext(), app_uuid, dataDict);
        }
    }
    private void sendPebbleData(int upperByte, byte[] data)
    {
        int nummsgs = data.length / PEBBLE_OUTGOING_BYTES + (data.length % PEBBLE_OUTGOING_BYTES > 0 ? 1 : 0);
        for(int i = 0; i < nummsgs; ++i) {
            int begin = i*PEBBLE_OUTGOING_BYTES;
            int end = (i+1)*PEBBLE_OUTGOING_BYTES;
            if(end > data.length)
                end = data.length;
            byte[] sendData = Arrays.copyOfRange(data,begin,end);
            PebbleDictionary dataDict = new PebbleDictionary();
            dataDict.addBytes(upperByte << 24 | i, sendData);
            Log.d("SpritzPebble", "Starting Pebble app.");
            PebbleKit.startAppOnPebble(getApplicationContext(), app_uuid);
            Log.d("SpritzPebble", "Sending data to Pebble app.");
            PebbleKit.sendDataToPebble(getApplicationContext(), app_uuid, dataDict);
        }
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

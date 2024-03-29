package edu.virginia.cs.va2j;

import java.util.Arrays;
import java.util.UUID;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class SendToPebbleService extends IntentService {

	private int ind = 0;
	private int max = 0;
	private boolean fin = false;

	private static final int PEBBLE_MESSAGE_WORDS = 0x10;
	private static final int PEBBLE_MESSAGE_notificationText = 0x10;
	private static final int PEBBLE_MESSAGE_END = 0x40;
	private static final int PEBBLE_OUTGOING_BYTES = 64;

	// LOGGING TAG
	private static final String TAG = "SendToPebbleService";

	// Our UUID
	public static final UUID app_uuid = UUID.fromString("5901b974-bea9-45c5-bcba-81658d112f01");

	// receivers
	private BroadcastReceiver ack;
	private BroadcastReceiver nack;

	// Exit flag
	private long done = 0;
	private int count = 0;
	
	public SendToPebbleService() {
		super("SendToPebbleService");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//this.unregisterReceiver(ack);
		//this.unregisterReceiver(nack);
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		String notificationText = intent.getStringExtra("notify");
		notificationText = notificationText.replaceAll("\\s+", " ");
		final String notify = notificationText;
		//String 

		if(PebbleKit.isWatchConnected(getApplicationContext())) {

			max = notificationText.length() / PEBBLE_OUTGOING_BYTES + (notificationText.length() % PEBBLE_OUTGOING_BYTES > 0 ? 1 : 0);
			ack = PebbleKit.registerReceivedAckHandler(getApplicationContext(), new PebbleKit.PebbleAckReceiver(app_uuid) {
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

			nack = PebbleKit.registerReceivedNackHandler(getApplicationContext(), new PebbleKit.PebbleNackReceiver(app_uuid) {
				@Override
				public void receiveNack(Context context, int transactionId) {
					Log.i(TAG, "Received nack for transaction " + transactionId);
					if (!fin) {
						sendPebbleString(PEBBLE_MESSAGE_notificationText, notify);
					}
				}
			});

			sendPebbleString(PEBBLE_MESSAGE_WORDS, notify);
			
			PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(app_uuid) {
				@Override
				public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
					// Log.i("SendToPebbleService", "Received value=" + data.getUnsignedInteger(0) + " for key: 0");
					done = data.getInteger(0);
					PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
				}
			});
			
			while(done == 0 && count < 10) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				count++;
			}
			done = 0; count = 0;
		}
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

}

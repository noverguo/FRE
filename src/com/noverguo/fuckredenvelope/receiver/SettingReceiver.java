package com.noverguo.fuckredenvelope.receiver;

import com.noverguo.fuckredenvelope.Settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SettingReceiver extends BroadcastReceiver {
	public static final String ACTION_TALKS = "com.noverguo.fuckredenvelope.receiver.SettingReceiver";
	public static final String KEY_TALKS = "key_talks";
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction() == null) {
			return;
		}
		String action = intent.getAction();
		if(ACTION_TALKS.equals(action)) {
			String[] talks = intent.getStringArrayExtra(KEY_TALKS);
			if(talks != null) {
				Settings.setTalks(talks);
			}
		}
	}

}

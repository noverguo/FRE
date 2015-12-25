package com.nv.fre.receiver;

import java.util.Arrays;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nv.fre.Settings;
import com.nv.fre.TalkSel;

public class SettingReceiver extends BroadcastReceiver {
	public static final String ACTION_TALKS = "com.nv.fuckredenvelope.receiver.SettingReceiver";
	public static final String KEY_TALKS = "key_talks";
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction() == null) {
			return;
		}
		String action = intent.getAction();
		Log.e("SR", "SettingReceiver: " + action);
		if(ACTION_TALKS.equals(action)) {
			String[] talks = intent.getStringArrayExtra(KEY_TALKS);
			Log.e("SR", "SettingReceiver talks: " + talks);
			if(talks != null) {
				Log.e("SR", "SettingReceiver talks: " + Arrays.asList(talks));
				String[] oldTalks = Settings.getTalks();
				if(oldTalks != null) {
					for(int i=0;i<talks.length;++i) {
						TalkSel ts = new TalkSel(talks[i]);
						if(ts.talkName == null) {
							Log.e("SR", "SettingReceiver null:" + talks[i]);
							talks[i] = null;
						}
						for(String oldTalk : oldTalks) {
							if(ts.equals(new TalkSel(oldTalk))) {
								talks[i] = oldTalk;
								break;
							}
						}
					}
				}
				Settings.setTalks(talks);
			}
		}
	}

}

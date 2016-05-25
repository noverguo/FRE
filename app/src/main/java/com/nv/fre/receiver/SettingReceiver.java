package com.nv.fre.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.nv.fre.Settings;
import com.nv.fre.TalkSel;
import com.nv.fre.utils.SizeUtils;

import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;
import rx.Observable;

public class SettingReceiver extends BroadcastReceiver {
	public static final String ACTION_TALKS = "com.nv.fre.receiver.SettingReceiver.ACTION_TALKS";
	public static final String KEY_TALKS = "key_talks";
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction() == null) {
			return;
		}
		String action = intent.getAction();
		if(ACTION_TALKS.equals(action)) {
			String talks = intent.getStringExtra(KEY_TALKS);
			if(!TextUtils.isEmpty(talks)) {
				Settings.setTalksString(talks);
			}
		}
	}

    public static void updateTalks(Context context, List<TalkSel> talks) {
        if (SizeUtils.isEmpty(talks)) {
            return;
        }
        context.sendBroadcast(new Intent(SettingReceiver.ACTION_TALKS).putExtra(SettingReceiver.KEY_TALKS, TalkSel.listToString(talks)));
    }

}

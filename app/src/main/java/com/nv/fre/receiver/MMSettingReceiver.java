package com.nv.fre.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import com.nv.fre.TalkSel;
import com.nv.fre.mm.MMContext;
import com.nv.fre.mm.MMSettings;
import com.nv.fre.utils.SizeUtils;

import java.util.List;

/**
 * Created by noverguo on 2016/5/20.
 */
public class MMSettingReceiver extends BroadcastReceiver {
    public static final String ACTION_TALKS = "com.nv.fre.mm.MMHook.ACTION_TALKS";
    public static final String KEY_TALKS = "com.nv.fre.mm.MMHook.KEY_TALKS";
    public static final String KEY_HOOK_ALL = "com.nv.fre.mm.MMHook.KEY_HOOK_ALL";
    public static final String KEY_DISPLAY_ALL = "com.nv.fre.mm.MMHook.KEY_DISPLAY_ALL";
    Callback callback;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_TALKS.equals(intent.getAction())) {
            boolean hookAll = intent.getBooleanExtra(KEY_HOOK_ALL, true);
            boolean displayJustRE = intent.getBooleanExtra(KEY_DISPLAY_ALL, true);
            MMSettings.setHookAll(context, hookAll);
            MMSettings.setDisplayJustRE(context, displayJustRE);
            List<TalkSel> talkSels;
            final String talks = intent.getStringExtra(KEY_TALKS);
            if (TextUtils.isEmpty(talks)) {
                talkSels = null;
            } else {
                talkSels = TalkSel.stringToList(talks);
                MMSettings.setTalks(context, talkSels);
            }
            callback.onReceive(hookAll, displayJustRE, talkSels);

        }
    }

    private MMSettingReceiver(Callback callback) {
        this.callback = callback;
    }

    public static void register(Context context, Callback callback) {
        IntentFilter intentFilter = new IntentFilter(ACTION_TALKS);
        context.registerReceiver(new MMSettingReceiver(callback), intentFilter);
    }

    public void unregister(Context context) {
        context.unregisterReceiver(this);
    }

    public static void setSetting(Context context, boolean hookAll, boolean displayJustRE, List<TalkSel> talkSels) {
        Intent intent = new Intent(ACTION_TALKS).putExtra(KEY_HOOK_ALL, hookAll).putExtra(KEY_DISPLAY_ALL, displayJustRE);
        if (!SizeUtils.isEmpty(talkSels)) {
            intent.putExtra(KEY_TALKS, TalkSel.listToString(talkSels));
        }
        context.sendBroadcast(intent);
    }

    public interface Callback {
        void onReceive(boolean hookAll, boolean displayJustRE, List<TalkSel> talkSels);
    }

}

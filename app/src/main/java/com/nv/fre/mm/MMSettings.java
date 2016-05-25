package com.nv.fre.mm;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.nv.fre.FREApplication;
import com.nv.fre.TalkSel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class MMSettings {
    private static final String PRE_NAME = "mm_setting";
    private static final String PRE_KEY_HOOK_ALL = "ha";
    private static final String PRE_KEY_DISPLAY_ALL = "da";
    private static final String PRE_KEY_TALKERS = "ts";
    public static List<TalkSel> getTalks(final Context context) {
        String strTalks = getTalksString(context);
        if(!TextUtils.isEmpty(strTalks)) {
            return TalkSel.stringToList(strTalks);
        }
        return Collections.emptyList();
    }

    public static String getTalksString(Context context) {
        return getPref(context).getString(PRE_KEY_TALKERS, null);
    }

    public static void setTalks(final Context context, final List<TalkSel> talks) {
        setTalksString(context, TalkSel.listToString(talks));
    }

    public static void setTalksString(Context context, final String talks) {
        getPref(context).edit().putString(PRE_KEY_TALKERS, talks).commit();
    }

    public static boolean isHookAll(Context context) {
        return getPref(context).getBoolean(PRE_KEY_HOOK_ALL, true);
    }

    public static void setHookAll(Context context, boolean hookAll) {
        getPref(context).edit().putBoolean(PRE_KEY_HOOK_ALL, hookAll).commit();
    }

    public static boolean isDisplayJustRE(Context context) {
        return getPref(context).getBoolean(PRE_KEY_DISPLAY_ALL, false);
    }

    public static void setDisplayJustRE(Context context, boolean hookAll) {
        getPref(context).edit().putBoolean(PRE_KEY_DISPLAY_ALL, hookAll).commit();
    }

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
    }
}

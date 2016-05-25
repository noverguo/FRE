package com.nv.fre;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class Settings {
	private static final String PRE_NAME = "fre_setting";
	private static final String PRE_KEY_HOOK_ALL = "ha";
	private static final String PRE_KEY_DISPLAY_ALL = "da";
	private static final String PRE_KEY_TALKERS = "ts";
	public static Observable<List<TalkSel>> getTalks() {
        return Observable.create(new Observable.OnSubscribe<List<TalkSel>>() {
            @Override
            public void call(Subscriber<? super List<TalkSel>> subscriber) {
                String strTalks = getTalksString();
                if(!TextUtils.isEmpty(strTalks)) {
                    subscriber.onNext(TalkSel.stringToList(strTalks));
                }
                subscriber.onCompleted();
            }
        });
	}

    public static String getTalksString() {
        return getPref().getString(PRE_KEY_TALKERS, null);
    }
	
	public static void setTalks(final List<TalkSel> talks) {
        Schedulers.io().createWorker().schedule(new Action0() {
            @Override
            public void call() {
                setTalksString(TalkSel.listToString(talks));
            }
        });
	}

    public static void setTalksString(final String talks) {
        getPref().edit().putString(PRE_KEY_TALKERS, talks).commit();
    }
	
	public static boolean isHookAll() {
		return getPref().getBoolean(PRE_KEY_HOOK_ALL, true);
	}
	
	public static void setHookAll(boolean hookAll) {
        getPref().edit().putBoolean(PRE_KEY_HOOK_ALL, hookAll).commit();
	}

	public static boolean isDisplayJustRE() {
		return getPref().getBoolean(PRE_KEY_DISPLAY_ALL, false);
	}

	public static void setDisplayJustRE(boolean hookAll) {
        getPref().edit().putBoolean(PRE_KEY_DISPLAY_ALL, hookAll).commit();
	}

    private static SharedPreferences getPref() {
        return FREApplication.getContext().getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
    }
}

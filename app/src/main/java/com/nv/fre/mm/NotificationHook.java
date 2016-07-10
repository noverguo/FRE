package com.nv.fre.mm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.nv.fre.BuildConfig;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class NotificationHook {
	/**
	 * 消息不提醒功能
	 * 
	 * @param hi
	 */
	public static void hookNoChange(MMContext hi) {
		NotificationHook ph = new NotificationHook();
		ph.hook(hi);
	}
	private Set<PendingIntent> hideIntentFilters = new HashSet<>();
	/**
	 * 禁止上传crash异常
	 * 
	 * @param hi
	 */
	private void hook(final MMContext hi) {
		XposedBridge.hookAllMethods(PendingIntent.class, "getActivity", new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				Intent intent = null;
				for (Object obj : param.args) {
					if (obj != null && obj instanceof Intent) {
						intent = (Intent) obj;
						break;
					}
				}
				if (intent == null) {
					return;
				}
				String talker = intent.getStringExtra("Main_User");
				if (BuildConfig.DEBUG) XposedBridge.log("PendingIntent.getActivity: " + talker + ", " + hi.grepTalks.containsKey(talker));
				if (hi.hookAll || !hi.grepTalks.containsKey(talker)) {
					return;
				}
				if (hi.allMsgs.size() > 0) {
					Msg msg = hi.allMsgs.valueAt(hi.allMsgs.size()-1);
					if (BuildConfig.DEBUG) XposedBridge.log("need intercept: " + (msg.isRE && msg.talker != null && msg.talker.equals(talker)));
					if (msg.isRE && msg.talker != null && msg.talker.equals(talker)) {
						return;
					}
				}
				if (BuildConfig.DEBUG) XposedBridge.log("PendingIntent.getActivity: " + talker + ", " + hi.grepTalks.containsKey(talker) + ", " + hi.grepTalks.get(talker).hideNotification);
				if (!hi.grepTalks.get(talker).hideNotification) {
					return;
				}
				if (param.getResult() != null) {
					final PendingIntent contentIntent = (PendingIntent) param.getResult();
					synchronized (hideIntentFilters) {
						hideIntentFilters.add(contentIntent);
					}
					hi.bgHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							synchronized (hideIntentFilters) {
								if (!hideIntentFilters.contains(contentIntent)) {
									return;
								}
								hideIntentFilters.remove(contentIntent);
							}
						}
					}, 1000);
				}
			}
		});
		XposedBridge.hookAllMethods(NotificationManager.class, "notify", new MM_MethodHook(){
			@Override
			public void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
				Notification notification;
				if (param.args[1] instanceof Notification) {
					notification = (Notification) param.args[1];
				} else if (param.args[2] instanceof Notification) {
					notification = (Notification) param.args[2];
				} else {
					return;
				}
				PendingIntent contentIntent = notification.contentIntent;
				if (BuildConfig.DEBUG) XposedBridge.log("NotificationManager.notify: " + contentIntent + ", " + hideIntentFilters.contains(contentIntent));
				synchronized (hideIntentFilters) {
					if (!hideIntentFilters.contains(contentIntent)) {
						return;
					}
					hideIntentFilters.remove(contentIntent);
				}
				param.setResult(null);
			}
		});
	}
}

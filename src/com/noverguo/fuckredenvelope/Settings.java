package com.noverguo.fuckredenvelope;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
	private static final String PRE_NAME = "setting";
	private static final String PRE_KEY_HOOK_ALL = "ha";
	private static final String PRE_KEY_TALKERS = "ts";
	public static String[] getTalks() {
		SharedPreferences sp = FREApplication.getContext().getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
		String strTalks = sp.getString(PRE_KEY_TALKERS, null);
		if(strTalks == null) {
			return null;
		}
		return strTalks.split(";");
	}
	
	public static void setTalks(String[] talks) {
		if(talks == null) {
			return;
		}
		StringBuilder buf = new StringBuilder();
		for(String talk : talks) {
			if(talk == null) {
				continue;
			}
			buf.append(talk).append(";");
		}
		if(buf.length() == 0) {
			return;
		}
		buf.subSequence(0, buf.length() - 1);
		SharedPreferences sp = FREApplication.getContext().getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
		sp.edit().putString(PRE_KEY_TALKERS, buf.toString()).commit();
	}
	
	public static boolean isHookAll() {
		SharedPreferences sp = FREApplication.getContext().getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
		return sp.getBoolean(PRE_KEY_HOOK_ALL, true);
	}
	
	public static void setHookAll(boolean hookAll) {
		SharedPreferences sp = FREApplication.getContext().getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
		sp.edit().putBoolean(PRE_KEY_HOOK_ALL, hookAll).commit();
	}
}

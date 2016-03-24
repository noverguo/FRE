package com.nv.fre.mm;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.nv.fre.FREApplication;

import java.util.Arrays;

import de.robv.android.xposed.XposedBridge;

public class MMSettings {
	private static final String PRE_NAME = "setting";
	private static final String PRE_KEY_TALKERS = "ts";
	private static final String PRE_KEY_DISPLAY_ALL = "da";
	public static String[] getTalks(Context context) {
		SharedPreferences sp = context.getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
		String strTalks = sp.getString(PRE_KEY_TALKERS, null);
//		XposedBridge.log("getTalks: " + strTalks + ", " + (strTalks == null ? "" : Arrays.asList(strTalks).toString()));
		if(TextUtils.isEmpty(strTalks)) {
			return null;
		}
		return strTalks.split(";");
	}
	
	public static void setTalks(Context context, String[] talks) {
//		XposedBridge.log("setTalks: " + talks + ", " + (talks == null ? "" : Arrays.asList(talks).toString()));
		StringBuilder buf = new StringBuilder();
		if(talks != null && talks.length > 0) {
			for (String talk : talks) {
				if (talk == null) {
					continue;
				}
				buf.append(talk).append(";");
			}
		}
		if(buf.length() != 0) {
			buf.subSequence(0, buf.length() - 1);
		}
		SharedPreferences sp = context.getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
		sp.edit().putString(PRE_KEY_TALKERS, buf.toString()).commit();
	}

	public static boolean isDisplayAll(Context context) {
		SharedPreferences sp = context.getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
		return sp.getBoolean(PRE_KEY_DISPLAY_ALL, true);
	}

	public static void setDisplayAll(Context context, boolean hookAll) {
		SharedPreferences sp = context.getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
		sp.edit().putBoolean(PRE_KEY_DISPLAY_ALL, hookAll).commit();
	}
}

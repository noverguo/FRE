package com.nv.fre.mm;

import com.nv.fre.BuildConfig;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class MM_MethodHook extends XC_MethodHook {
	public static boolean inHook = false;

	@Override
	final public void beforeHookedMethod(MethodHookParam param) throws Throwable {
		if (inHook) {
			return;
		}
		inHook = true;
		try {
			MM_beforeHookedMethod(param);
		} catch(Throwable e) {
			if(BuildConfig.DEBUG) XposedBridge.log(e);
		}
		inHook = false;
	}

	@Override
	final public void afterHookedMethod(MethodHookParam param) throws Throwable {
		if (inHook) {
			return;
		}
		inHook = true;
		try {
			MM_afterHookedMethod(param);
		} catch(Throwable e) {
			if(BuildConfig.DEBUG) XposedBridge.log(e);
		}
		inHook = false;
	}

	@Override
	final public void call(Param param) throws Throwable {
		if (inHook) {
			return;
		}
		inHook = true;
		try {
			MM_call(param);
		} catch(Throwable e) {
			if(BuildConfig.DEBUG) XposedBridge.log(e);
		}
		inHook = false;
	}

	public void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
	}

	public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
	}

	public void MM_call(Param param) throws Throwable {
	}
}
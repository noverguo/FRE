package com.noverguo.fuckredenvelope.mm;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class MM_MethodHook extends XC_MethodHook {
	public static boolean inHook = false;

	@Override
	final protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		if (inHook) {
			return;
		}
		inHook = true;
		try {
			MM_beforeHookedMethod(param);
		} catch(Throwable e) {
			XposedBridge.log(e);
		}
		inHook = false;
	}

	@Override
	final protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		if (inHook) {
			return;
		}
		inHook = true;
		try {
			MM_afterHookedMethod(param);
		} catch(Throwable e) {
			XposedBridge.log(e);
		}
		inHook = false;
	}

	@Override
	final protected void call(Param param) throws Throwable {
		if (inHook) {
			return;
		}
		inHook = true;
		try {
			MM_call(param);
		} catch(Throwable e) {
			XposedBridge.log(e);
		}
		inHook = false;
	}

	protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
	}

	protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
	}

	protected void MM_call(Param param) throws Throwable {
	}
}
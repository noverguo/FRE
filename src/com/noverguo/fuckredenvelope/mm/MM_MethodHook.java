package com.noverguo.fuckredenvelope.mm;

import de.robv.android.xposed.XC_MethodHook;

public class MM_MethodHook extends XC_MethodHook {
	public static boolean inHook = false;

	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		if (inHook) {
			return;
		}
		inHook = true;
		MM_beforeHookedMethod(param);
		inHook = false;
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		if (inHook) {
			return;
		}
		inHook = true;
		MM_afterHookedMethod(param);
		inHook = false;
	}

	@Override
	protected void call(Param param) throws Throwable {
		if (inHook) {
			return;
		}
		inHook = true;
		MM_call(param);
		inHook = false;
	}

	protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
	}

	protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
	}

	protected void MM_call(Param param) throws Throwable {
	}

}
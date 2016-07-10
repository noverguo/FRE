package com.nv.fre.mm;

import android.media.AudioAttributes;
import android.os.Vibrator;

import com.nv.fre.BuildConfig;
import com.nv.fre.utils.ClassUtils;
import com.nv.fre.utils.XposedUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class VibratorHook {
	/**
	 * 消息不提醒功能
	 * 
	 * @param hi
	 */
	public static void hookNoChange(MMContext hi) {
		VibratorHook ph = new VibratorHook();
		ph.hook(hi);
	}

	/**
	 * 禁止上传crash异常
	 * 
	 * @param hi
	 */
	private void hook(final MMContext hi) {
		final XC_MethodHook methodHook = new MM_MethodHook() {
			@Override
			public void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (BuildConfig.DEBUG) XposedBridge.log("Vibrator.vibrate: " + (System.currentTimeMillis() < hi.virbateDisableTime.get()));
				if (System.currentTimeMillis() < hi.virbateDisableTime.get()) {
					hi.virbateDisableTime.set(0);
					param.setResult(null);
				}
			}
		};
		XposedBridge.hookAllConstructors(Vibrator.class, new MM_MethodHook(){
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.hookAllMethods(Vibrator.class, "vibrate", methodHook);
				XposedUtils.findAndHookMethod(param.thisObject.getClass(), "vibrate", long.class, methodHook);
				XposedUtils.findAndHookMethod(param.thisObject.getClass(), "vibrate", long[].class, int.class, methodHook);
//				XposedUtils.findAndHookMethod(ClassUtils.getDeclaredMethodClass(param.thisObject.getClass(), "vibrate", long[].class, int.class), "vibrate", methodHook);
//				XposedUtils.findAndHookMethod(ClassUtils.getDeclaredMethodClass(param.thisObject.getClass(), "vibrate", long[].class, int.class, AudioAttributes.class), "vibrate", methodHook);
			}
		});
//		XposedHelpers.findAndHookMethod(Vibrator.class, "vibrate", long[].class, int.class, methodHook);
//		XposedHelpers.findAndHookMethod(Vibrator.class, "vibrate", long[].class, int.class, AudioAttributes.class, methodHook);
	}
}

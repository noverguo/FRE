package com.nv.fre.mm;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;

import android.telephony.TelephonyManager;

import com.nv.fre.TelephonyProperties;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class PropertiesHook {
	/**
	 * 防止封号
	 * 
	 * @param hi
	 */
	public static void hookPreventCheck(HookInfo hi) {
		PropertiesHook ph = new PropertiesHook();
		ph.hookUnableUploadException(hi);
		ph.hookReadImeiAndImsi(hi);
	}

	UncaughtExceptionHandler justLogHanlder = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
//			XposedBridge.log("UncaughtExceptionHandler: " + paramThrowable);
		}
	};
	UncaughtExceptionHandler oldDefaultHandler;
	Map<Thread, UncaughtExceptionHandler> oldHandlerMap = new HashMap<Thread, UncaughtExceptionHandler>();
	/**
	 * 禁止上传crash异常
	 * 
	 * @param hi
	 */
	private void hookUnableUploadException(HookInfo hi) {
		XposedHelpers.findAndHookMethod(Thread.class, "setDefaultUncaughtExceptionHandler", UncaughtExceptionHandler.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				super.MM_afterHookedMethod(param);
				UncaughtExceptionHandler oh = (UncaughtExceptionHandler) param.getResult();
				if(oh != null) {
					param.setResult(justLogHanlder);
					oldDefaultHandler = oh;
				}
			}
		});
		XposedHelpers.findAndHookMethod(Thread.class, "getDefaultUncaughtExceptionHandler", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(oldDefaultHandler);
			}
		});
		XposedHelpers.findAndHookMethod(Thread.class, "setUncaughtExceptionHandler", UncaughtExceptionHandler.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				super.MM_afterHookedMethod(param);
				UncaughtExceptionHandler oh = (UncaughtExceptionHandler) param.getResult();
				if(oh != null) {
					param.setResult(justLogHanlder);
					oldHandlerMap.put((Thread) param.thisObject, oh);
				} else {
					oldHandlerMap.remove(param.thisObject);
				}
			}
		});
		XposedHelpers.findAndHookMethod(Thread.class, "getUncaughtExceptionHandler", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if(oldHandlerMap.containsKey(param.thisObject)) {
					param.setResult(oldHandlerMap.get(param.thisObject));
				} else {
					param.setResult(null);
				}
			}
		});
	}
	
	XC_MethodHook hookImei = new MM_MethodHook() {
		@Override
		protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
			param.setResult("");
		}
	};
	
	MM_MethodHook hookGetProperties = new MM_MethodHook() {
		@Override
		protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
			String key = (String) param.args[0];
			if (TelephonyProperties.PROPERTY_IMEI.equals(key)) {
				param.setResult("");
			}
		}
	};

	/**
	 * 禁止读imei和imsi
	 * 
	 * @param hi
	 */
	private void hookReadImeiAndImsi(HookInfo hi) {
		// 1）获取运营商sim卡imsi号：
		// String android_imsi = telephonyManager.getSubscriberId();//获取手机IMSI号
		// String IMSI =
		// android.os.SystemProperties.get(android.telephony.TelephonyProperties.PROPERTY_IMSI);
		// 2）获取IME标识两种方法(手机唯一的标识)
		// String imei = ((TelephonyManager)
		// context.getSystemService(TELEPHONY_SERVICE)).getDeviceId();
		// String IMEI =
		// android.os.SystemProperties.get(android.telephony.TelephonyProperties.PROPERTY_IMEI)
		XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSubscriberId", hookImei);
		XposedHelpers.findAndHookMethod(TelephonyManager.class, "getDeviceId", hookImei);
		XposedHelpers.findAndHookMethod("android.os.SystemProperties", hi.classLoader, "get", String.class, hookGetProperties);
		XposedHelpers.findAndHookMethod("android.os.SystemProperties", hi.classLoader, "get", String.class, String.class, hookGetProperties);
	}

}

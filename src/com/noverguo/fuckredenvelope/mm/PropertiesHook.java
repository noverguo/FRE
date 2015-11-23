package com.noverguo.fuckredenvelope.mm;

import android.telephony.TelephonyManager;

import com.noverguo.fuckredenvelope.TelephonyProperties;

import de.robv.android.xposed.XposedHelpers;

public class PropertiesHook {
	/**
	 * 禁止读imei和imsi
	 * @param hookInfo
	 */
	public static void hookReadImeiAndImsi(HookInfo hookInfo) {
		// 1）获取运营商sim卡imsi号：
		// String android_imsi = telephonyManager.getSubscriberId();//获取手机IMSI号
		// String IMSI =
		// android.os.SystemProperties.get(android.telephony.TelephonyProperties.PROPERTY_IMSI);
		// 2）获取IME标识两种方法(手机唯一的标识)
		// String imei = ((TelephonyManager)
		// context.getSystemService(TELEPHONY_SERVICE)).getDeviceId();
		// String IMEI =
		// android.os.SystemProperties.get(android.telephony.TelephonyProperties.PROPERTY_IMEI)
		XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSubscriberId", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult("");
			}
		});
		XposedHelpers.findAndHookMethod(TelephonyManager.class, "getDeviceId", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult("");
			}
		});
		MM_MethodHook getPropertiesHook = new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				String key = (String) param.args[0];
				if (TelephonyProperties.PROPERTY_IMEI.equals(key)) {
					param.setResult("");
				}
			}
		};
		XposedHelpers.findAndHookMethod("android.os.SystemProperties", hookInfo.classLoader, "get", String.class, getPropertiesHook);
		XposedHelpers.findAndHookMethod("android.os.SystemProperties", hookInfo.classLoader, "get", String.class, String.class, getPropertiesHook);
	}

}

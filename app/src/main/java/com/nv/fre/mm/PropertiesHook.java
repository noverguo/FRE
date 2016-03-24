package com.nv.fre.mm;

import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.nv.fre.Const;
import com.nv.fre.TelephonyProperties;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class PropertiesHook {
	/**
	 * 防止封号
	 * 
	 * @param hi
	 */
	public static void hookPreventCheck(MMContext hi) {
		PropertiesHook ph = new PropertiesHook();
		ph.hookUnableUploadException(hi);
		ph.hookReadImeiAndImsi(hi);
		ph.hookReadPackage(hi);
	}

	UncaughtExceptionHandler justLogHanlder = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
//			//XposedBridge.log("UncaughtExceptionHandler: " + paramThrowable);
		}
	};
	UncaughtExceptionHandler oldDefaultHandler;
	Map<Thread, UncaughtExceptionHandler> oldHandlerMap = new HashMap<Thread, UncaughtExceptionHandler>();
	/**
	 * 禁止上传crash异常
	 * 
	 * @param hi
	 */
	private void hookUnableUploadException(MMContext hi) {
		XposedHelpers.findAndHookMethod(Thread.class, "setDefaultUncaughtExceptionHandler", UncaughtExceptionHandler.class, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
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
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(oldDefaultHandler);
			}
		});
		XposedHelpers.findAndHookMethod(Thread.class, "setUncaughtExceptionHandler", UncaughtExceptionHandler.class, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
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
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
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
		public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
			param.setResult("");
		}
	};
	
	MM_MethodHook hookGetProperties = new MM_MethodHook() {
		@Override
		public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
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
	private void hookReadImeiAndImsi(MMContext hi) {
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

	private XC_MethodHook hookPackageInfoGrep = new MM_MethodHook() {
		@Override
		public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
			List<PackageInfo> packageInfoList = (List<PackageInfo>) param.getResult();
			if(packageInfoList != null && packageInfoList.size() > 0) {
				List<PackageInfo> newPackageInfoList = new ArrayList<>();
				for(PackageInfo pi : packageInfoList) {
					if(!Const.PACKAGE_NAME.equals(pi.packageName)) {
						newPackageInfoList.add(pi);
					} else {
						//XposedBridge.log("PackageInfo be hook: " + Const.PACKAGE_NAME);
					}
				}
				param.setResult(newPackageInfoList);
			}
		}
	};

	private XC_MethodHook hookResolveInfoGrep = new MM_MethodHook() {
		@Override
		public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
			List<ResolveInfo> resolveInfoList = (List<ResolveInfo>) param.getResult();
			if(resolveInfoList != null && resolveInfoList.size() > 0) {
				List<ResolveInfo> newResolveInfoList = new ArrayList<>();
				for(ResolveInfo ri : resolveInfoList) {
					if(ri.activityInfo != null) {
						if (Const.PACKAGE_NAME.equals(ri.activityInfo.packageName)) {
							//XposedBridge.log("ResolveInfo be hook: " + Const.PACKAGE_NAME);
							continue;
						}
					}
					if(ri.serviceInfo != null) {
						if (Const.PACKAGE_NAME.equals(ri.serviceInfo.packageName)) {
							//XposedBridge.log("ResolveInfo be hook: " + Const.PACKAGE_NAME);
							continue;
						}
					}
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
						if (ri.providerInfo != null) {
							if (Const.PACKAGE_NAME.equals(ri.providerInfo.packageName)) {
								//XposedBridge.log("ResolveInfo be hook: " + Const.PACKAGE_NAME);
								continue;
							}
						}
					}
					newResolveInfoList.add(ri);
				}
				param.setResult(newResolveInfoList);
			}
		}
	};

	/**
	 * 禁止读到当前应用
	 * @param hi
     */
	private void hookReadPackage(MMContext hi) {
		XposedHelpers.findAndHookMethod(ContextWrapper.class, "getPackageManager", new MM_MethodHook() {
			private boolean init = false;
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!init && param.getResult() != null) {
					init = true;
					hookPackageManager((Class<? extends PackageManager>) param.getResult().getClass());
				}
			}
		});


		// 过滤pm命令
		ShellHook.hookExec(hi, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				String[] cmds = (String[]) param.args[0];
				if(cmds == null || cmds.length == 0) {
					return;
				}
				boolean foundPM = false;
				boolean foundL = false;
				boolean foundList = false;
				boolean foundPackages = false;
				// pm -l
				// pm list packages
				for(String cmd : cmds) {
					if(cmd == null) {
						continue;
					}
					cmd = cmd.trim();
					if(cmd.equals("pm")) {
						foundPM = true;
					} else if(foundPM && cmd.equals("-l")) {
						foundL = true;
					} else if(foundPM && "list".equals(cmd)) {
						foundList = true;
					} else if(foundList && cmd.equals("packages")) {
						foundPackages = true;
					}
				}
				if(!(foundPM && (foundL || (foundList && foundPackages)))) {
					return;
				}
				Process process = (Process) param.getResult();
				param.setResult(new ShellHook.ProcessWrapper(process) {
					@Override
					public InputStream getInputStream() {
						InputStream in = super.getInputStream();
						ByteArrayOutputStream grepOut = new ByteArrayOutputStream();
						try {
							List<String> lines = IOUtils.readLines(in, "UTF-8");
							if(lines != null) {
								for(String line : lines) {
									if(line.contains("com.nv.fre")) {
										//XposedBridge.log("shell exec be hook: " + Const.PACKAGE_NAME);
										continue;
									}
									grepOut.write(line.getBytes("UTF-8"));
									grepOut.write("\n".getBytes("UTF-8"));
								}
							}
							in.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return new ByteArrayInputStream(grepOut.toByteArray());
					}
				});
			}
		});
	}

	private void hookPackageManager(Class<? extends PackageManager> packageManagerClass) {
		// 过滤 PackageManager.getInstalledApplications(int flags) 方法
		XposedHelpers.findAndHookMethod(packageManagerClass, "getInstalledApplications", int.class, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				List<ApplicationInfo> applicationInfoList = (List<ApplicationInfo>) param.getResult();
				if(applicationInfoList != null && applicationInfoList.size() > 0) {
					List<ApplicationInfo> newApplicationInfoList = new ArrayList<>();
					for(ApplicationInfo ai : applicationInfoList) {
						if(!Const.PACKAGE_NAME.equals(ai.packageName)) {
							newApplicationInfoList.add(ai);
						} else {
							//XposedBridge.log("ApplicationInfo be hook: " + Const.PACKAGE_NAME);
						}
					}
					param.setResult(newApplicationInfoList);
				}
			}
		});

		// 过滤 PackageManager.getInstalledPackages(int flags) 方法
		XposedHelpers.findAndHookMethod(packageManagerClass, "getInstalledPackages", int.class, hookPackageInfoGrep);

		// 过滤 PackageManager.getPackagesHoldingPermissions(String[] permissions, int flags) 方法
		XposedHelpers.findAndHookMethod(packageManagerClass, "getPackagesHoldingPermissions", String[].class, int.class, hookPackageInfoGrep);

		// 过滤 PackageManager.getPreferredPackages(int flags) 方法
		XposedHelpers.findAndHookMethod(packageManagerClass, "getPreferredPackages", int.class, hookPackageInfoGrep);

		// 过滤 PackageManager.queryBroadcastReceivers(Intent intent, int flags) 方法
		XposedHelpers.findAndHookMethod(packageManagerClass, "queryBroadcastReceivers", Intent.class, int.class, hookResolveInfoGrep);

		// 过滤 PackageManager.queryIntentActivities(Intent intent, int flags) 方法
		XposedHelpers.findAndHookMethod(packageManagerClass, "queryIntentActivities", Intent.class, int.class, hookResolveInfoGrep);

		// 过滤 PackageManager.queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags) 方法
		XposedHelpers.findAndHookMethod(packageManagerClass, "queryIntentActivityOptions", ComponentName.class, Intent[].class, Intent.class, int.class, hookResolveInfoGrep);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			// 过滤 PackageManager.queryIntentContentProviders(Intent intent, int flags) 方法
			XposedHelpers.findAndHookMethod(packageManagerClass, "queryIntentContentProviders", Intent.class, int.class, hookResolveInfoGrep);
		}

		// 过滤 PackageManager.queryIntentServices(Intent intent, int flags) 方法
		XposedHelpers.findAndHookMethod(packageManagerClass, "queryIntentServices", Intent.class, int.class, hookResolveInfoGrep);

		// 过滤 PackageManager.queryContentProviders (String processName, int uid, int flags) 方法
		XposedHelpers.findAndHookMethod(packageManagerClass, "queryContentProviders", String.class, int.class, int.class, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				List<ProviderInfo> providerInfoListInfoList = (List<ProviderInfo>) param.getResult();
				if(providerInfoListInfoList != null && providerInfoListInfoList.size() > 0) {
					List<ProviderInfo> newProviderInfoList = new ArrayList<>();
					for(ProviderInfo pi : providerInfoListInfoList) {
						if(!Const.PACKAGE_NAME.equals(pi.packageName)) {
							newProviderInfoList.add(pi);
						} else {
							//XposedBridge.log("ApplicationInfo be hook: " + Const.PACKAGE_NAME);
						}
					}
					param.setResult(newProviderInfoList);
				}
			}
		});
	}

}

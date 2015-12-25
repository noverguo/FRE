package com.nv.fre.mm;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MMHook implements IXposedHookLoadPackage {
	HookInfo hi = new HookInfo();
	// 入口
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if (!"com.tencent.mm".equals(lpparam.packageName)) {
			return;
		}
//		XposedBridge.log("find wechat app: " + lpparam.packageName);

		hi.init(lpparam);
		// 不给读imei和imsi，以免封号
		PropertiesHook.hookPreventCheck(hi);
		// 读取消息，发现红包则启动窗口
		IncomeMsgHook.hookReadMsg(hi);
		// 启动窗口后，可能会停在消息列表窗口（具体原因待查），这时需要去点击进入对应的窗口
		CommunicationsHook.hookClickChattingItem(hi);
		// 注入红包相关点击事件
		RedEnvelopeHook.hookRedEnvelopeClickListener(hi);
		// 发现是手慢了或失效，则关闭
		RedEnvelopeHook.hookLateSoClose(hi);
		// 检测消息的View，如发现有红包的View，则进行点击
		ChattingMsgHook.hookMsgView(hi);
		// 聊天UI的生命周期状态改变
		UiLifecycleHook.hookChattingUIStatus(hi);

		
		
		// 检测到红包后需要自动发送消息，以表示对他人的尊重
//		hookAutoSendMsg();
		hookForTest();
	}

	private void hookForTest() throws Exception {
//		XposedBridge.hookAllMethods(PendingIntent.class, "getActivity", new MM_MethodHook() {
//			@Override
//			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
//				if (param.args == null || param.args.length < 3) {
//					return;
//				}
//				Object obj = param.args[2];
//				if (obj == null || !(obj instanceof Intent)) {
//					return;
//				}
//				Intent intent = (Intent) obj;
//				String res = "";
//				for(Object p : param.args) {
//					res += p + "  ";
//				}
//				XposedBridge.log("------------------------------------");
//				XposedBridge.log("getActivity: " + res);
//				XposedBridge.log("getActivity: requestCode" + param.args[1] + ", " + intent.toString());
//				if(param.args[0] != null) {
//					XposedBridge.log("getActivity: requestCode" + param.args[0].getClass().getName() + ", " + intent.toString());
//				}
//				Bundle extras = intent.getExtras();
//				if (extras != null) {
//					XposedBridge.log(extras.toString());
//				}
//				XposedBridge.log("------------------------------------");
//			}
//		});
//		XposedBridge.hookAllMethods(ContextWrapper.class, "startActivity", startActivityCallback);
//		XposedBridge.hookAllMethods(ContextWrapper.class, "startActivityAsUser", startActivityCallback);
//		XposedBridge.hookAllMethods(Activity.class, "startActivityForResult", startActivityCallback);
//		android.support.v4.app.Fragment.a(Context paramContext, String paramString, Bundle paramBundle)
//		
//		XposedHelpers.findAndHookMethod("android.support.v4.app.Fragment", hi.classLoader, "onCreate", Bundle.class, new MM_MethodHook() {
//			@Override
//			protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("Fragment onCreate: " + param.args[0] + " <- " + param.thisObject.getClass().getName());
//			}
//		});
//		
//		XposedBridge.hookAllConstructors(hi.classLoader.loadClass("android.support.v4.app.Fragment"), new MM_MethodHook() {
//			@Override
//			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("Fragment new: " + param.thisObject.getClass().getName());
//				try {
//				XposedHelpers.findAndHookMethod(param.thisObject.getClass().getSuperclass(), "onPause", new MM_MethodHook(){
//					@Override
//					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//						XposedBridge.log("onPause super: " + param.thisObject.getClass().getName());
//					}
//				});
//				} catch(Exception e) {
//					e.printStackTrace();
//				}
//				
//				try {
//				XposedHelpers.findAndHookMethod(param.thisObject.getClass().getSuperclass(), "onResume", new MM_MethodHook(){
//					@Override
//					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//						XposedBridge.log("onResume super: " + param.thisObject.getClass().getName());
//					}
//				});
//				} catch(Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//		
//		
//		XposedHelpers.findAndHookMethod("android.support.v4.app.Fragment", hi.classLoader, "onPause", new MM_MethodHook() {
//			@Override
//			protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("Fragment onPause: " + " <- " + param.thisObject.getClass().getName());
//			}
//		});
//		
//		XposedHelpers.findAndHookMethod("android.support.v4.app.Fragment", hi.classLoader, "onResume", new MM_MethodHook() {
//			@Override
//			protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("Fragment onResume: " + " <- " + param.thisObject.getClass().getName());
//			}
//		});
	}
//
//	MM_MethodHook startActivityCallback = new MM_MethodHook() {
//		@Override
//		protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
//			XposedBridge.log("startActivity: " + param.thisObject.getClass().getName() + "." + param.method.getName());
//			if (param.args == null || param.args.length == 0) {
//				return;
//			}
//			Object arg0 = param.args[0];
//			if (!(arg0 instanceof Intent)) {
//				return;
//			}
//			Intent intent = (Intent) arg0;
//			XposedBridge.log("startActivity:\t" + intent.toString());
//			XposedBridge.log("startActivity:\t\t" + intent.getExtras());
//		}
//	};
}
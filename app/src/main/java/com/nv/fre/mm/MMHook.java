package com.nv.fre.mm;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MMHook implements IXposedHookLoadPackage {
	HookInfo hi = new HookInfo();
	// 入口
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		hookForTest(lpparam);
		if (!"com.tencent.mm".equals(lpparam.packageName)) {
			return;
		}
//		XposedBridge.log("find wechat app: " + lpparam.packageName);

		hi.init(lpparam);
		// 不给读imei和imsi，不给读应用信息，防止封号
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
	}

	private void hookForTest(final LoadPackageParam lpparam) throws Exception {
//		if (!"com.tencent.noverguo.readproperties".equals(lpparam.packageName)) {
//			return;
//		}
//		XposedBridge.log("hook readproperties app");
//		PropertiesHook.hookPreventCheck(hi);
	}

}
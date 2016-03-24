package com.nv.fre.mm;

import com.nv.fre.Const;
import com.nv.fre.api.GrpcServer;
import com.nv.fre.mm.itf.Checker;
import com.nv.fre.nano.Fre;
import com.nv.fre.utils.PackageUtils;
import com.nv.fre.utils.UUIDUtils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import io.grpc.stub.StreamObserver;

public class MMHook implements IXposedHookLoadPackage {
	MMContext hi = new MMContext();
	MMChecker allowChecker = new MMChecker();
	MMChecker hookClassesChecker = new MMChecker();
	// 入口
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.isFirstApplication || !Const.MM_PACKAGE_NAME.equals(lpparam.packageName)) {
			return;
		}
		hi.init(lpparam, new MMContext.Callback() {
			@Override
			public void onCreate() {
				if(!lpparam.isFirstApplication) {
					return;
				}
				allowChecker.init(hi, new MMChecker.Callback() {
					@Override
					public void run(final Checker checker) {
						Fre.FuckRequest request = new Fre.FuckRequest();
						request.uuid = UUIDUtils.getUUID(hi.context);
						GrpcServer.fuckMM(request, new StreamObserver<Fre.FuckReply>() {
							@Override
							public void onNext(Fre.FuckReply value) {
//								XposedBridge.log("fuckMM onNext");
								hi.allow = value.allow;
								checker.finish();
							}

							@Override
							public void onError(Throwable t) {
//								XposedBridge.log("fuckMM onError: " + t.getMessage());
								checker.error();
							}

							@Override
							public void onCompleted() {
							}
						});
					}
				});
				hookClassesChecker.init(hi, new MMChecker.Callback() {
					@Override
					public void run(final Checker checker) {
						Fre.GetHookClassesRequest req = new Fre.GetHookClassesRequest();
						req.uuid = UUIDUtils.getUUID(hi.context);
						req.mmVersionCode = PackageUtils.getVersionCode(hi.context, Const.MM_PACKAGE_NAME);
						GrpcServer.getHookClasses(req, new StreamObserver<Fre.GetHookClassesReply>() {
							@Override
							public void onNext(Fre.GetHookClassesReply rsp) {
//								XposedBridge.log("getHookClasses onNext: " + rsp.hookClassesMap);
								if(rsp.support) {
									ConfuseValue.init(rsp.hookClassesMap);
									try {
										hookChange();
										checker.finish();
										return;
									} catch (Exception e) {
									}
								}
								checker.error();
							}

							@Override
							public void onError(Throwable t) {
//								XposedBridge.log("getHookClasses onError");
								checker.error();
							}

							@Override
							public void onCompleted() {
							}
						});
					}
				});
			}
		});

		hookNoChange();
	}


	private void hookNoChange() throws Exception {
		// 读取消息，发现红包则启动窗口
		IncomeMsgHook.hookReadMsg(hi);
		// 不给读imei和imsi，不给读应用信息，防止封号
		PropertiesHook.hookPreventCheck(hi);
		// 固定不变的先hook
		CommunicationsHook.hookNoChange(hi);
	}

	private void hookChange() throws Exception {
		// 启动窗口后，可能会停在消息列表窗口（具体原因待查），这时需要去点击进入对应的窗口
		CommunicationsHook.hookClickChattingItem(hi);
		// 注入红包相关点击事件
		RedEnvelopeHook.hookOnResumeInit(hi);
		// 检测消息的View，如发现有红包的View，则进行点击
		ChattingMsgHook.hookMsgView(hi);
		// 聊天UI的生命周期状态改变
		UiLifecycleHook.hookChattingUIStatus(hi);

		// 检测到红包后需要自动发送消息，以表示对他人的尊重
//		hookAutoSendMsg();
	}
}
package com.nv.fre.mm;

import com.nv.fre.UUIDUtils;
import com.nv.fre.api.GrpcServer;
import com.nv.fre.nano.Fre;
import com.nv.fre.utils.ConnectedHelper;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import io.grpc.stub.StreamObserver;

public class MMHook implements IXposedHookLoadPackage {
	HookInfo hi = new HookInfo();
	// 入口
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if (!"com.tencent.mm".equals(lpparam.packageName)) {
			return;
		}
		hi.init(lpparam, new HookInfo.Callback() {
			@Override
			public void onCreate() {
				try {
					hookMM(lpparam);
				} catch (Exception e) {
					e.printStackTrace();
				}
				postCheckAllow();
			}
		});

		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.LauncherUI", hi.classLoader, "onResume", new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				//XposedBridge.log("allow: " + hi.allow + ", checking: " + checking + ", needCheck: " + needCheck);
				if(!hi.allow && !checking) {
					postCheckAllow();
				}
			}
		});
	}

	private ConnectedHelper connectedHelper = new ConnectedHelper();
	private void registerCheck() {
		connectedHelper.registerConnectedCheck(hi.context, new ConnectedHelper.Callback() {
			@Override
			public boolean canRun() {
				return needCheck && !checking;
			}

			@Override
			public void onConnected() {
				if(canRun()) {
					postCheckAllow();
				}
			}
		});
	}

	private void unregisterCheck() {
		connectedHelper.unregisterConnectedCheck(hi.context);
	}

	private boolean needCheck = false;
	private boolean checking = false;
	private Runnable checkAllowRunnable = new Runnable() {
		@Override
		public void run() {
			checkAllow();
		}
	};
	private void postCheckAllow() {
		checking = false;
		needCheck = false;
		unregisterCheck();
		hi.bgHandler.removeCallbacks(checkAllowRunnable);
		hi.bgHandler.postDelayed(checkAllowRunnable, 8000);
	}

	private void checkAllow() {
		//XposedBridge.log("fuckMM checkAllow");
		checking = true;
		if(!GrpcServer.initHostAndPort()) {
			//XposedBridge.log("fuckMM GrpcServer init failed");
			waitNextCheck();
			return;
		}
		//XposedBridge.log("fuckMM checkAllow start");
		Fre.FuckRequest request = new Fre.FuckRequest();
		request.uuid = UUIDUtils.getUUID(hi.context);
		GrpcServer.fuckMM(request, new StreamObserver<Fre.FuckReply>() {
			@Override
			public void onNext(Fre.FuckReply value) {
				//XposedBridge.log("fuckMM onNext");
				hi.allow = value.allow;
				checking = false;
				// 24小时后重新检测，防止非法使用
				hi.bgHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						GrpcServer.sInit = false;
						needCheck = true;
						registerCheck();
					}
				}, 1000 * 60 * 60 * 24);
			}

			@Override
			public void onError(Throwable t) {
				//XposedBridge.log("fuckMM onError: " + t.getMessage());
				waitNextCheck();
			}

			@Override
			public void onCompleted() {
			}
		});
	}

	private void waitNextCheck() {
		needCheck = true;
		checking = false;
		registerCheck();
	}

	private void hookMM(final LoadPackageParam lpparam) throws Exception {
		// 不给读imei和imsi，不给读应用信息，防止封号
		PropertiesHook.hookPreventCheck(hi);
		// 读取消息，发现红包则启动窗口
		IncomeMsgHook.hookReadMsg(hi);
		// 启动窗口后，可能会停在消息列表窗口（具体原因待查），这时需要去点击进入对应的窗口
		CommunicationsHook.hookClickChattingItem(hi);
		// 注入红包相关点击事件
		RedEnvelopeHook.hookRedEnvelopeClickListener(hi);
		// 检测消息的View，如发现有红包的View，则进行点击
		ChattingMsgHook.hookMsgView(hi);
		// 聊天UI的生命周期状态改变
		UiLifecycleHook.hookChattingUIStatus(hi);
		// 检测到红包后需要自动发送消息，以表示对他人的尊重
//		hookAutoSendMsg();
	}
}
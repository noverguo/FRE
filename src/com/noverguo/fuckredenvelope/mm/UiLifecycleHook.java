package com.noverguo.fuckredenvelope.mm;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class UiLifecycleHook {
	public static AtomicBoolean flag = new AtomicBoolean(false);
	/**
	 * 检测并保存聊天界面的状态
	 * @param hi
	 */
	public static void hookChattingUIStatus(final HookInfo hi) {
		new UiLifecycleHook().hookStatus(hi);
	}
	
	private void hookStatus(final HookInfo hi) {
		// com.tencent.mm.ui.chatting.ChattingUI$a
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.chatting.ChattingUI$a", hi.classLoader, "onResume", new MM_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("onResume: " + param.thisObject.getClass().getName());
				// 当前在聊天室中
				hi.setStayInRoom();
				if(flag.get()) {
					if(hi.chattingListViewAdapter != null && hi.isStayInRoom()) {
						XposedBridge.log("notifyDataSetChanged: " + hi.chattingListViewAdapter.getClass().getName() + ", " + hi.chattingListViewAdapter.getCount());
						hi.chattingListViewAdapter.notifyDataSetChanged();
					}
				}
			}
		});
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.chatting.ChattingUI$a", hi.classLoader, "onPause", new MM_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("onPause: " + param.thisObject.getClass().getName());
				hi.setStayUnknow();
			}
		});
		
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.conversation.e", hi.classLoader, "onResume", new MM_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(hi.stay != HookInfo.STAY_IN_ROOM) {
//					XposedBridge.log("onResume: set stayTalker null" + param.thisObject.getClass().getName());
					hi.stayTalker = null;
				}
			}
		});
	}
	
	
	
}

package com.noverguo.fuckredenvelope.mm;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class UiLifecycleHook {
	public static AtomicBoolean flag = new AtomicBoolean(false);

	/**
	 * 检测并保存聊天界面的状态
	 * 
	 * @param hi
	 * @throws Exception 
	 */
	public static void hookChattingUIStatus(final HookInfo hi) throws Exception {
		new UiLifecycleHook().hookStatus(hi);
	}

	private void hookStatus(final HookInfo hi) throws Exception {
//	public void com.tencent.mm.ui.i.a(String paramString, com.tencent.mm.sdk.g.i parami) {
//		aSm();
//	}
//		final Class<?> cjClass = hi.classLoader.loadClass("com.tencent.mm.ui.chatting.cj");
//		final Method fkMethod = cjClass.getMethod("FK", new Class<?>[0]);
		Class<?> iClass = hi.classLoader.loadClass("com.tencent.mm.ui.i");
		Class<?> giClass = hi.classLoader.loadClass("com.tencent.mm.sdk.g.i");
		final Method aMethod = iClass.getMethod("a", String.class, giClass);
		
		final Runnable notifyDataSetChanged = new Runnable() {
			public void run() {
				if (hi.chattingListViewAdapter != null && hi.isStayInRoom()) {
					XposedBridge.log("notifyDataSetChanged: " + hi.chattingListViewAdapter.getClass().getName() + ", " + hi.chattingListViewAdapter.getCount());
					try {
						aMethod.invoke(hi.chattingListViewAdapter, new Object[]{null, null});
					} catch (Exception e) {
						e.printStackTrace();
					}
//					hi.chattingListViewAdapter.notifyDataSetChanged();
				}
			}
		};
		
		// com.tencent.mm.ui.chatting.ChattingUI$a
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.chatting.ChattingUI$a", hi.classLoader, "onResume", new MM_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				// XposedBridge.log("onResume: " +
				// param.thisObject.getClass().getName());
				// 当前在聊天室中
				hi.setStayInRoom();
				if (flag.get()) {
					if (hi.chattingListViewAdapter != null && hi.isStayInRoom()) {
						hi.postDelayed(notifyDataSetChanged, 8000);
					}
				}
			}
		});
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.chatting.ChattingUI$a", hi.classLoader, "onPause", new MM_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				// XposedBridge.log("onPause: " +
				// param.thisObject.getClass().getName());
				hi.setStayUnknow();
			}
		});

		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.conversation.e", hi.classLoader, "onResume", new MM_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (hi.stay != HookInfo.STAY_IN_ROOM) {
					// XposedBridge.log("onResume: set stayTalker null" +
					// param.thisObject.getClass().getName());
					hi.stayTalker = null;
				}
			}
		});
	}

}

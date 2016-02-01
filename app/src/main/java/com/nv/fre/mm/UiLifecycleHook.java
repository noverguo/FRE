package com.nv.fre.mm;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import com.nv.fre.receiver.UnlockReceiver;

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
		// com.tencent.mm.ui.chatting.ChattingUI$a
		XposedHelpers.findAndHookMethod(HookClasses.getClassName(HookClasses.KEY_CHATTING_WINDOW_CLASS), hi.classLoader, "onResume", new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				// 当前在聊天室中
				hi.setStayInRoom();
			}
		});
		XposedHelpers.findAndHookMethod(HookClasses.getClassName(HookClasses.KEY_CHATTING_WINDOW_CLASS), hi.classLoader, "onPause", new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				hi.setStayUnknow();
			}
		});

		XposedHelpers.findAndHookMethod(HookClasses.getClassName(HookClasses.KEY_CONVERSATION_WINDOW_CLASS), hi.classLoader, "onResume", new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if (hi.stay != HookInfo.STAY_IN_ROOM) {
					hi.stayTalker = null;
				}
			}
		});
		
		XposedHelpers.findAndHookMethod(HookClasses.getClassName(HookClasses.KEY_LAUNCHER_UI_CLASS), hi.classLoader, "onPause", new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				UnlockReceiver.screenLock = true;
			}
		});

		XposedHelpers.findAndHookMethod(HookClasses.getClassName(HookClasses.KEY_LAUNCHER_UI_CLASS), hi.classLoader, "onResume", new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				UnlockReceiver.screenLock = false;
			}
		});
	}

}

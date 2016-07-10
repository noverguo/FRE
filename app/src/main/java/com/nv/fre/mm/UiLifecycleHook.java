package com.nv.fre.mm;

import java.util.concurrent.atomic.AtomicBoolean;

import com.nv.fre.receiver.UnlockReceiver;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class UiLifecycleHook {
	public static AtomicBoolean flag = new AtomicBoolean(false);
    static UiLifecycleHook inst;
    MMContext hi;
    private static void init(MMContext hi) {
        if(inst == null) {
            inst = new UiLifecycleHook();
            inst.hi = hi;
        }
    }
	/**
	 * 检测并保存聊天界面的状态
	 * 
	 * @param hi
	 * @throws Exception
	 */
	public static void hookChattingUIStatus(final MMContext hi) throws Exception {
        init(hi);
		inst.hookStatus();
	}

    MM_MethodHook chattingWindowOnResumeMethodHook = new MM_MethodHook() {
        @Override
        public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
            // 当前在聊天室中
            hi.setStayInRoom();
        }
    };
    MM_MethodHook chattingWindowOnPauseMethodHook = new MM_MethodHook() {
        @Override
        public void MM_afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            hi.setStayUnknow();
        }
    };
    MM_MethodHook conversationWindowOnResumeMethodHook = new MM_MethodHook() {
        @Override
        public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
            if (hi.stay != MMContext.STAY_IN_ROOM) {
                hi.stayTalker = null;
            }
        }
    };

    MM_MethodHook launcherUiOnPauseMethodHook = new MM_MethodHook() {
        @Override
        public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
            UnlockReceiver.screenLock = true;
        }
    };

    MM_MethodHook launcherUiOnResumeMethodHook = new MM_MethodHook() {
        @Override
        public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
            UnlockReceiver.screenLock = false;
        }
    };

	private void hookStatus() throws Exception {
		// com.tencent.mm.ui.chatting.ChattingUI$a
		XposedHelpers.findAndHookMethod(ConfuseValue.getConfuseName(ConfuseValue.KEY_CHATTING_WINDOW_CLASS), hi.classLoader, "onResume", chattingWindowOnResumeMethodHook);
		XposedHelpers.findAndHookMethod(ConfuseValue.getConfuseName(ConfuseValue.KEY_CHATTING_WINDOW_CLASS), hi.classLoader, "onPause", chattingWindowOnPauseMethodHook);

		XposedHelpers.findAndHookMethod(ConfuseValue.getConfuseName(ConfuseValue.KEY_CONVERSATION_WINDOW_CLASS), hi.classLoader, "onResume", conversationWindowOnResumeMethodHook);
		
		XposedHelpers.findAndHookMethod(ConfuseValue.getConfuseName(ConfuseValue.KEY_LAUNCHER_UI_CLASS), hi.classLoader, "onPause", launcherUiOnPauseMethodHook);

		XposedHelpers.findAndHookMethod(ConfuseValue.getConfuseName(ConfuseValue.KEY_LAUNCHER_UI_CLASS), hi.classLoader, "onResume", launcherUiOnResumeMethodHook);
	}

}

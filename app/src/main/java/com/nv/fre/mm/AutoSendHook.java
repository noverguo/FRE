package com.nv.fre.mm;

import android.widget.ImageButton;
import android.widget.TextView;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AutoSendHook {
	private ImageButton switchToTalk;
	private ImageButton switchToKeyboard;

	public void hookAutoSendMsg(MMContext hi) throws Exception {
		final Class<?> mmEditTextClass = hi.classLoader.loadClass("com.tencent.mm.ui.widget.MMEditText");
		XposedHelpers.findAndHookMethod(TextView.class, "handleTextChanged", CharSequence.class, int.class, int.class, int.class, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				TextView tv = (TextView) param.thisObject;
				if (tv.getClass() != mmEditTextClass) {
					return;
				}
//				XposedBridge.log("handleTextChanged: " + tv.getClass().getName());
			}

		});

		XposedBridge.hookAllConstructors(ImageButton.class, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				ImageButton ib = (ImageButton) param.thisObject;
				CharSequence contentDescription = ib.getContentDescription();
				if (contentDescription == null) {
					return;
				}
				if (contentDescription.equals("切换到按住说话")) {
					switchToTalk = ib;
				} else if (contentDescription.equals("切换到键盘")) {
					switchToKeyboard = ib;
				}
			}
		});

	}
}

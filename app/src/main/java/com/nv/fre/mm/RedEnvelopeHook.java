package com.nv.fre.mm;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.nv.fre.Utils;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class RedEnvelopeHook {
	
	/**
	 * 红包相关点击事件
	 * @param hi
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 */
	public static void hookRedEnvelopeClickListener(final HookInfo hi) throws ClassNotFoundException, NoSuchFieldException {
//		final Class callbackClass = classLoader.loadClass("com.tencent.mm.ui.chatting.ck");
		XposedHelpers.findAndHookMethod(View.class, "setOnClickListener", View.OnClickListener.class, new MM_MethodHook() {
			View.OnClickListener clickCallback = null;
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!(param.thisObject instanceof Button)) {
					return;
				}
				View curView = (View) param.thisObject;
				Object[] args = param.args;
				clickCallback = null;
				if (args != null && args.length > 0 && args[0] != null) {
					if (args[0] instanceof View.OnClickListener) {
						clickCallback = (View.OnClickListener) args[0];
					}
				}
				if (clickCallback == null) {
					return;
				}

				hookClickFuckRedEnvelope(param, (Button) curView);
			}
			
			// 点击拆红包
			private void hookClickFuckRedEnvelope(MethodHookParam param, Button btnView) {
				CharSequence text = btnView.getText();
				if (text != null && text.toString().equals("拆红包")) {
//					XposedBridge.log(param.thisObject.getClass().getName() + " --> " + param.args[0].getClass().getName());
					clickCallback.onClick(btnView);
				}
			}
		});
	}
	
	/**
	 * 发现是手慢了或失效的红包，则关闭
	 * @param hi
	 */
	public static void hookLateSoClose(final HookInfo hi) {
//		XposedHelpers.findAndHookMethod(TextView.class, "onDraw", Canvas.class, new MM_MethodHook() {
		XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, BufferType.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!hi.isStarted()) {
					return;
				}
				TextView tv = (TextView) param.thisObject;
				String text = tv.getText().toString();
				if(text == null || (!text.equals("手慢了，红包派完了") && !text.equals("超过1天未领取，红包已失效"))) {
					return;
				}
//				XposedBridge.log("发现 手慢 或 失效的红包");
				
				ViewParent parent = tv.getParent();
				if(!Utils.instanceOf(parent, LinearLayout.class, true)) {
					return;
				}
				parent = parent.getParent();
				if(!Utils.instanceOf(parent, RelativeLayout.class, true)) {
					return;
				}
				RelativeLayout rlParent = (RelativeLayout) parent;
				if(rlParent == null || rlParent.getChildCount() != 5) {
					return;
				}
				View closeView = rlParent.getChildAt(3);
				if(closeView == null || !Utils.instanceOf(closeView, ImageView.class, true)) {
					return;
				}
				closeView.callOnClick();
				hi.setStayInRoom();
				hi.end();
			}
		});
	}
	
	public static void hookCloseDetailRedEnvelope(final HookInfo hi) {
		// com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI
		XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI", hi.classLoader, "onCreate", Bundle.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(final MethodHookParam param) throws Throwable {
				if(!hi.isStarted()) {
					return;
				}
//				XposedBridge.log("查看完红包，点击关闭 ");
				hi.finishActivity((Activity) param.thisObject);
				hi.setStayInRoom();
				hi.end();
			}
		});
	}
}

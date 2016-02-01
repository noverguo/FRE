package com.nv.fre.mm;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nv.fre.utils.ReflectUtil;

import java.lang.reflect.Field;

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
//		XposedHelpers.findAndHookMethod(View.class, "setOnClickListener", View.OnClickListener.class, new MM_MethodHook() {
//			boolean init = false;
//			@Override
//			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
//				if(!init) {
//					try {
//						hookClickRedEnvelope(hi);
//						hookCloseDetailRedEnvelope(hi);
//						init = true;
//					} catch (Exception e) {
//					}
//				}
//
//			}
//		});
		XposedHelpers.findAndHookMethod(HookClasses.getClassName(HookClasses.KEY_CHATTING_WINDOW_CLASS), hi.classLoader, "onResume", new MM_MethodHook() {
			boolean init = false;
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!init) {
					try {
						hookClickRedEnvelope(hi);
						hookCloseDetailRedEnvelope(hi);
						init = true;
					} catch (Exception e) {
					}
				}
			}
		});
	}

	private static void hookClickRedEnvelope(final HookInfo hi) throws ClassNotFoundException {
		XposedHelpers.findAndHookMethod(HookClasses.getClassName(HookClasses.KEY_LUCKY_MONEY_RECEIVE_UI_CLASS), hi.classLoader,
				HookClasses.getClassName(HookClasses.KEY_LUCKY_MONEY_RECEIVE_UI_RE_SET_ONCLICK_METHOD), int.class, int.class, String.class,
				hi.classLoader.loadClass(HookClasses.getClassName(HookClasses.KEY_LUCKY_MONEY_RECEIVE_UI_RE_SET_ONCLICK_METHOD_ARG3_CLASS)),
				new MM_MethodHook(){
			private long preMsgId = -1;
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
//							XposedBridge.log("红包界面：LuckyMoneyReceiveUI.e: " + hi.status + ", " + hi.isStarted() + ", " + hi.isStayInRoom());
				if(!hi.isStarted() || preMsgId == hi.curMsgId) {
					return;
				}
				preMsgId = hi.curMsgId;
				//XposedBridge.log("LuckyMoneyReceiveUI.e： 可以抢红包了");
				// 1.0.1: ePT
				// 1.0.2: eVK
				Field field = ReflectUtil.getField(param.thisObject.getClass(), HookClasses.getClassName(HookClasses.KEY_RE_OPEN_BUTTON_FIELD));
				//XposedBridge.log("getField： " + field.getName() + ", " + field.getType().getName() + ", " + param.thisObject.getClass().getName());
				field.setAccessible(true);
				Button btn = (Button) field.get(param.thisObject);
				if(btn.getVisibility() == View.VISIBLE) {
					//XposedBridge.log("发现可点击红包: " + Utils.getPrintString(btn));
					btn.performClick();
				}

				// 1.0.1: eQx
				// 1.0.2: eVh
				field = ReflectUtil.getField(param.thisObject.getClass(), HookClasses.getClassName(HookClasses.KEY_RE_INFO_TEXTVIEW_FIELD));
				field.setAccessible(true);
				TextView textView = (TextView) field.get(param.thisObject);
				String value = textView.getText().toString();
				if(textView.getVisibility() == View.VISIBLE && ("超过1天未领取，红包已失效".equals(value) || "手慢了，红包派完了".equals(value))) {
					//XposedBridge.log("发现领取不了的红包: " + Utils.getPrintString(textView));
					// 1.0.1: ePU
					// 1.0.2: eVL
					field = ReflectUtil.getField(param.thisObject.getClass(), HookClasses.getClassName(HookClasses.KEY_RE_CLOSE_BUTTON_FIELD));
					field.setAccessible(true);
					View view = (View) field.get(param.thisObject);
					view.performClick();
					hi.setStayInRoom();
					hi.end();
					return;
				}

			}
		});
	}
	
	public static void hookCloseDetailRedEnvelope(final HookInfo hi) {
		// com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI
		XposedHelpers.findAndHookMethod(HookClasses.getClassName(HookClasses.KEY_LUCKY_MONEY_DETAIL_UI_CLASS), hi.classLoader, "onCreate", Bundle.class, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(final MethodHookParam param) throws Throwable {
//				XposedBridge.log("查看红包详情.");
				if(!hi.isStarted()) {
					return;
				}
				hi.finishActivity((Activity) param.thisObject);
				hi.setStayInRoom();
				hi.end();
			}
		});
	}
}

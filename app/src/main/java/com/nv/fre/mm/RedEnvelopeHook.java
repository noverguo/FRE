package com.nv.fre.mm;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nv.fre.ReflectUtil;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;

public class RedEnvelopeHook {
	
	/**
	 * 红包相关点击事件
	 * @param hi
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 */
	public static void hookRedEnvelopeClickListener(final HookInfo hi) throws ClassNotFoundException, NoSuchFieldException {
		XposedHelpers.findAndHookMethod(View.class, "setOnClickListener", View.OnClickListener.class, new MM_MethodHook() {
			View.OnClickListener clickCallback = null;
			boolean init = false;
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!init) {
					init = true;
					XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI", hi.classLoader,  "e", int.class, int.class, String.class, hi.classLoader.loadClass("com.tencent.mm.r.j"), new MM_MethodHook(){
						private long preMsgId = -1;
						@Override
						public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
//							//XposedBridge.log("LuckyMoneyReceiveUI.e: " + hi.status + ", " + hi.isStarted() + ", " + hi.isStayInRoom());
							if(!hi.isStarted() || preMsgId == hi.curMsgId) {
								return;
							}
							preMsgId = hi.curMsgId;
							//XposedBridge.log("LuckyMoneyReceiveUI.e： 可以抢红包了");
							Field field = ReflectUtil.getField(param.thisObject.getClass(), "ePT");
							//XposedBridge.log("getField： " + field.getName() + ", " + field.getType().getName() + ", " + param.thisObject.getClass().getName());
							field.setAccessible(true);
							Button btn = (Button) field.get(param.thisObject);
							if(btn.getVisibility() == View.VISIBLE) {
								//XposedBridge.log("发现可点击红包: " + Utils.getPrintString(btn));
								btn.performClick();
							}

							field = ReflectUtil.getField(param.thisObject.getClass(), "eQx");
							field.setAccessible(true);
							TextView textView = (TextView) field.get(param.thisObject);
							String value = textView.getText().toString();
							if(textView.getVisibility() == View.VISIBLE && ("超过1天未领取，红包已失效".equals(value) || "手慢了，红包派完了".equals(value))) {
								//XposedBridge.log("发现领取不了的红包: " + Utils.getPrintString(textView));
								field = ReflectUtil.getField(param.thisObject.getClass(), "ePU");
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

			}
		});
	}
	
	public static void hookCloseDetailRedEnvelope(final HookInfo hi) {
		// com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI
		XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI", hi.classLoader, "onCreate", Bundle.class, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(final MethodHookParam param) throws Throwable {
				if(!hi.isStarted()) {
					return;
				}
//				//XposedBridge.log("查看完红包，点击关闭 ");
				hi.finishActivity((Activity) param.thisObject);
				hi.setStayInRoom();
				hi.end();
			}
		});
	}
}

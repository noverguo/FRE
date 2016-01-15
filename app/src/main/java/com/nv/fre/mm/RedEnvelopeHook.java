package com.nv.fre.mm;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.nv.fre.MatchView;
import com.nv.fre.ReflectUtil;
import com.nv.fre.Utils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
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
//							XposedBridge.log("LuckyMoneyReceiveUI.e: " + hi.status + ", " + hi.isStarted() + ", " + hi.isStayInRoom());
							if(!hi.isStarted() || preMsgId == hi.curMsgId) {
								return;
							}
							preMsgId = hi.curMsgId;
//							XposedBridge.log("LuckyMoneyReceiveUI.e： 可以抢红包了");
							Field field = ReflectUtil.getField(param.thisObject.getClass(), "ePT");
//							XposedBridge.log("getField： " + field.getName() + ", " + field.getType().getName() + ", " + param.thisObject.getClass().getName());
							field.setAccessible(true);
							Button btn = (Button) field.get(param.thisObject);
							if(btn.getVisibility() == View.VISIBLE) {
//								XposedBridge.log("发现可点击红包: " + Utils.getPrintString(btn));
								btn.performClick();
							}

							field = ReflectUtil.getField(param.thisObject.getClass(), "eQx");
							field.setAccessible(true);
							TextView textView = (TextView) field.get(param.thisObject);
							String value = textView.getText().toString();
							if(textView.getVisibility() == View.VISIBLE && ("超过1天未领取，红包已失效".equals(value) || "手慢了，红包派完了".equals(value))) {
//								XposedBridge.log("发现领取不了的红包: " + Utils.getPrintString(textView));
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

				//				XposedBridge.log("-----------------------------------------------------");
//				Utils.printParent((View) param.thisObject);
//				XposedBridge.log("=====================================================");
//				Utils.printViewHierarchy((View) param.thisObject);
//				View curView = (View) param.thisObject;
//				XposedBridge.log("hookRedEnvelopeClickListener: " + curView + ", " + param.args + ", " + (param.args != null && param.args.length > 0 ? param.args[0] : "0"));
//				Object[] args = param.args;
//				clickCallback = null;
//				if (args != null && args.length > 0 && args[0] != null) {
//					if (args[0] instanceof View.OnClickListener) {
//						clickCallback = (View.OnClickListener) args[0];
//					}
//				}
//				if (clickCallback == null) {
//					return;
//				}
//
//				hookClickFuckRedEnvelope(param, curView);

			}
			
			// 点击拆红包
			private void hookClickFuckRedEnvelope(MethodHookParam param, View view) {
				// old
//				if (!(view instanceof Button)) {
//					return;
//				}
//				CharSequence text = btnView.getText();
//				if (text != null && text.toString().equals("拆红包")) {
////					XposedBridge.log(param.thisObject.getClass().getName() + " --> " + param.args[0].getClass().getName());
//					clickCallback.onClick(btnView);
//				}

				// 应该是关闭按钮
//				if (!(view instanceof ImageView)) {
//					return;
//				}
//				if(Utils.matchParent(view, "RelativeLayout", "RelativeLayout", "LayoutListenerView", "FrameLayout", "LinearLayout")) {
//					XposedBridge.log("hookClickFuckRedEnvelope.onClick");
////					clickCallback.onClick(view);
//					Utils.printViewAndSubView((View)view.getParent());
//
//					View child = Utils.getChild(new MatchView[]{new MatchView(0, new Class[]{RelativeLayout.class, ImageView.class, LinearLayout.class, ImageView.class})
//					, new MatchView(2, new Class[]{ImageView.class, LinearLayout.class, Button.class})}, (View)view.getParent());
//					XposedBridge.log("find re btn: " + child + "" + (child == null ? "" : (child.isClickable() + ", " + child.isEnabled())));
//					if(child != null && child instanceof Button) {
//						XposedBridge.log("performClick: " + child.toString());
//						child.performClick();
//					}
//				}
			}
		});
	}
	
	/**
	 * 发现是手慢了或失效的红包，则关闭
	 * @param hi
	 */
	public static void hookLateSoClose(final HookInfo hi) {
//		XposedHelpers.findAndHookMethod(TextView.class, "onDraw", Canvas.class, new MM_MethodHook() {
//		XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, BufferType.class, new MM_MethodHook() {
//			@Override
//			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
//				if(!hi.isStarted()) {
//					return;
//				}
//				TextView tv = (TextView) param.thisObject;
//				String text = tv.getText().toString();
//				if(text == null || (!text.equals("手慢了，红包派完了") && !text.equals("超过1天未领取，红包已失效"))) {
//					return;
//				}
//				XposedBridge.log("发现 手慢 或 失效的红包");
//
//				ViewParent parent = tv.getParent();
//				if(!Utils.instanceOf(parent, LinearLayout.class, true)) {
//					return;
//				}
//				parent = parent.getParent();
//				if(!Utils.instanceOf(parent, RelativeLayout.class, true)) {
//					return;
//				}
//				RelativeLayout rlParent = (RelativeLayout) parent;
//				if(rlParent == null || rlParent.getChildCount() != 5) {
//					return;
//				}
//				View closeView = rlParent.getChildAt(3);
//				if(closeView == null || !Utils.instanceOf(closeView, ImageView.class, true)) {
//					return;
//				}
//				closeView.callOnClick();
//				hi.setStayInRoom();
//				hi.end();
//			}
//		});
	}
	
	public static void hookCloseDetailRedEnvelope(final HookInfo hi) {
		// com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI
		XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI", hi.classLoader, "onCreate", Bundle.class, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(final MethodHookParam param) throws Throwable {
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

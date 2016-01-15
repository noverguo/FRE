package com.nv.fre.mm;

import java.lang.reflect.Field;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nv.fre.ClickView;
import com.nv.fre.MatchView;
import com.nv.fre.Utils;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ChattingMsgHook {
	MatchView[] redEnvelopeChildViewClasses;
	Class<?>[] redEnvelopeParentViewClasses;
	MatchView[] newMsgViewClasses;
	{
		redEnvelopeChildViewClasses = new MatchView[4];
		redEnvelopeChildViewClasses[0] = new MatchView(0, new Class<?>[] { LinearLayout.class, RelativeLayout.class });
		redEnvelopeChildViewClasses[1] = new MatchView(1, new Class<?>[] { ImageView.class, RelativeLayout.class });
		redEnvelopeChildViewClasses[2] = new MatchView(0, new Class<?>[] { LinearLayout.class });
		redEnvelopeChildViewClasses[3] = new MatchView(1, new Class<?>[] { TextView.class, TextView.class });

		// old: LinearLayout LinearLayout RelativeLayout
		// new: LinearLayout LinearLayout LinearLayout LinearLayout com.tencent.mm.ui.chatting.av
		redEnvelopeParentViewClasses = new Class<?>[5];
		redEnvelopeParentViewClasses[0] = LinearLayout.class;
		redEnvelopeParentViewClasses[1] = LinearLayout.class;
		redEnvelopeParentViewClasses[2] = LinearLayout.class;
		redEnvelopeParentViewClasses[3] = LinearLayout.class;
		
		newMsgViewClasses = new MatchView[1];
		newMsgViewClasses[0] = new MatchView(1, new Class<?>[]{ImageView.class, TextView.class});
	}
	/**
	 * 检测消息的View，如发现有红包的View，则进行点击
	 * 如当前有红包，则过滤消息，以显示红包View，方便点击
	 * 
	 * @param hi
	 * @throws Exception
	 */
	public static void hookMsgView(final HookInfo hi) throws Exception {
		ChattingMsgHook cmh = new ChattingMsgHook(hi);
		
		cmh.hookAdapter();
		// 检测红包的View点击事件
		cmh.hookClickListener();
		// 发现红包就点击
		cmh.hookGetView();
		// 过滤非红包的消息
//		cmh.hookGetCount();
//		cmh.hookGetItem();
	}

	private SparseArray<Object> items;
	private Class<?> msgClass;
	private Field msgIdField;
	private HookInfo hi;
	private WeakHashMap<ListAdapter, ListView> listViewMap = new WeakHashMap<ListAdapter, ListView>();
	private ChattingMsgHook(HookInfo hi) throws Exception {
		this.hi = hi;
		items = new SparseArray<Object>();
		// old: com.tencent.mm.storage.ad
		// new: com.tencent.mm.storage.ae
		msgClass = hi.classLoader.loadClass("com.tencent.mm.storage.ae");
		msgIdField = msgClass.getField("field_msgId");
		msgIdField.setAccessible(true);
	}
	
	private void hookAdapter() throws Exception {
		final Class<?> adapterClass = hi.classLoader.loadClass("com.tencent.mm.ui.chatting.cj");
		// 关联adapter，便于之后使用
		XposedHelpers.findAndHookMethod(ListView.class, "setAdapter", ListAdapter.class, new MM_MethodHook() {
			@Override
			public void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (param.args != null && param.args.length > 0 && param.args[0] != null) {
					XposedBridge.log("setAdapter: " + param.thisObject.getClass().getName() + ", " + param.args[0].getClass().getName());
					if(param.args[0].getClass() == adapterClass) {
						listViewMap.put((ListAdapter) param.args[0], (ListView) param.thisObject);
					}
				}
			}
		});
	}
	
	/**
	 * 红包相关点击事件
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 */
	private void hookClickListener() throws ClassNotFoundException, NoSuchFieldException {
		XposedHelpers.findAndHookMethod(View.class, "setOnClickListener", View.OnClickListener.class, new MM_MethodHook() {
			View.OnClickListener clickCallback = null;
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				View curView = (View) param.thisObject;
				Object[] args = param.args;
				if (args[0] != null && args[0] instanceof View.OnClickListener) {
					clickCallback = (View.OnClickListener) args[0];
				} 
				if(clickCallback == null) {
					return;
				}

				if (curView instanceof LinearLayout) {
					if(!relativeClickRedEnvelope(param, curView)) {
						relativeClickNewMsg(param, curView);
					}
				}
				clickCallback = null;
			}
			
			// 关联聊天记录中领取红包的点击事件
			private boolean relativeClickRedEnvelope(MethodHookParam param, View curView) throws IllegalAccessException, IllegalArgumentException {
				TextView tv = (TextView) Utils.getChild(redEnvelopeChildViewClasses, curView);
				if (tv == null || !"领取红包".equals(tv.getText().toString())) {
					return false;
				}
//				XposedBridge.log("发现红包: " + curView.getClass().getName());
				ViewParent parent = (ViewParent) curView;
				while(parent.getParent() != null) {
					parent = parent.getParent();
				}
				hi.clickCallbackMap.put((View)parent, new ClickView(curView, clickCallback));
				final View keyView = (View) parent;
				hi.postDelayed(new Runnable() {
					@Override
					public void run() {
						hi.clickCallbackMap.remove(keyView);
					}
				}, 15000);
				return true;
			}
			
			private String preNewMsg;
			private AtomicBoolean flag = new AtomicBoolean(false);
			private void relativeClickNewMsg(MethodHookParam param, final View curView) {
				if(!hi.canFuck()) {
					return;
				}
				final TextView tv = (TextView) Utils.getChild(newMsgViewClasses, curView);
				if(tv == null) {
					return;
				}
				String curNewMsg = tv.getText().toString();
				if(curNewMsg.equals(preNewMsg) || !curNewMsg.endsWith("条新消息")) {
					return;
				}
				final View.OnClickListener callback = clickCallback;
				flag.set(false);
				if(preNewMsg == null) {
					XposedHelpers.findAndHookMethod(callback.getClass(), "onClick", View.class, new MM_MethodHook() {
						@Override
						public void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
							flag.set(true);
						}
					});
				}
				preNewMsg = curNewMsg;
				hi.postDelayed(new Runnable() {
					@Override
					public void run() {
						if(flag.get()) {
							return;
						}
//						XposedBridge.log("clickNewMsg: " + tv.getText().toString() + ", " + callback.getClass().getName() + ", " + curView.getClass().getName());
						callback.onClick(curView);
					}
				}, 8000);
			}

		});
	}
	
	private void hookGetView() {
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.chatting.cj", hi.classLoader, "getView", int.class, View.class, ViewGroup.class, new MM_MethodHook() {
			private AtomicBoolean isRun = new AtomicBoolean(false);
			Runnable clickRedEnvelopCallback = new Runnable() {
				@Override
				public void run() {
					isRun.set(true);
					ClickView curClickView = hi.redEnvelopClickView.get(hi.curMsgId);
					if(curClickView == null) {
						// 当前的没有，就改成存在的
						hi.curMsgId = hi.redEnvelopClickView.keyAt(0);
						curClickView = hi.redEnvelopClickView.get(hi.curMsgId);
					}
					hi.redEnvelopClickView.remove(hi.curMsgId);
					hi.status = HookInfo.STATUS_CLICK_RED_ENVELOPE_VIEW;
					XposedBridge.log("点击领取红包: " + hi.curMsgId);
					// 点击领取红包
					curClickView.clickCallback.onClick(curClickView.view);
					hi.doneMsgIds.add(hi.curMsgId);
				}
			};
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				final View view = (View) param.getResult();
				if (view == null) {
					return;
				}
				BaseAdapter adapter = (BaseAdapter) param.thisObject;
				Integer pos = (Integer) param.args[0];
				if (pos < 0) {
					return;
				}
				// 这里的getItem需要使用注入的代码
				inHook = false;
				Object item = adapter.getItem(pos);
				inHook = true;
//				XposedBridge.log("getView getItem: " + item.getClass().getName());
				if (item == null || item.getClass() != msgClass) {
					return;
				}
				hi.chattingListViewAdapter = adapter;
				// 只处理红包
				if (!hi.clickCallbackMap.containsKey(view)) {
					return;
				}
				final Long msgId = (Long) msgIdField.get(item);
				// 红包已领取，不再进行领取
				if (hi.doneMsgIds.contains(msgId)) {
					hi.clickCallbackMap.remove(view);
					return;
				}
				if (hi.curMsgId == -1) {
					if (hi.redEnvelopMsgs.size() > 0) {
						// 先把新的红包抢到，再考虑当前界面中未抢到的
						hi.clickCallbackMap.remove(view);
						return;
					} else {
						// 有旧红包未领取
						hi.curMsgId = msgId;
					}
				} else if (hi.curMsgId != msgId) {
				}
				if(hi.redEnvelopClickView.size() == 0) {
					isRun.set(false);
					// getView和clickRedEnvelopCallback都运行在主线程，getView会执行多次，
				}
				hi.redEnvelopClickView.put(msgId, hi.clickCallbackMap.get(view));
//				XposedBridge.log("getView 发现可领取的红包: " + msgId);
				if(!isRun.get()) {
					hi.uiHandler.removeCallbacks(clickRedEnvelopCallback);
					hi.uiHandler.post(clickRedEnvelopCallback);
				}
			}
		});
	}
	
	private void hookGetCount() {
		// hook BaseAdapter.getCount，ListView.getView之前肯定需要先知道有多少个item
		// 如果是有新红包消息时，即此时会插入新的view，这时需要，
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.i", hi.classLoader, "getCount", new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("com.tencent.mm.ui.i.getCount: " + param.getResult());
				if (!hi.canFuck()) {
					return;
				}
				if (!hi.initCount) {
					items.clear();
					Integer count = (Integer) param.getResult();
					final BaseAdapter adapter = (BaseAdapter) param.thisObject;
					int curIdx = 0;
//					StringBuilder log = new StringBuilder("com.tencent.mm.ui.i.getCount: status: " + hi.status + " :");
//					log.append(count).append(" --> ");
					hi.itemMap.clear();
					for (int i = 0; i < count; ++i) {
						Object item = adapter.getItem(i);
						if (item == null) {
							continue;
						}
						if (item.getClass() != msgClass) {
							return;
						}
						Long msgId = (Long) msgIdField.get(item);
//						log.append(msgId);
						if (hi.redEnvelopMsgs.get(msgId) != null || hi.noDoRedEnvelopMsgIds.contains(msgId)) {
//							log.append(": ").append(curIdx).append(" -> ").append(i);
							// itemMap.put(curIdx++, i);
							items.put(curIdx++, item);
						}
//						log.append(",");
					}
//					XposedBridge.log(log.toString());
					if (items.size() > 0) {
						hi.initCount = true;
						hi.uiHandler.post(new Runnable() {
							@Override
							public void run() {
								adapter.notifyDataSetChanged();
							}
						});
					}
				}
				if (items.size() > 0) {
//					XposedBridge.log("com.tencent.mm.ui.i.getCount change: " + items.size());
					param.setResult(items.size());
				}
			}
		});
	}

	private void hookGetItem() {
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.i", hi.classLoader, "getItem", int.class, new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("com.tencent.mm.ui.i.getItem: " + param.args[0]);
				if (!hi.canFuck() || !hi.initCount) {
					return;
				}
				Object item = param.getResult();
				if (item == null || item.getClass() != msgClass) {
					return;
				}
				// int val = itemMap.get(((Integer)param.args[0]), -999);
				Object curItem = items.get(((Integer) param.args[0]));
				// if(val == -999) {
				if (curItem == null) {
					return;
				}
//				XposedBridge.log("com.tencent.mm.ui.i.getItem: status: " + hi.status + " :" + (Integer) param.args[0] + " --> " + msgIdField.get(curItem));
				// 转移到红包的位置
				// BaseAdapter adapter = (BaseAdapter) param.thisObject;
				// param.setResult(adapter.getItem(val));
				param.setResult(curItem);
			}
		});
	}
}

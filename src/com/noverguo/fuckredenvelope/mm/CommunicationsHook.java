package com.noverguo.fuckredenvelope.mm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.noverguo.fuckredenvelope.MatchView;
import com.noverguo.fuckredenvelope.ReflectUtil;
import com.noverguo.fuckredenvelope.Utils;
import com.noverguo.fuckredenvelope.receiver.SettingReceiver;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class CommunicationsHook {
	MatchView[] groupViewMatchViews;
	{
		groupViewMatchViews = new MatchView[4];
		groupViewMatchViews[0] = new MatchView(1, new Class<?>[] { RelativeLayout.class, LinearLayout.class });
		groupViewMatchViews[1] = new MatchView(0, new Class<?>[] { LinearLayout.class, LinearLayout.class });
		groupViewMatchViews[2] = new MatchView(0, new Class<?>[] { LinearLayout.class, View.class });
		groupViewMatchViews[3] = new MatchView(0, new Class<?>[] { View.class });
	}

	/**
	 * 启动窗口后，可能会停在消息列表窗口（具体原因待查），这时需要去点击进入对应的窗口
	 * 
	 * @param hi
	 * @throws Exception
	 */
	public static void hookClickChattingItem(final HookInfo hi) throws Exception {
		final CommunicationsHook ck = new CommunicationsHook(hi);
		ck.hookAdapter();
		ck.hookGetView();
		ck.hookItemClick();
	}

	private HookInfo hi;
	Map<String, String> allTalks = new HashMap<String, String>();
	WeakHashMap<ListAdapter, ListView> listViewMap = new WeakHashMap<ListAdapter, ListView>();
	Class<?> conversationListViewClass;
	Class<?> conversationAdapterClass;
	Class<?> conversationItemClass;
	Field usernameField;
	Class<?> textViewClass;
	Method getTextMethod;

	public CommunicationsHook(HookInfo hi) throws Exception {
		this.hi = hi;
		conversationListViewClass = hi.classLoader.loadClass("com.tencent.mm.ui.conversation.ConversationOverscrollListView");
		conversationAdapterClass = hi.classLoader.loadClass("com.tencent.mm.ui.conversation.d");
		conversationItemClass = hi.classLoader.loadClass("com.tencent.mm.storage.r");
		usernameField = conversationItemClass.getField("field_username");
		textViewClass = hi.classLoader.loadClass("com.tencent.mm.ui.base.NoMeasuredTextView");
		getTextMethod = textViewClass.getMethod("getText", new Class<?>[0]);
	}

	private void hookAdapter() {
		// 关联adapter，便于之后使用
		XposedHelpers.findAndHookMethod(ListView.class, "setAdapter", ListAdapter.class, new MM_MethodHook() {
			@Override
			protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (param.args != null && param.args.length > 0 && param.args[0] != null) {
					if (param.thisObject.getClass() != conversationListViewClass || param.args[0].getClass() != conversationAdapterClass) {
						return;
					}
					listViewMap.put((ListAdapter) param.args[0], (ListView) param.thisObject);
				}
			}
		});
	}

	private void hookGetView() {
		// 聊天室列表
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.conversation.d", hi.classLoader, "getView", int.class, View.class, ViewGroup.class, new MM_MethodHook() {
			private long pre = 0;

			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("com.tencent.mm.ui.conversation.d.getView");
				final View view = (View) param.getResult();
				if (view == null) {
					return;
				}
				final Integer pos = (Integer) param.args[0];
				final ListAdapter adapter = (ListAdapter) param.thisObject;
				Object item = adapter.getItem(pos);
				if (item.getClass() != conversationItemClass) {
					return;
				}

				String userName = (String) usernameField.get(item);
				if (userName == null) {
					return;
				}
				// 获得聊天室的名称
				String displayName = getDisplayName(view);
				if (!allTalks.containsKey(userName)) {
					allTalks.put(userName, displayName);
					updateTalks();
				}

				long cur = hi.curMsgId << 16 + pos;
				if (cur == pre) {
					return;
				}
				Msg msg = hi.allMsgs.get(hi.curMsgId);
				if (msg == null || msg.talker == null) {
					return;
				}
				if (!userName.equals(msg.talker) || (hi.canFuck() && msg.talker.equals(hi.stayTalker))) {
					return;
				}
				ListView lv = listViewMap.get(adapter);
				if (lv == null) {
					return;
				}
				hi.stayTalker = userName;
				XposedBridge.log("进入聊天室1: " + hi.stayTalker);
				pre = cur;
				// 匹配到当前红包消息对应的item，并执行点击
				// +7是因为它前面还有其实特殊的
				lv.performItemClick(view, pos + 7, adapter.getItemId(pos));

			}

			private String getDisplayName(View view) throws Exception {
				if (textViewClass != null && getTextMethod != null) {
					View displayView = Utils.getChild(groupViewMatchViews, view);
					if (displayView != null) {
						if (displayView.getClass() == textViewClass) {
							Object res = getTextMethod.invoke(displayView, new Object[0]);
							if (res != null) {
								return res.toString();
							}
						}
					}
				}
				return "";
			}
		});
	}

	private void hookItemClick() {
		XposedHelpers.findAndHookMethod(AdapterView.class, "setOnItemClickListener", OnItemClickListener.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if (param.thisObject.getClass() != conversationListViewClass) {
					return;
				}
				final ListView conversationListView = (ListView) param.thisObject;
				if (param.args != null && param.args.length > 0 && param.args[0] != null) {
					OnItemClickListener listener = (OnItemClickListener) param.args[0];
					// XposedBridge.log("setOnItemClickListener: " + listener);
					Class<? extends OnItemClickListener> itemListenerClass = listener.getClass();
					Method method = ReflectUtil.getMethod(itemListenerClass, "onItemClick");
					if (method != null) {
						XposedBridge.hookAllMethods(itemListenerClass, method.getName(), new MM_MethodHook() {
							protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
								Integer pos = (Integer) param.args[2];
								if (pos == null || pos < 0 || pos > conversationListView.getCount()) {
									return;
								}
								Object item = conversationListView.getItemAtPosition(pos);
								if (item == null) {
									return;
								}
								// 点击进入聊天室后，需要记录下来
								String userName = (String) usernameField.get(item);
								hi.stayTalker = userName;
								XposedBridge.log("onItemClick: 进入聊天室2: " + hi.stayTalker + " status: " + hi.status + " stay: " + hi.stay + "  stayTalker: " + hi.stayTalker);
							};
						});
					}
				}
			}
		});
	}

	private Runnable updateToView = new Runnable() {
		@Override
		public void run() {
			String[] values = new String[allTalks.size()];
			int i = 0;
			for (String key : allTalks.keySet()) {
				values[i++] = key + "," + allTalks.get(key);
			}
			hi.context.sendBroadcast(new Intent(SettingReceiver.ACTION_TALKS).putExtra(SettingReceiver.KEY_TALKS, values));
		}
	};

	private void updateTalks() {
		hi.postDelayed(updateToView, 1000);
	}
}

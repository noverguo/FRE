package com.noverguo.fuckredenvelope.mm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LongSparseArray;
import android.telephony.TelephonyManager;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.noverguo.fuckredenvelope.ClickView;
import com.noverguo.fuckredenvelope.MatchView;
import com.noverguo.fuckredenvelope.ReflectUtil;
import com.noverguo.fuckredenvelope.TelephonyProperties;
import com.noverguo.fuckredenvelope.Utils;
import com.noverguo.fuckredenvelope.receiver.SettingReceiver;
import com.noverguo.fuckredenvelope.receiver.UnlockReceiver;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MMHook implements IXposedHookLoadPackage {
	public static final String ACTION_TALKS = "com.noverguo.fuckredenvelope.mm.MMHook.ACTION_TALKS";
	public static final String KEY_TALKS = "com.noverguo.fuckredenvelope.mm.MMHook.KEY_TALKS";
	private static final int STATUS_NOTHING = 0;
	private static final int STATUS_START_ACTIVITY = 1;
	private static final int STATUS_IN_ROOM = 2;
	private static final int STATUS_CLICK_RED_ENVELOPE_VIEW = 3;
	private static final int STAY_NOTTHING = 0;
	private static final int STAY_IN_LIST = 1;
	private static final int STAY_IN_ROOM = 2;
	private static final int STAY_OTHER = 3;
	private ClassLoader classLoader;
	private Context context;
	private LinkedBlockingDeque<Msg> queue = new LinkedBlockingDeque<Msg>();
	private LongSparseArray<Msg> allMsgs = new LongSparseArray<Msg>();
	private LongSparseArray<Msg> redEnvelopMsgs = new LongSparseArray<Msg>();
	private Map<View, ClickView> clickCallbackMap = new HashMap<View, ClickView>();
	private Set<Long> doneMsgIds = new HashSet<Long>();
	private Set<String> grepTalks = new HashSet<String>();
	private int status = STATUS_NOTHING;
	private int stay = STAY_NOTTHING;
	private String posTalker = null;
	private long curMsgId = -1;
	
	private SparseIntArray itemMap = new SparseIntArray();
	private static boolean inHook = false;
	
	private Handler uiHandler;

	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if (!"com.tencent.mm".equals(lpparam.packageName)) {
			return;
		}
		XposedBridge.log("find wechat app: " + lpparam.packageName);

		initContext(lpparam);
		// 不给读imei和imsi，以免封号
		hookReadImeiAndImsi();
		// 读取消息，发现红包则启动窗口
		hookReadMsg();
		// 启动窗口后，可能会停在消息列表窗口（具体原因待查），这时需要去点击进入对应的窗口
		hookClickChattingItem();
		// 注入红包相关点击事件
		hookRedEnvelopeClickListener();
		// 发现是手慢了，红包派完了，则关闭
		hookLateSoClose();
		// 检测消息的View，如发现有红包的View，则进行点击
		hookMsgView();
		// 检测到红包后需要自动发送消息，以表示对他人的尊重
//		hookAutoSendMsg();
//		hookForTest();
	}
	
	private BroadcastReceiver talksReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(ACTION_TALKS.equals(intent.getAction())) {
				String[] talks = intent.getStringArrayExtra(KEY_TALKS);
				grepTalks.clear();
				if(talks != null) {
					grepTalks.addAll(Arrays.asList(talks));
				}
			}
		}
	};

	private void initContext(final LoadPackageParam lpparam) {
		this.classLoader = lpparam.classLoader;
		XposedHelpers.findAndHookMethod("com.tencent.mm.app.MMApplication", classLoader, "onCreate", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				context = (Context) param.thisObject;
				IntentFilter intentFilter = new IntentFilter(ACTION_TALKS);
				context.registerReceiver(talksReceiver, intentFilter);
			}
		});
		
		XposedHelpers.findAndHookMethod("com.tencent.mm.app.MMApplication", classLoader, "onCreate", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				context = (Context) param.thisObject;
				IntentFilter intentFilter = new IntentFilter(ACTION_TALKS);
				context.registerReceiver(talksReceiver, intentFilter);
			}
		});
		
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.LauncherUI", classLoader, "onPause", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				stay = STAY_OTHER;
			}
		});
	}
	
	private void hookReadImeiAndImsi() {
//		1）获取运营商sim卡imsi号：
//		 String android_imsi = telephonyManager.getSubscriberId();//获取手机IMSI号 
//		 String IMSI = android.os.SystemProperties.get(android.telephony.TelephonyProperties.PROPERTY_IMSI);
//		2）获取IME标识两种方法(手机唯一的标识)
//		String imei = ((TelephonyManager) context.getSystemService(TELEPHONY_SERVICE)).getDeviceId();
//		String IMEI = android.os.SystemProperties.get(android.telephony.TelephonyProperties.PROPERTY_IMEI)
		XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSubscriberId", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult("");
			}
		});
		XposedHelpers.findAndHookMethod(TelephonyManager.class, "getDeviceId", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult("");
			}
		});
		XposedHelpers.findAndHookMethod("android.os.SystemProperties", classLoader, "get", String.class, getPropertiesHook);
		XposedHelpers.findAndHookMethod("android.os.SystemProperties", classLoader, "get", String.class, String.class, getPropertiesHook);
	}
	
	MM_MethodHook getPropertiesHook = new MM_MethodHook() {
		@Override
		protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
			String key = (String) param.args[0];
			if(TelephonyProperties.PROPERTY_IMEI.equals(key)) {
				param.setResult("");
			}
		}
	};

	private void hookReadMsg() {
		XposedHelpers.findAndHookMethod(ContentValues.class, "size", new MM_MethodHook() {
			boolean hookCloseDetail = false;
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					if (!hookCloseDetail) {
						hookCloseDetailRedEnvelope();
						hookCloseDetail = true;
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				ContentValues values = (ContentValues) param.thisObject;
				Msg msg = new Msg(values);
				if (msg.msgId == null || msg.content == null) {
					return;
				}
				if (allMsgs.get(msg.msgId) != null) {
					return;
				}
				while(queue.size() > 50) {
					long rmId = queue.removeFirst().msgId;
					if(redEnvelopMsgs.get(rmId) == null) {
						allMsgs.remove(rmId);
					}
				}
				queue.add(msg);
				allMsgs.put(msg.msgId, msg);
				
				StringBuilder buf = new StringBuilder("newMsg --> ");
				for(String key : values.keySet()) {
					buf.append(key).append(": ");
					Object value = values.get(key);
					if(value != null) {
						String info = value.toString();
						if(info.length() > 200) {
							info = info.substring(0, 200);
						}
						buf.append(info).append(", ");
					}
				}
				XposedBridge.log(buf.toString());
				
				String content = msg.content;
				XposedBridge.log("有新的消息: " + msg.msgId + ": " + content + " status: " + status);
				if (content == null) {
					return;
				}
				if (content.contains("领取红包") && content.contains("微信红包") && content.contains("查看红包")) {
					if(!grepTalks.isEmpty()) {
						String talker = values.getAsString("talker");
						if(talker == null || !grepTalks.contains(talker)) {
							return;
						}
					}
					
					redEnvelopMsgs.put(msg.msgId, msg);
					
					startFuckRedEnvelop(msg);
					
					// 解锁屏幕
					context.sendBroadcast(new Intent(UnlockReceiver.ACTION_UNLOCK));
				}
			}

		});
	}
	
	private void hookMsgView() throws Exception {
//		com.tencent.mm.ui.chatting.cj.getView(int paramInt, View paramView, ViewGroup paramViewGroup)
		final Class<?> msgClass = classLoader.loadClass("com.tencent.mm.storage.ad");
		final Field msgIdField = msgClass.getField("field_msgId");
		msgIdField.setAccessible(true);
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.chatting.cj", classLoader, "getView", int.class, View.class, ViewGroup.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				View view = (View) param.getResult();
				if(view == null) {
					return;
				}
				if(!clickCallbackMap.containsKey(view)) {
					return;
				}
				BaseAdapter adapter = (BaseAdapter) param.thisObject;
				Integer pos = (Integer)param.args[0];
				if(pos < 0 || pos >= adapter.getCount()) {
					return;
				}
				Object item = adapter.getItem(pos);
				if(item == null || item.getClass() != msgClass) {
					return;
				}
				stay = STAY_IN_ROOM;
				Long msgId = (Long) msgIdField.get(item);
				XposedBridge.log("msgId: " + curMsgId + " -> " + msgId);
				if(doneMsgIds.contains(msgId)) {
					XposedBridge.log("红包已领取，不再进行领取: " + msgId);
					return;
				}
				if(curMsgId == -1) {
					if(redEnvelopMsgs.size() > 0) { 
						// 先把新的红包抢到，再考虑当前界面中未抢到的
						return;
					} else {
						curMsgId = msgId;
						XposedBridge.log("有旧红包未领取: " + msgId);
					}
				} else if(curMsgId != msgId) {
					return;
				}
				status = STATUS_CLICK_RED_ENVELOPE_VIEW;
				XposedBridge.log("getView msgId: " + msgId + " : 点击领取红包");
				ClickView clickView = clickCallbackMap.get(view);
				clickView.clickCallback.onClick(clickView.view);
				doneMsgIds.add(msgId);
			}
		});
//		final Method avCMethod = classLoader.loadClass("com.tencent.mm.ui.i").getMethod("avC", new Class<?>[0]);
//		if(avCMethod == null) {
//			XposedBridge.log("not found com.tencent.mm.ui.i.avC method");
//		}
		
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.i", classLoader, "getCount", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if(status == STATUS_NOTHING) {
					return;
				}
				Integer count = (Integer) param.getResult();
				BaseAdapter adapter = (BaseAdapter) param.thisObject;
//				Integer acv = (Integer) avCMethod.invoke(adapter, new Object[0]);
				int curIdx = 0;
				XposedBridge.log("com.tencent.mm.ui.i.getCount: " + count);
				itemMap.clear();
				for(int i=0;i<count;++i) {
					Object item = adapter.getItem(i);
					if(item == null) {
						continue;
					}
					if(item.getClass() != msgClass) {
						return;
					}
					Long msgId = (Long) msgIdField.get(item);
					if(redEnvelopMsgs.get(msgId) != null) {
						itemMap.put(curIdx++, i);
					}
				}
			}
		});
		
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.i", classLoader, "getItem", int.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if(status == STATUS_NOTHING) {
					return;
				}
				Object item = param.getResult();
				if(item == null || item.getClass() != msgClass) {
					return;
				}
				XposedBridge.log("com.tencent.mm.ui.i.getItem: " + (Integer)param.args[0]);
				int val = itemMap.get(((Integer)param.args[0]), -999);
				if(val == -999) {
					return;
				}
				// 转移到红包的位置
				BaseAdapter adapter = (BaseAdapter) param.thisObject;
				param.setResult(adapter.getItem(val));
			}
		});
	}
	final static MatchView[] groupViewMatchViews;
	static {
		groupViewMatchViews = new MatchView[4];
		groupViewMatchViews[0] = new MatchView(1, new Class<?>[]{RelativeLayout.class, LinearLayout.class});
		groupViewMatchViews[1] = new MatchView(0, new Class<?>[]{LinearLayout.class, LinearLayout.class});
		groupViewMatchViews[2] = new MatchView(0, new Class<?>[]{LinearLayout.class, View.class});
		groupViewMatchViews[3] = new MatchView(0, new Class<?>[]{View.class});
	}
	private Map<String, String> allTalks = new HashMap<String, String>();
	private Map<ListAdapter, ListView> listViewMap = new HashMap<ListAdapter, ListView>();
	private void hookClickChattingItem() throws Exception {
		final Class<?> conversationListViewClass = classLoader.loadClass("com.tencent.mm.ui.conversation.ConversationOverscrollListView");
		final Class<?> conversationAdapterClass = classLoader.loadClass("com.tencent.mm.ui.conversation.d");
		// 关联adapter，便于之后使用
		XposedHelpers.findAndHookMethod(ListView.class, "setAdapter", ListAdapter.class, new MM_MethodHook() {
			@Override
			protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("setAdapter: " + param.thisObject.getClass().getName());
				if(param.args != null && param.args.length > 0 && param.args[0] != null) {
					XposedBridge.log("setAdapter: " + param.thisObject.getClass().getName() + " --> " + param.args[0].getClass().getName());
					if(param.thisObject.getClass() != conversationListViewClass || param.args[0].getClass() != conversationAdapterClass) {
						return;
					}
					listViewMap.put((ListAdapter)param.args[0], (ListView)param.thisObject);
				}
			}
		});
		final Class<?> conversationItemClass = classLoader.loadClass("com.tencent.mm.storage.r");
		if(conversationItemClass == null) {
			XposedBridge.log("com.tencent.mm.storage.r is null");
			return;
		}
		final Field usernameField = conversationItemClass.getField("field_username");
		ReflectUtil.cacheFields(conversationItemClass);
		final Class<?> textViewClass = classLoader.loadClass("com.tencent.mm.ui.base.NoMeasuredTextView");
		final Method getTextMethod;
		if(textViewClass == null) {
			XposedBridge.log("can not find com.tencent.mm.ui.base.NoMeasuredTextView class");
			getTextMethod = null;
		} else {
			getTextMethod = textViewClass.getMethod("getText", new Class<?>[0]);
			if(getTextMethod == null) {
				XposedBridge.log("can not find com.tencent.mm.ui.base.NoMeasuredTextView.getText method");
			}
		}
		
		// 聊天室或发现栏目
		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.conversation.d", classLoader, "getView", int.class, View.class, ViewGroup.class, new MM_MethodHook() {
			private long pre = 0;
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("com.tencent.mm.ui.conversation.d.getView");
				final View view = (View) param.getResult();
				if(view == null) {
					return;
				}
				final Integer pos = (Integer) param.args[0];
				final ListAdapter adapter = (ListAdapter) param.thisObject;
				XposedBridge.log("conversation: pos: " + pos + " size: " + adapter.getCount());
				if(pos < 0 || pos >= adapter.getCount()) {
					return;
				}
				Object item = adapter.getItem(pos);
				XposedBridge.log("conversation: " + item);
				if(item == null) {
					return;
				}
				XposedBridge.log("conversation: " + item.getClass().getName() + " --> " + conversationItemClass.getName());
				if(item.getClass() != conversationItemClass) {
					return;
				}
				
				String userName = (String) usernameField.get(item);
				if(userName == null) {
					return;
				}
				
				stay = STAY_IN_LIST;
				posTalker = null;
				
				String displayName = getDisplayName(view);
				if(!allTalks.containsKey(userName)) {
					allTalks.put(userName, displayName);
					updateTalks();
				}
				XposedBridge.log("conversation: " + ReflectUtil.getFieldInfos(item));
				long cur = curMsgId << 16 + pos;
				if(cur == pre) {
					return;
				}
				Msg msg = allMsgs.get(curMsgId);
				if(msg == null) {
					return;
				}
				if(msg.talker == null) {
					return;
				}
				XposedBridge.log("getView matchroom" + userName + " --> " + msg.talker);
				if(!userName.equals(msg.talker)) {
					return;
				}
				pre = cur;
				final ListView listView = listViewMap.get(adapter);
				XposedBridge.log("ListView: " + cur + " --> " + listView);
				listView.performItemClick(view, pos + 7, adapter.getItemId(pos));
				
				stay = STAY_IN_ROOM;
				posTalker = userName;
			}
			
			private String getDisplayName(View view) throws Exception {
				if(textViewClass == null) {
					return "";
				}
				if(getTextMethod == null) {
					return "";
				}
				View displayView = Utils.getChild(groupViewMatchViews, view);
				if(displayView == null) {
					return "";
				}
				XposedBridge.log("get display view: " + displayView.getClass().getName());
				if(displayView.getClass() == textViewClass) {
					Object res = getTextMethod.invoke(displayView, new Object[0]);
					XposedBridge.log("find display string: " + res);
					if(res != null) { 
						return res.toString();
					}
				}
				return "";
			}
		});
		XposedHelpers.findAndHookMethod(AdapterView.class, "setOnItemClickListener", OnItemClickListener.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param)
					throws Throwable {
				if(param.args != null && param.args.length > 0 && param.args[0] != null) {
					OnItemClickListener listener = (OnItemClickListener) param.args[0];
					XposedBridge.log("setOnItemClickListener: " + listener);
					Class<? extends OnItemClickListener> itemListenerClass = listener.getClass();
					Method method = ReflectUtil.getMethod(itemListenerClass, "onItemClick");
					if(method != null) {
						XposedBridge.hookAllMethods(itemListenerClass, method.getName(), new MM_MethodHook() {
							protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
								XposedBridge.log("onItemClick: " + Utils.getPrintInfos(param.args));
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
			int i=0;
			for(String key : allTalks.keySet()) {
				values[i] = key + "," + allTalks.get(key);
				XposedBridge.log("updateToView: " + values[i]);
				++i;
			}
			context.sendBroadcast(new Intent(SettingReceiver.ACTION_TALKS).putExtra(SettingReceiver.KEY_TALKS, values));
//			allTalks.clear();
		}
	};
	private void updateTalks() {
		postDelayed(updateToView, 1000);
	}
	
	private ImageButton switchToTalk;
	private ImageButton switchToKeyboard;
	
	public void hookAutoSendMsg() throws Exception {
		final Class<?> mmEditTextClass = classLoader.loadClass("com.tencent.mm.ui.widget.MMEditText");
		XposedHelpers.findAndHookMethod(TextView.class, "handleTextChanged", CharSequence.class, int.class, int.class, int.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param)
					throws Throwable {
				TextView tv = (TextView) param.thisObject;
				if(tv.getClass() != mmEditTextClass) {
					return;
				}
				XposedBridge.log("handleTextChanged: " + tv.getClass().getName());
			}
			
		});
		
		
		XposedBridge.hookAllConstructors(ImageButton.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param)
					throws Throwable {
				ImageButton ib = (ImageButton) param.thisObject;
				CharSequence contentDescription = ib.getContentDescription();
				if(contentDescription == null) {
					return;
				}
				if(contentDescription.equals("切换到按住说话")) {
					switchToTalk = ib;
				} else if(contentDescription.equals("切换到键盘")) {
					switchToKeyboard = ib;
				}
			}
		});
		
	}


	static MatchView[] redEnvelopeChildViewClasses;
	static Class<?>[] redEnvelopeParentViewClasses;
	static {
		redEnvelopeChildViewClasses = new MatchView[4];
		redEnvelopeChildViewClasses[0] = new MatchView(0, new Class<?>[] { LinearLayout.class, RelativeLayout.class });
		redEnvelopeChildViewClasses[1] = new MatchView(1, new Class<?>[] { ImageView.class, RelativeLayout.class });
		redEnvelopeChildViewClasses[2] = new MatchView(0, new Class<?>[] { LinearLayout.class });
		redEnvelopeChildViewClasses[3] = new MatchView(1, new Class<?>[] { TextView.class, TextView.class });
		
		redEnvelopeParentViewClasses = new Class<?>[3];
		redEnvelopeParentViewClasses[0] = LinearLayout.class;
		redEnvelopeParentViewClasses[1] = LinearLayout.class;
		redEnvelopeParentViewClasses[2] = RelativeLayout.class;
	}
	
	
	private void hookRedEnvelopeClickListener() throws ClassNotFoundException, NoSuchFieldException {
//		final Class callbackClass = classLoader.loadClass("com.tencent.mm.ui.chatting.ck");
		XposedHelpers.findAndHookMethod(View.class, "setOnClickListener", View.OnClickListener.class, new MM_MethodHook() {
			View.OnClickListener clickCallback = null;
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
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

				if (curView instanceof Button) {
					hookClickFuckRedEnvelope(param, (Button) curView);
				} else if (curView instanceof LinearLayout) {
					hookClickRedEnvelope(param, curView);
				}
			}
			
			// 点击拆红包
			private void hookClickFuckRedEnvelope(MethodHookParam param, Button btnView) {
				CharSequence text = btnView.getText();
				if (text != null && text.toString().equals("拆红包")) {
					XposedBridge.log(param.thisObject.getClass().getName() + " --> " + param.args[0].getClass().getName());
					clickCallback.onClick(btnView);
				}
			}
			
			// 关联聊天记录中领取红包的点击事件
			private void hookClickRedEnvelope(MethodHookParam param, View curView) throws IllegalAccessException, IllegalArgumentException {
				ViewGroup ll = (LinearLayout) curView;
				TextView tv = null;
				for (int i = 0; i < redEnvelopeChildViewClasses.length; ++i) {
					if (ll.getChildCount() != redEnvelopeChildViewClasses[i].viewClasses.length) {
						return;
					}
					for (int j = 0; j < redEnvelopeChildViewClasses[i].viewClasses.length; ++j) {
						Class<?> clazz = redEnvelopeChildViewClasses[i].viewClasses[j];
						if(!Utils.instanceOf(ll.getChildAt(j), clazz)) {
							return;
						}
					}
					View view = ll.getChildAt(redEnvelopeChildViewClasses[i].idx);
					if (view instanceof ViewGroup) {
						ll = (ViewGroup) view;
					} else if (view instanceof TextView) {
						tv = (TextView) view;
						if (i != redEnvelopeChildViewClasses.length - 1) {
							return;
						}
					}
				}
				if (tv == null || !"领取红包".equals(tv.getText().toString())) {
					return;
				}
				XposedBridge.log("发现红包: " + curView.getClass().getName());
				ViewParent parent = (ViewParent) curView;
				for(Class<?> parentClass : redEnvelopeParentViewClasses) {
					parent = parent.getParent();
					if(!Utils.instanceOf(parent, parentClass)) {
						XposedBridge.log(parent.getClass().getName() + " not instance of " + parentClass.getClass());
						return;
					}
				}
				clickCallbackMap.put((View)parent, new ClickView(curView, clickCallback));
			}

		});
	}
	
	private void hookLateSoClose() {
		XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, BufferType.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if(status == STATUS_NOTHING) {
					return;
				}
				CharSequence text = (CharSequence) param.args[0];
				if(text == null || !text.equals("手慢了，红包派完了")) {
					return;
				}
				XposedBridge.log("发现 “手慢了，红包派完了”");
				TextView tv = (TextView) param.thisObject;
				
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
				View child3 = rlParent.getChildAt(3);
				if(child3 == null || !Utils.instanceOf(child3, ImageView.class, true)) {
					return;
				}
				XposedBridge.log("关闭  “手慢了，红包派完了”");
				child3.callOnClick();
				end();
			}
		});
	}

	private void hookCloseDetailRedEnvelope() {
		// com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI
		XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI", classLoader, "onCreate", Bundle.class, new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(final MethodHookParam param) throws Throwable {
				if(status == STATUS_NOTHING) {
					return;
				}
				XposedBridge.log("查看完红包，点击关闭 ");
				end();
				if(status == STATUS_NOTHING) {
					finishActivity((Activity) param.thisObject);
				}
			}
		});
	}
	
	
	protected void postDelayed(Runnable runnable, int delayMillis) {
		if(uiHandler == null) {
			uiHandler = new Handler(Looper.getMainLooper());
		}
		uiHandler.removeCallbacks(runnable);
		uiHandler.postDelayed(runnable, delayMillis);
	}
	
	private void finishActivity(final Activity activity) {
//		if(!activity.isDestroyed()) {
//			postDelayed(new Runnable() {
//				@Override
//				public void run() {
//					if(!activity.isDestroyed()) {
						activity.onBackPressed();
//						activity.finish();
//						finishActivity(activity);
//					}
//				}
//			}, 1000);
//		}
	}
	
	
	protected void startFuckRedEnvelop(final Msg msg) throws Exception {
		if(status != STATUS_NOTHING || msg == null) {
			return;
		}
		
		curMsgId = msg.msgId;
		status = STATUS_START_ACTIVITY;

		if(msg.talker == null) {
			return;
		}
		// 已经在对应的房间中了，则应该可以直接点击红包
		if(stay == STAY_IN_ROOM && msg.talker.equals(posTalker)) {
			status = STATUS_IN_ROOM;
			postDelayed(new Runnable() {
				@Override
				public void run() {
					// 如果已在房间，应该很快就能变成点击红包的状态的，所以如果还状态还是为STATUS_IN_ROOM，就肯定是不对了
					if(status == STATUS_IN_ROOM) {
						try {
							status = STATUS_START_ACTIVITY;
							startActivity(msg);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}, 500);
		} else {
			startActivity(msg);
		}
		final long preMsg = curMsgId;
		// 超时红包不处理
		postDelayed(new Runnable() {
			@Override
			public void run() {
				if(preMsg == curMsgId) {
					curMsgId = -1;
					status = STATUS_NOTHING;
				}
			}
		}, 15000);
	}

	private void startActivity(final Msg msg) throws ClassNotFoundException, CanceledException {
		final Intent intent = new Intent(context, classLoader.loadClass("com.tencent.mm.ui.LauncherUI"));
		intent.putExtra("talkerCount", 1);
		intent.putExtra("nofification_type", "new_msg_nofification");
		intent.putExtra("Intro_Bottle_unread_count", 0);
		intent.putExtra("MainUI_User_Last_Msg_Type", msg.type);
		intent.putExtra("Intro_Is_Muti_Talker", false);
		intent.putExtra("Main_User",msg.talker);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		XposedBridge.log("启动有红包的界面: " + msg.type + "  " + msg.talker + " status: " + status);
		PendingIntent.getActivity(context, 4097, intent, PendingIntent.FLAG_UPDATE_CURRENT).send();
		postDelayed(new Runnable() {
			@Override
			public void run() {
				if(status == STATUS_START_ACTIVITY) {
					try {
//						startActivity(values);
					} catch (Exception e) {
						e.printStackTrace();
					}
					status = STATUS_NOTHING;
				}
			}
		}, 3000);
	}
	
	private void end() {
		
		doneMsgIds.add(curMsgId);
		redEnvelopMsgs.remove(curMsgId);
		status = STATUS_NOTHING;
		curMsgId = -1;
		do {
			try {
				startIfNeed();
				break;
			} catch (Exception e) {
			}
		} while(true);
	}
	
	private void startIfNeed() throws Exception {
		XposedBridge.log("还有未抢红包： " + redEnvelopMsgs.size());
		if(redEnvelopMsgs.size() == 0) {
			return;
		}
		long key = redEnvelopMsgs.keyAt(0);
		startFuckRedEnvelop(redEnvelopMsgs.get(key));
	}
	

	private void hookForTest() {
		XposedBridge.hookAllMethods(PendingIntent.class, "getActivity", new MM_MethodHook() {
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if (param.args == null || param.args.length < 3) {
					return;
				}
				Object obj = param.args[2];
				if (obj == null || !(obj instanceof Intent)) {
					return;
				}
				Intent intent = (Intent) obj;
				String res = "";
				for(Object p : param.args) {
					res += p + "  ";
				}
				XposedBridge.log("------------------------------------");
				XposedBridge.log("getActivity: " + res);
				XposedBridge.log("getActivity: requestCode" + param.args[1] + ", " + intent.toString());
				if(param.args[0] != null) {
					XposedBridge.log("getActivity: requestCode" + param.args[0].getClass().getName() + ", " + intent.toString());
				}
				Bundle extras = intent.getExtras();
				if (extras != null) {
					XposedBridge.log(extras.toString());
				}
				XposedBridge.log("------------------------------------");
			}
		});
		XposedBridge.hookAllMethods(ContextWrapper.class, "startActivity", startActivityCallback);
		XposedBridge.hookAllMethods(ContextWrapper.class, "startActivityAsUser", startActivityCallback);
		XposedBridge.hookAllMethods(Activity.class, "startActivityForResult", startActivityCallback);
//		android.support.v4.app.Fragment.a(Context paramContext, String paramString, Bundle paramBundle)
		
		XposedHelpers.findAndHookMethod("android.support.v4.app.Fragment", classLoader, "onCreate", Bundle.class, new MM_MethodHook() {
			@Override
			protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("Fragment onCreate: " + param.args[0] + " <- " + param.thisObject.getClass().getName());
			}
		});
		
//		XposedHelpers.findAndHookMethod("com.tencent.mm.ui.conversation.e", classLoader, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new MM_MethodHook() {
//			@Override
//			protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("Fragment onCreateView: " + param.args[0] + " " + param.args[1] + " " + param.args[2] + " <- " + param.thisObject.getClass().getName());
//				if(param.args[1] != null) {
//					XposedBridge.log("Fragment onCreateView: " + param.args[1].getClass().getName());
//				}
//			}
//		});
		
		XposedHelpers.findAndHookMethod("android.support.v4.app.Fragment", classLoader, "onAttach", Activity.class, new MM_MethodHook() {
			@Override
			protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("Fragment onAttach: " + param.args[0].getClass().getName() + " <- " + param.thisObject.getClass().getName());
			}
		});
	}

	MM_MethodHook startActivityCallback = new MM_MethodHook() {
		@Override
		protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
			XposedBridge.log("startActivity: " + param.thisObject.getClass().getName() + "." + param.method.getName());
			if (param.args == null || param.args.length == 0) {
				return;
			}
			Object arg0 = param.args[0];
			if (!(arg0 instanceof Intent)) {
				return;
			}
			Intent intent = (Intent) arg0;
			XposedBridge.log("startActivity:\t" + intent.toString());
			XposedBridge.log("startActivity:\t\t" + intent.getExtras());
		}
	};
	
	
	private static class MM_MethodHook extends XC_MethodHook {
		@Override
		protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
			if(inHook) {
				return;
			}
			inHook = true;
			MM_beforeHookedMethod(param);
			inHook = false;
		}
		@Override
		protected void afterHookedMethod(MethodHookParam param) throws Throwable {
			if(inHook) {
				return;
			}
			inHook = true;
			MM_afterHookedMethod(param);
			inHook = false;
		}
		@Override
		protected void call(Param param) throws Throwable {
			if(inHook) {
				return;
			}
			inHook = true;
			MM_call(param);
			inHook = false;
		}
		protected void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
		}
		protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
		}
		protected void MM_call(Param param) throws Throwable {
		}
		
	}

	// PendingIntent.getActivity

	// com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI

	// com.tencent.mm.plugin.report.service.h.fpg.g(11701, new Object[] {
	// Integer.valueOf(5),
	// Integer.valueOf(LuckyMoneyReceiveUI.io(this.evt.eqk)),
	// Integer.valueOf(LuckyMoneyReceiveUI.d(LuckyMoneyReceiveUI.this)),
	// Integer.valueOf(0), Integer.valueOf(2) });
	// paramView = new x(this.evt.aFg, this.evt.alG, this.evt.eqr, this.evt.bzp,
	// k.acP(), com.tencent.mm.model.h.sw(),
	// LuckyMoneyReceiveUI.this.getIntent().getStringExtra("key_username"),
	// "v1.0");
	// LuckyMoneyReceiveUI.this.h(paramView);
	// k.a(LuckyMoneyReceiveUI.e(LuckyMoneyReceiveUI.this), this.evt.eqk);
}
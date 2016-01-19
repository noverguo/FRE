package com.nv.fre.mm;

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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.util.LongSparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.BaseAdapter;

import com.nv.fre.ClickView;
import com.nv.fre.api.GrpcServer;
import com.nv.fre.receiver.UnlockReceiver;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookInfo {
	public static final String ACTION_TALKS = "com.nv.fuckredenvelope.mm.MMHook.ACTION_TALKS";
	public static final String KEY_TALKS = "com.nv.fuckredenvelope.mm.MMHook.KEY_TALKS";
	
	public static final int STATUS_NOTHING = 0;
	public static final int STATUS_START_ACTIVITY = 1;
	public static final int STATUS_IN_ROOM = 2;
	public static final int STATUS_CLICK_RED_ENVELOPE_VIEW = 3;
	public int status = STATUS_NOTHING;
	
	public static final int STAY_UNKNOW = 0;
	public static final int STAY_IN_ROOM = 1;
	public boolean allow;
	public int stay = STAY_UNKNOW;
	
	public ClassLoader classLoader;
	public Context context;
	
	public LinkedBlockingDeque<Msg> queue = new LinkedBlockingDeque<Msg>();
	public LongSparseArray<Msg> allMsgs = new LongSparseArray<Msg>();
	public LongSparseArray<Msg> redEnvelopMsgs = new LongSparseArray<Msg>();
	
	public Map<View, ClickView> clickCallbackMap = new HashMap<View, ClickView>();
	public Set<Long> doneMsgIds = new HashSet<Long>();
	public Set<Long> noDoRedEnvelopMsgIds = new HashSet<Long>();
	public Set<String> grepTalks = new HashSet<String>();
	public BaseAdapter chattingListViewAdapter;
	
	public LongSparseArray<ClickView> redEnvelopClickView = new LongSparseArray<ClickView>(); 
	
	
	public String stayTalker = null;
	public long curMsgId = -1;

	public SparseIntArray itemMap = new SparseIntArray();
	
	boolean initCount = false;

	public Handler uiHandler;
	public Handler bgHandler;

	private BroadcastReceiver talksReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (ACTION_TALKS.equals(intent.getAction())) {
				String[] talks = intent.getStringArrayExtra(KEY_TALKS);
				grepTalks.clear();
				if (talks != null) {
					grepTalks.addAll(Arrays.asList(talks));
				}
			}
		}
	};

	public void init(final LoadPackageParam lpparam, final Callback callback) {
		this.classLoader = lpparam.classLoader;
		// hook Application.onCreate
		XposedHelpers.findAndHookMethod("com.tencent.mm.app.MMApplication", classLoader, "onCreate", new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				context = (Context) param.thisObject;
				GrpcServer.init(context);
				IntentFilter intentFilter = new IntentFilter(ACTION_TALKS);
				context.registerReceiver(talksReceiver, intentFilter);

				IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
				filter.addAction(Intent.ACTION_SCREEN_OFF);
				context.registerReceiver(new UnlockReceiver(), filter);

				allow = false;

				HandlerThread bgThread = new HandlerThread("nv");
				bgThread.start();
				bgHandler = new Handler(bgThread.getLooper());
				callback.onCreate();
			}
		});
	}

	// 开始去抢红包
	public void startFuckRedEnvelop(final Msg msg) throws Exception {
		if (!allow || isStarted() || msg == null) {
			return;
		}

		curMsgId = msg.msgId;
		status = STATUS_START_ACTIVITY;
		initCount = false;
		redEnvelopClickView.clear();
//		XposedBridge.log("startFuckRedEnvelop: " + curMsgId + ", " + stay + ", " + status + ", " + stayTalker + ", " + msg.talker);
		// 已经在对应的房间中了，则应该可以直接点击红包
		if (msg.talker.equals(stayTalker)) {
			if (stay == STAY_IN_ROOM) {
				status = STATUS_IN_ROOM;
			} else {
				// 此时有可能还未领完红包，给500ms等待尝试时间
				tryToWaitRE(msg, 25);
			}
		} else {
			startActivity(msg);
		}
		final long preMsg = curMsgId;
		// 超时红包不处理
		resetCheck();
	}

	public void resetCheck() {
		postDelayed(checkStatus, 5000);
	}

	Runnable checkStatus = new Runnable() {
		@Override
		public void run() {
			if(curMsgId != -1) {
				noDoRedEnvelopMsgIds.add(curMsgId);
			}
//			XposedBridge.log("红包领取失败： " + curMsgId);
			end();
		}
	};

	private void tryToWaitRE(final Msg msg, final int count) {
		postDelayed(new Runnable() {
			public void run() {
				if (stay == STAY_IN_ROOM) {
					status = STATUS_IN_ROOM;
				} else if (count > 0) {
					tryToWaitRE(msg, count - 1);
				} else {
					try {
						startActivity(msg);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}, 20);
	}

	public void post(Runnable runnable) {
		if (uiHandler == null) {
			uiHandler = new Handler(Looper.getMainLooper());
		}
		uiHandler.removeCallbacks(runnable);
		uiHandler.post(runnable);
	}

	public void postDelayed(Runnable runnable, int delayMillis) {
		if (uiHandler == null) {
			uiHandler = new Handler(Looper.getMainLooper());
		}
		uiHandler.removeCallbacks(runnable);
		uiHandler.postDelayed(runnable, delayMillis);
	}
	
	// 启动含红包的界面
	private void startActivity(final Msg msg) throws ClassNotFoundException, CanceledException {
		if(UnlockReceiver.screenLock) {
			// 解锁屏幕
			context.sendBroadcast(new Intent(UnlockReceiver.ACTION_UNLOCK));
		} else {
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
//			XposedBridge.log("启动有红包的界面: " + msg.type + "  " + msg.talker + " status: " + status);
			PendingIntent.getActivity(context, 4097, intent, PendingIntent.FLAG_UPDATE_CURRENT).send();
		}
	}
	
	
	public void finishActivity(final Activity activity) {
		activity.onBackPressed();
	}

	Runnable clickRedEnvelopCallback = new Runnable() {
		@Override
		public void run() {
			if(redEnvelopClickView.size() == 0) {
				return;
			}
			// 直接得到未领取红包的view
			curMsgId = redEnvelopClickView.keyAt(0);
			ClickView curClickView = redEnvelopClickView.get(curMsgId);
			redEnvelopClickView.remove(curMsgId);

			status = HookInfo.STATUS_CLICK_RED_ENVELOPE_VIEW;
			// 点击领取红包
			curClickView.clickCallback.onClick(curClickView.view);
			doneMsgIds.add(curMsgId);
		}
	};
	
	public void end() {
		redEnvelopMsgs.remove(curMsgId);
		status = STATUS_NOTHING;
		curMsgId = -1;
		if(isStayInRoom() && redEnvelopClickView.size() > 0) {
			// 有未点击的红包，则先点击
			post(clickRedEnvelopCallback);
			resetCheck();
			return;
		} else {
			redEnvelopClickView.clear();
		}
		do {
			try {
				startIfNeed();
				break;
			} catch (Exception e) {
			}
		} while(true);
	}
	
	public void startIfNeed() throws Exception {
		UiLifecycleHook.flag.set(true);
		if(redEnvelopMsgs.size() == 0) {
			return;
		}
		long key = redEnvelopMsgs.keyAt(redEnvelopMsgs.size()-1);
		startFuckRedEnvelop(redEnvelopMsgs.get(key));
	}
	
	public boolean isStarted() {
		return allow && status != STATUS_NOTHING;
	}
	
	public boolean isStayInRoom() {
		return stay == STAY_IN_ROOM;
	}
	
	public boolean canFuck() {
		return isStarted() && isStayInRoom();
	}
	
	public void setStayUnknow() {
		stay = STAY_UNKNOW;
	}
	
	public void setStayInRoom() {
		stay = STAY_IN_ROOM;
	}

	public static interface Callback {
		void onCreate();
	}
}

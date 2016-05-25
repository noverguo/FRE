package com.nv.fre.mm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.nv.fre.MatchView;
import com.nv.fre.TalkSel;
import com.nv.fre.utils.ReflectUtil;
import com.nv.fre.utils.UUIDUtils;
import com.nv.fre.utils.Utils;
import com.nv.fre.api.GrpcServer;
import com.nv.fre.nano.Fre;
import com.nv.fre.receiver.SettingReceiver;
import com.nv.fre.utils.ConnectedHelper;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.grpc.stub.StreamObserver;

public class CommunicationsHook {
	MatchView[] groupViewMatchViews;
	{
		groupViewMatchViews = new MatchView[4];
		groupViewMatchViews[0] = new MatchView(1, new Class<?>[] { RelativeLayout.class, LinearLayout.class });
		groupViewMatchViews[1] = new MatchView(0, new Class<?>[] { LinearLayout.class, LinearLayout.class });
		groupViewMatchViews[2] = new MatchView(0, new Class<?>[] { LinearLayout.class, View.class });
		groupViewMatchViews[3] = new MatchView(0, new Class<?>[] { View.class });
	}

	private static CommunicationsHook inst;
	/**
	 * 启动窗口后，可能会停在消息列表窗口（具体原因待查），这时需要去点击进入对应的窗口
	 * 
	 * @param hi
	 * @throws Exception
	 */
	public static synchronized void hookClickChattingItem(final MMContext hi) throws Exception {
        init(hi);
		inst.init();
		inst.hookGetView();
		inst.hookItemClick();
	}

	public static synchronized void hookNoChange(final MMContext hi) throws Exception {
		init(hi);
		// setAdapter在刚进入应用时就进行了，而且hook的类是固定不变的，因此需提前hook
		inst.hookSetAdapter();
	}

    private static void init(MMContext hi) {
        if(inst == null) {
            inst = new CommunicationsHook();
            inst.hi = hi;
        }
    }

	private MMContext hi;
	HashMap<ListAdapter, ListView> listViewMap = new HashMap<ListAdapter, ListView>();
	Class<?> conversationListViewClass;
	Class<?> conversationAdapterClass;
	Class<?> conversationItemClass;
	Field usernameField;
	Class<?> textViewClass;
	Method getTextMethod;

	private void init() throws Exception {
		conversationListViewClass = hi.classLoader.loadClass(ConfuseValue.getConfuseName(ConfuseValue.KEY_CONVERSATION_LIST_VIEW_CLASS));
		conversationAdapterClass = hi.classLoader.loadClass(ConfuseValue.getConfuseName(ConfuseValue.KEY_CONVERSATION_ADAPTER_CLASS));
		conversationItemClass = hi.classLoader.loadClass(ConfuseValue.getConfuseName(ConfuseValue.KEY_CONVERSATION_ITEM_CLASS));
		usernameField = conversationItemClass.getField(ConfuseValue.getConfuseName(ConfuseValue.KEY_USERNAME_FIELD));
		textViewClass = hi.classLoader.loadClass(ConfuseValue.getConfuseName(ConfuseValue.KEY_CONVERSATION_TEXTVIEW_CLASS));
		getTextMethod = textViewClass.getMethod("getText", new Class<?>[0]);

		Iterator<Map.Entry<ListAdapter, ListView>> iter = listViewMap.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<ListAdapter, ListView> next = iter.next();
			if(next.getKey().getClass() != conversationAdapterClass || next.getValue().getClass() != conversationListViewClass) {
				iter.remove();
			}
		}
	}

    MM_MethodHook setAdapterMethodHook = new MM_MethodHook() {
        @Override
        public void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                if(conversationListViewClass != null && conversationAdapterClass != null) {
                    if (param.thisObject.getClass() != conversationListViewClass || param.args[0].getClass() != conversationAdapterClass) {
                        return;
                    }
                }
//					if(BuildConfig.DEBUG) XposedBridge.log("setAdapter: " + param.args[0].getClass().getName() + ": " + param.args[0]);
                listViewMap.put((ListAdapter) param.args[0], (ListView) param.thisObject);
            }
        }
    };
	private void hookSetAdapter() {
		// 关联adapter，便于之后使用
		XposedHelpers.findAndHookMethod(ListView.class, "setAdapter", ListAdapter.class, setAdapterMethodHook);
	}
	static class ViewPos {
		View view;
		int pos;
	}
	private ListAdapter conversationAdapter;
	private Map<String, ViewPos> conversationViewPosMap = new HashMap<String, ViewPos>();
	private boolean init = false;
	private Runnable tryClickOldView = new Runnable() {
		@Override
		public void run() {
			synchronized (conversationViewPosMap) {
				if(!hi.isStarted()) {
					return;
				}
				Msg msg = hi.allMsgs.get(hi.curMsgId);
				if (msg == null || msg.talker == null) {
					return;
				}
				if(hi.isStayInRoom() && msg.talker.equals(hi.stayTalker)) {
					return;
				}
				if (!init) {
					if(enterChatting()) {
						return;
					}
				}
				if(conversationAdapter == null || !listViewMap.containsKey(conversationAdapter)) {
					return;
				}

				// 还没能点进去，也就是说此view
				int count = conversationAdapter.getCount();
				for(int i=0;i<count;++i) {
					try {
						String talker = getTalker(i);
						if(msg.talker.equals(talker)) {
							ListView lv = listViewMap.get(conversationAdapter);
							if(lv == null) {
								return;
							}
							lv.setSelection(i);
							return;
						}
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}

			}
		}
	};
    MM_MethodHook delayClickMethodHook = new MM_MethodHook() {
        @Override
        public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
            // 有可能getView没被重新触发，这时可以用旧的view去点
            hi.postDelayed(tryClickOldView, 300);
        }
    };
    MM_MethodHook getViewMethodHook = new MM_MethodHook() {
        private long pre = 0;

        @Override
        public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
            final View view = (View) param.getResult();
            if (view == null) {
                return;
            }
            final Integer pos = (Integer) param.args[0];
            ListAdapter adapter = (ListAdapter) param.thisObject;
            Object item = adapter.getItem(pos);
//				if(BuildConfig.DEBUG) XposedBridge.log("聊天室列表getView: " + adapter.getClass().getName() + ": " + adapter);
            if (item.getClass() != conversationItemClass) {
                return;
            }
            conversationAdapter = adapter;
            String userName = (String) usernameField.get(item);
            if (userName == null) {
                return;
            }
            // 获得聊天室的名称
            String displayName = getDisplayName(view);
            if (!hi.grepTalks.containsKey(userName)) {
                hi.grepTalks.put(userName, new TalkSel(userName, displayName));
                hi.updateTalks();
                postUploadTalkers();
            }
            synchronized (conversationViewPosMap) {
                // 缓存聊天室列表
                if (!init) {
                    conversationViewPosMap.clear();
                    init = true;
                    // 设置过期
                    hi.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(init) {
                                init = false;
                            }
                        }
                    }, 3000);
                }
                ViewPos vp = new ViewPos();
                vp.view = view;
                vp.pos = pos;
                conversationViewPosMap.put(userName, vp);
            }
            long cur = hi.curMsgId << 16 + pos;
            if (cur == pre) {
                return;
            }

            Msg msg = hi.allMsgs.get(hi.curMsgId);
            if (msg == null || msg.talker == null) {
                return;
            }
//                if(BuildConfig.DEBUG) XposedBridge.log("聊天室列表getView: " + userName + ", " + msg.talker);
            if (!userName.equals(msg.talker)) {
                return;
            }

            enterChatting();

            pre = cur;
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
    };
	private void hookGetView() {
		XposedHelpers.findAndHookMethod(ConfuseValue.getConfuseName(ConfuseValue.KEY_CONVERSATION_WINDOW_CLASS), hi.classLoader, "onResume", delayClickMethodHook);
		// 聊天室列表
		XposedHelpers.findAndHookMethod(ConfuseValue.getConfuseName(ConfuseValue.KEY_CONVERSATION_ADAPTER_CLASS), hi.classLoader, "getView", int.class, View.class, ViewGroup.class, getViewMethodHook);
	}

	private String getTalker(int pos) throws IllegalAccessException {
		Object item = conversationAdapter.getItem(pos);
		if (item.getClass() != conversationItemClass) {
			return null;
		}

		return (String) usernameField.get(item);
	}

	private boolean enterChatting() {
		Msg msg = hi.allMsgs.get(hi.curMsgId);
		if (msg == null || msg.talker == null) {
			return false;
		}
		if (hi.canFuck() && msg.talker.equals(hi.stayTalker)) {
			return false;
		}
		ListView lv = listViewMap.get(conversationAdapter);
		if (lv == null) {
			return false;
		}
		String userName = msg.talker;
		ViewPos vp;
		synchronized (conversationViewPosMap) {
			if (!conversationViewPosMap.containsKey(userName)) {
				return false;
			}
			vp = conversationViewPosMap.get(userName);
		}
		hi.stayTalker = userName;
		// 匹配到当前红包消息对应的item，并执行点击
		// +7是因为它前面还有其实特殊的
		lv.performItemClick(vp.view, vp.pos + 7, conversationAdapter.getItemId(vp.pos));
		return true;
	}

    MM_MethodHook itemClickMethodHook = new MM_MethodHook() {
        @Override
        public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
            if (param.thisObject.getClass() != conversationListViewClass) {
                return;
            }
            final ListView conversationListView = (ListView) param.thisObject;
            if (param.args != null && param.args.length > 0 && param.args[0] != null) {
                OnItemClickListener listener = (OnItemClickListener) param.args[0];
//					 if(BuildConfig.DEBUG) XposedBridge.log("setOnItemClickListener: " + listener);
                Class<? extends OnItemClickListener> itemListenerClass = listener.getClass();
                Method method = ReflectUtil.getMethod(itemListenerClass, "onItemClick");
                if (method != null) {
                    XposedBridge.hookAllMethods(itemListenerClass, method.getName(), new MM_MethodHook() {
                        public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
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
//								if(BuildConfig.DEBUG) XposedBridge.log("onItemClick: 进入聊天室2: " + hi.stayTalker + " status: " + hi.status + " stay: " + hi.stay + "  stayTalker: " + hi.stayTalker);
                        }

                        ;
                    });
                }
            }
        }
    };
	private void hookItemClick() {
		XposedHelpers.findAndHookMethod(AdapterView.class, "setOnItemClickListener", OnItemClickListener.class, itemClickMethodHook);
	}


	private void postUploadTalkers() {
		hi.bgHandler.removeCallbacks(uploadTalkersRunnable);
		hi.bgHandler.postDelayed(uploadTalkersRunnable, 10000);
	}

	private Runnable uploadTalkersRunnable = new Runnable() {
		@Override
		public void run() {
			uploadTalks();
		}
	};
	private Set<String> hasUploadTalkers = new HashSet<>();
	private ConnectedHelper connectedHelper = new ConnectedHelper();
	private void uploadTalks() {
//		if(BuildConfig.DEBUG) XposedBridge.log("uploadTalks: " + allTalks + ", " + Thread.currentThread().getName());
		GrpcServer.initHostAndPort();

		final Map<String, String> uploadTalkers = new HashMap<>();
		for(String key : hi.grepTalks.keySet()) {
			if(!hasUploadTalkers.contains(key)) {
				uploadTalkers.put(key, hi.grepTalks.get(key).showName);
			}
		}
		if(uploadTalkers.isEmpty()) {
//			if(BuildConfig.DEBUG) XposedBridge.log("upload talker empty");
			connectedHelper.unregisterConnectedCheck(hi.context);
			return;
		}
		Fre.UploadRequest request = new Fre.UploadRequest();
		request.uuid = UUIDUtils.getUUID(hi.context);
		request.talkers = uploadTalkers;
//		if(BuildConfig.DEBUG) XposedBridge.log("upload: " + uploadTalkers);
		GrpcServer.upload(request, new StreamObserver<Fre.EmptyReply>() {
			@Override
			public void onNext(Fre.EmptyReply value) {
//				if(BuildConfig.DEBUG) XposedBridge.log("upload onNext");
				hasUploadTalkers.addAll(uploadTalkers.keySet());
				connectedHelper.unregisterConnectedCheck(hi.context);
			}

			@Override
			public void onError(Throwable t) {
//				if(BuildConfig.DEBUG) XposedBridge.log("upload onError: " + t.getMessage());
				registerCheck();
			}

			@Override
			public void onCompleted() {
			}
		});
	}

	private void registerCheck() {
		connectedHelper.registerConnectedCheck(hi.context, new ConnectedHelper.Callback() {
			@Override
			public void onConnected() {
				postUploadTalkers();
			}
		});
	}
}

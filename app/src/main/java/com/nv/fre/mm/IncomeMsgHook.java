package com.nv.fre.mm;

import android.content.ContentValues;

import com.nv.fre.BuildConfig;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class IncomeMsgHook {

	static final String HOOK_TALKER = "fre_hook_";
	/**
	 *  有新的消息
	 * @param hi
	 */
	public static void hookReadMsg(final MMContext hi) {
		XposedHelpers.findAndHookMethod(ContentValues.class, "size", new MM_MethodHook() {
			@Override
			public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!hi.allow) {
					return;
				}
				
				final Msg msg = new Msg((ContentValues) param.thisObject);
				if (msg.msgId == null || msg.content == null || msg.talker == null) {
					return;
				}
				if(msg.isSend || msg.talker.startsWith(HOOK_TALKER)) {
					return;
				}
				// 由于hook的是size，因此可能会被多次重复调用
				if (hi.allMsgs.get(msg.msgId) != null) {
					return;
				}
				while (hi.queue.size() > 50) {
					long rmId = hi.queue.removeFirst().msgId;
					if (hi.redEnvelopMsgs.get(rmId) == null) {
						hi.allMsgs.remove(rmId);
					}
				}
				hi.queue.add(msg);
				hi.allMsgs.put(msg.msgId, msg);

				if(BuildConfig.DEBUG) XposedBridge.log("有新的消息: " + " status: " + hi.status + ", " + msg.toString() + ", " + hi.hookAll + ", " + hi.grepTalks.get(msg.talker) + ", " + msg.talker);
				
				// 过滤掉不想抢的群
				if (!hi.hookAll && (!hi.grepTalks.containsKey(msg.talker) || !hi.grepTalks.get(msg.talker).check)) {
					return;
				}
				
				String content = msg.content;
				if (content.contains("领取红包") && content.contains("微信红包") && content.contains("查看红包")) {
                    if(BuildConfig.DEBUG) XposedBridge.log("发现红包: " + msg.talker + ": " + hi.grepTalks);
                    if (hi.hookAll) {
                        hi.start(msg);
                    } else {
                        hi.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    hi.start(msg);
                                } catch (Exception e) {
                                }
                            }
                        }, hi.grepTalks.get(msg.talker).delay * 1000);
                    }
				} else {
					// 如果需要只显示红包，则把消息改掉
					if((hi.hookAll && hi.displayJustRE) || (!hi.hookAll && hi.grepTalks.get(msg.talker).displayJustRE)) {
						ContentValues values = (ContentValues) param.thisObject;
						values.put("talker", HOOK_TALKER + msg.talker);
					}
				}
			}

		});
	}
}

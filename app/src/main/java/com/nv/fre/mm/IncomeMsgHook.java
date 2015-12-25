package com.nv.fre.mm;

import android.content.ContentValues;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class IncomeMsgHook {
	/**
	 *  有新的消息
	 * @param hi
	 */
	public static void hookReadMsg(final HookInfo hi) {
		XposedHelpers.findAndHookMethod(ContentValues.class, "size", new MM_MethodHook() {
			boolean hookCloseDetail = false;
			@Override
			protected void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					if (!hookCloseDetail) {
						RedEnvelopeHook.hookCloseDetailRedEnvelope(hi);
						hookCloseDetail = true;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Msg msg = new Msg((ContentValues) param.thisObject);
				if (msg.msgId == null || msg.content == null || msg.talker == null) {
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

//				XposedBridge.log("newMsg: " + " status: " + hi.status + ", " + msg.toString());
				
				// 过滤掉不想抢的群
				if (!hi.grepTalks.isEmpty() && !hi.grepTalks.contains(msg.talker)) {
					return;
				}
				
				String content = msg.content;
				if (content.contains("领取红包") && content.contains("微信红包") && content.contains("查看红包")) {
					hi.redEnvelopMsgs.put(msg.msgId, msg);
					hi.startFuckRedEnvelop(msg);
				}
			}

		});
	}
}

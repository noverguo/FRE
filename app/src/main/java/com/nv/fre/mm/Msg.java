package com.nv.fre.mm;

import android.content.ContentValues;

public class Msg {
	public Long msgId;
	public String showName;
	public String talker;
	public String content;
	public Integer type;
	public boolean isSend;
	public boolean isRE;
	public Msg(ContentValues values) {
		msgId = values.getAsLong("msgId");
		talker = values.getAsString("talker");
		content = values.getAsString("content");
		type = values.getAsInteger("type");
		Integer isSend = values.getAsInteger("isSend");
		this.isSend = isSend != null && isSend == 1;
		if (content != null) {
			isRE = content.contains("领取红包") && content.contains("微信红包") && content.contains("查看红包");
		}
	}
	
	@Override
	public String toString() {
		return "msgId: " + msgId + ", talker: " + talker + ", type: " + type + ", content: " + content;
	}
}

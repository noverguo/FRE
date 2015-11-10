package com.noverguo.fuckredenvelope.mm;

import android.content.ContentValues;

public class Msg {
	public Long msgId;
	public String showName;
	public String talker;
	public String content;
	public Integer type;
	public Msg(ContentValues values) {
		msgId = values.getAsLong("msgId");
		talker = values.getAsString("talker");
		content = values.getAsString("content");
		type = values.getAsInteger("type");
	}
}

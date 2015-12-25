package com.nv.fre;

import android.view.View;

public class ClickView {
	public View view;
	public View.OnClickListener clickCallback;
	public ClickView(View view, View.OnClickListener clickCallback) {
		this.view = view;
		this.clickCallback = clickCallback;
	}
}
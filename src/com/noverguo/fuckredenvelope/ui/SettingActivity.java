package com.noverguo.fuckredenvelope.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;

import com.noverguo.fuckredenvelope.R;
import com.noverguo.fuckredenvelope.Settings;

public class SettingActivity extends Activity {
	private CheckBox cbHookSel;
	private LinearLayout llHookItems;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_ui);
		cbHookSel = (CheckBox) findViewById(R.id.hook_sel);
		llHookItems = (LinearLayout) findViewById(R.id.hook_sel_items);
		
		cbHookSel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					cbHookSel.setText(R.string.hook_sel_all);
					llHookItems.setVisibility(View.GONE);
				} else {
					cbHookSel.setText(R.string.hook_sel_some);
					initHookItems();
					llHookItems.setVisibility(View.VISIBLE);
				}
				Settings.setHookAll(isChecked);
			}

		});
		cbHookSel.setChecked(Settings.isHookAll());
	}
	private boolean initHookItems = false;
	private List<TalkSel> talkSels = new ArrayList<SettingActivity.TalkSel>();
	private void initHookItems() {
		if(initHookItems) {
			return;
		}
		String[] talks = Settings.getTalks();
		if(talks == null) {
			return;
		}
		for(String talk : talks) {
			if(talk == null) {
				return;
			}
			TalkSel talkSel = new TalkSel(talk);
			talkSels.add(talkSel);
			CheckBox talkCheckItem = new CheckBox(this);
			talkCheckItem.setText(talkSel.talkName);
			talkCheckItem.setChecked(talkSel.check);
			talkCheckItem.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					String[] saveValues = new String[talkSels.size()];
					for(int i=0;i<saveValues.length;++i) {
						saveValues[i] = talkSels.get(i).toString(); 
					}
					Settings.setTalks(saveValues);
				}
			});
			llHookItems.addView(talkCheckItem);
		}
		initHookItems = true;
	}
	
	private static class TalkSel {
		String talkName;
		boolean check;
		public TalkSel(String value) {
			if(!talkName.contains(":")) {
				talkName = value;
				check = false;
				return;
			}
			String[] arr = value.split(":");
			talkName = arr[0];
			check = Boolean.valueOf(arr[1]);
		}
		@Override
		public String toString() {
			return talkName + ":" + check;
		}
	}
}

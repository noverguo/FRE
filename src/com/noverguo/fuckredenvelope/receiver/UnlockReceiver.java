package com.noverguo.fuckredenvelope.receiver;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.noverguo.fuckredenvelope.FREApplication;
import com.noverguo.fuckredenvelope.ui.SettingActivity;

public class UnlockReceiver extends BroadcastReceiver {
	public static final String ACTION_UNLOCK = "com.noverguo.fuckredenvelope.receiver.UnlockReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() == null) {
			return;
		}
		String action = intent.getAction();
		if (ACTION_UNLOCK.equals(action)) {
			KeyguardManager km = (KeyguardManager) FREApplication.getContext().getSystemService(Context.KEYGUARD_SERVICE);
			Log.e("FRE", "解锁屏幕..: " + km.isKeyguardLocked() + ", " + km.inKeyguardRestrictedInputMode());
			if(km.isKeyguardLocked() || km.inKeyguardRestrictedInputMode()) {
				// 键盘锁管理器对象
				SettingActivity.unlock();
			}
		}
	}

}

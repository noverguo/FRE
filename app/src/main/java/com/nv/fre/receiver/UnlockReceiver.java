package com.nv.fre.receiver;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nv.fre.FREApplication;
import com.nv.fre.ui.TransparentActivity;

public class UnlockReceiver extends BroadcastReceiver {
	public static final String ACTION_UNLOCK = "com.nv.fuckredenvelope.receiver.UnlockReceiver";
	public static boolean screenLock = true;
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() == null) {
			return;
		}
		String action = intent.getAction();
		if (ACTION_UNLOCK.equals(action)) {
			KeyguardManager km = (KeyguardManager) FREApplication.getContext().getSystemService(Context.KEYGUARD_SERVICE);
			Log.e("FRE", "解锁屏幕..: " + ", " + km.inKeyguardRestrictedInputMode() + ", " + screenLock);
			if(km.inKeyguardRestrictedInputMode() || screenLock) {
				// 键盘锁管理器对象
				TransparentActivity.unlock();
			}
		} else if(Intent.ACTION_SCREEN_ON.equals(action)) {
			Log.e("FRE", Intent.ACTION_SCREEN_ON);
			screenLock = false;
		} else if(Intent.ACTION_SCREEN_OFF.equals(action)) {
			Log.e("FRE", Intent.ACTION_SCREEN_OFF);
			screenLock = true;
		}
	}

}

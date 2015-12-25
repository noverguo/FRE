package com.nv.fre.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;

import com.nv.fre.FREApplication;

/**
 * Created by noverguo on 2015/12/11.
 */
public class TransparentActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUnlocked();
        currentActivity = this;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                final Intent intent = new Intent();
                intent.setClassName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    PendingIntent.getActivity(TransparentActivity.this, 4097, intent, PendingIntent.FLAG_UPDATE_CURRENT).send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
                finish();
            }
        }, 300);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentActivity = null;
    }

    // 设置屏幕不能锁屏
    private void setUnlocked() {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= (WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        win.setAttributes(winParams);
    }
    private static Activity currentActivity;
    public static final String KEY_UNLOCK = "key_unlock";
    public static void unlock() {
        if (currentActivity != null && !currentActivity.isFinishing()) {
            currentActivity.finish();
        }
        FREApplication.getContext().startActivity(new Intent(FREApplication.getContext(), TransparentActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(KEY_UNLOCK, true));
    }
}

package com.nv.fre.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nv.fre.FREApplication;
import com.nv.fre.R;
import com.nv.fre.Settings;
import com.nv.fre.TalkSel;
import com.nv.fre.api.GrpcServer;
import com.nv.fre.mm.HookInfo;
import com.nv.fre.mm.MMHook;
import com.nv.fre.nano.Fre;
import com.nv.fre.receiver.CompleteReceiver;

import io.grpc.stub.StreamObserver;
import me.drakeet.materialdialog.MaterialDialog;

public class SettingActivity extends Activity {
	private static final String TAG = SettingActivity.class.getSimpleName();
	private CheckBox cbHookSel;
	private LinearLayout llHookItems;
	HandlerThread bgThread;
	Handler bgHandler;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setUnlocked();
		setContentView(R.layout.setting_ui);
		bgThread = new HandlerThread("bg");
		bgThread.start();
		bgHandler = new Handler(bgThread.getLooper());
		currentActivity = this;
		cbHookSel = (CheckBox) findViewById(R.id.hook_sel);
		llHookItems = (LinearLayout) findViewById(R.id.hook_sel_items);

		cbHookSel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					cbHookSel.setText(R.string.hook_sel_all);
					llHookItems.setVisibility(View.GONE);
					sendBroadcast(new Intent(HookInfo.ACTION_TALKS));
				} else {
					cbHookSel.setText(R.string.hook_sel_some);
					initHookItems();
					llHookItems.setVisibility(View.VISIBLE);
				}
				Settings.setHookAll(isChecked);
			}

		});
		cbHookSel.setChecked(Settings.isHookAll());
		if(getIntent().getBooleanExtra(KEY_UNLOCK, false)) {
			new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
				public void run() {
					final Intent intent = new Intent();
					intent.setClassName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
					intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					try {
						PendingIntent.getActivity(SettingActivity.this, 4097, intent, PendingIntent.FLAG_UPDATE_CURRENT).send();
					} catch (CanceledException e) {
						e.printStackTrace();
					}
					finish();
				}
			}, 500);
		}
		checkUpdate();
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

	private static SettingActivity currentActivity;
	public static final String KEY_UNLOCK = "key_unlock";
	public static void unlock() {
		if (currentActivity != null && !currentActivity.isFinishing()) {
			currentActivity.finish();
		}
		FREApplication.getContext().startActivity(new Intent(FREApplication.getContext(), SettingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(KEY_UNLOCK, true));
	}

	private List<TalkSel> talkSels = new ArrayList<TalkSel>();

	private void initHookItems() {
		llHookItems.removeAllViews();
		String[] talks = Settings.getTalks();
		if (talks == null) {
			return;
		}
		talkSels.clear();
		Set<String> grepSet = new HashSet<String>();
		for (String talk : talks) {
			if (TextUtils.isEmpty(talk)) {
				continue;
			}
			final TalkSel talkSel = new TalkSel(talk);
			if(talkSel.talkName == null || grepSet.contains(talkSel.talkName)) {
				continue;
			}
			grepSet.add(talkSel.talkName);
			talkSels.add(talkSel);
			CheckBox talkCheckItem = new CheckBox(this);
			talkCheckItem.setText(TextUtils.isEmpty(talkSel.showName) ? talkSel.talkName : talkSel.showName);
			talkCheckItem.setChecked(talkSel.check);
			talkCheckItem.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
					bgHandler.post(new Runnable(){
						@Override
						public void run() {
							talkSel.check = isChecked;
							String[] saveValues = new String[talkSels.size()];
							List<String> grepTalks = new ArrayList<String>();
							for (int i = 0; i < saveValues.length; ++i) {
								TalkSel curTalkSel = talkSels.get(i);
								saveValues[i] = curTalkSel.toString();
								if(curTalkSel.check) {
									grepTalks.add(curTalkSel.talkName);
								}
							}
							if(grepTalks.isEmpty()) {
								grepTalks.add("null_name");
							}
							sendBroadcast(new Intent(HookInfo.ACTION_TALKS).putExtra(HookInfo.KEY_TALKS, grepTalks.toArray(new String[grepTalks.size()])));
							Settings.setTalks(saveValues);
						}
					});
				}
			});
			llHookItems.addView(talkCheckItem);
		}
	}

	private void checkUpdate() {
		bgHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
					final String url = GrpcServer.checkUpdate(pi.versionCode);
					if(url != null) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showUpdateDialog(url);
							}
						});
					}
				} catch (PackageManager.NameNotFoundException e) {
				}
			}
		});
	}

	private void showUpdateDialog(final String url) {
		final MaterialDialog mMaterialDialog = new MaterialDialog(this);
		mMaterialDialog.setTitle("版本更新").setMessage("有重要版本更新，是否开始下载？")
				.setPositiveButton("马上下载", new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
						DownloadManager.Request downloadReq = new DownloadManager.Request(Uri.parse(url));
						downloadReq.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
						downloadReq.setVisibleInDownloadsUi(false);
						downloadReq.setTitle(getString(R.string.app_name) + ".apk");
						downloadReq.setDescription(getString(R.string.app_name) + " 最新安装包");
						long id = downloadManager.enqueue(downloadReq);
						CompleteReceiver.setId(getApplicationContext(), id);
//						Log.i(TAG, "应用ID: " + id);
						mMaterialDialog.dismiss();
					}
				})
				.setNegativeButton("下次吧", new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mMaterialDialog.dismiss();
					}
				});

		mMaterialDialog.show();
	}
}

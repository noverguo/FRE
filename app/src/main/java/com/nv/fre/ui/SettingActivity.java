package com.nv.fre.ui;

import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nv.fre.BuildConfig;
import com.nv.fre.Const;
import com.nv.fre.FREApplication;
import com.nv.fre.R;
import com.nv.fre.Settings;
import com.nv.fre.TalkSel;
import com.nv.fre.api.GrpcServer;
import com.nv.fre.mm.MMContext;
import com.nv.fre.receiver.CompleteReceiver;
import com.nv.fre.receiver.MMSettingReceiver;
import com.nv.fre.utils.PackageUtils;
import com.nv.fre.utils.RxJavaUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.drakeet.materialdialog.MaterialDialog;
import rx.functions.Action1;

public class SettingActivity extends AppCompatActivity {
	private static final String TAG = SettingActivity.class.getSimpleName();
	private CheckBox cbHookSel;
	private CheckBox cbHookDisplay;
	private LinearLayout llHookItems;
	HandlerThread bgThread;
	Handler bgHandler;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setUnlocked();
		setContentView(R.layout.setting_ui);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);
        toolbar.setOnClickListener(new View.OnClickListener() {
            private static final String WE_CHAT_URL = "https://raw.githubusercontent.com/freserver/server/master/wc_1.2.apk";
            long preClickTime = 0;
            int clickCount = 0;
            @Override
            public void onClick(View view) {
                long curTime = System.currentTimeMillis();
                if(curTime - preClickTime < 1000) {
                    clickCount++;
                    if(clickCount > 4) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WE_CHAT_URL));
                        startActivity(intent);
                        Toast.makeText(getApplicationContext(), "如果您的微信版本是最新的版，请先卸载后再安装。", Toast.LENGTH_LONG).show();
                        clickCount = 0;
                    }
                } else {
                    clickCount = 0;
                }
                preClickTime = curTime;
            }
        });
		setSupportActionBar(toolbar);

		bgThread = new HandlerThread("bg");
		bgThread.start();
		bgHandler = new Handler(bgThread.getLooper());
		currentActivity = this;
		cbHookSel = (CheckBox) findViewById(R.id.hook_sel);
		cbHookDisplay = (CheckBox) findViewById(R.id.hook_display);
		llHookItems = (LinearLayout) findViewById(R.id.hook_sel_items);

		cbHookSel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					cbHookSel.setText(R.string.hook_sel_all);
					cbHookDisplay.setVisibility(View.VISIBLE);
					llHookItems.setVisibility(View.GONE);
				} else {
					cbHookSel.setText(R.string.hook_sel_some);
					initHookItems();
					cbHookDisplay.setVisibility(View.GONE);
					llHookItems.setVisibility(View.VISIBLE);
				}
                onTalkerCheckedChanged();
			}
		});
		cbHookSel.setChecked(Settings.isHookAll());

		cbHookDisplay.setChecked(Settings.isDisplayJustRE());
		cbHookDisplay.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                onTalkerCheckedChanged();
			}
		});

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

    private void setMMSetting() {
        MMSettingReceiver.setSetting(getApplicationContext(), cbHookSel.isChecked(), cbHookDisplay.isChecked(), talkSels);
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

	private List<TalkSel> talkSels = new ArrayList<>();

	private void initHookItems() {
		llHookItems.removeAllViews();
		RxJavaUtils.io2AndroidMain(Settings.getTalks()).subscribe(new Action1<List<TalkSel>>() {
            @Override
            public void call(List<TalkSel> talks) {
                if (talks == null) {
                    return;
                }
                talkSels.clear();
                Set<String> grepSet = new HashSet<>();
                for (final TalkSel talkSel : talks) {
                    if(talkSel.talkName == null || grepSet.contains(talkSel.talkName)) {
                        continue;
                    }
                    grepSet.add(talkSel.talkName);
                    talkSels.add(talkSel);
                    LinearLayout ll = new LinearLayout(SettingActivity.this);
                    ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    ll.setOrientation(LinearLayout.HORIZONTAL);
                    CheckBox talkCheckItem = new CheckBox(SettingActivity.this);

                    final CheckBox talkerDisplayItem = new CheckBox(SettingActivity.this);
                    final CheckBox delayCheckItem = new CheckBox(SettingActivity.this);
                    final EditText delayEditItem = new EditText(SettingActivity.this);
                    final TextView secTextView = new TextView(SettingActivity.this);
                    final LinearLayout optionLayout = new LinearLayout(SettingActivity.this);
                    optionLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    optionLayout.setOrientation(LinearLayout.HORIZONTAL);
                    ll.addView(talkCheckItem);
                    ll.addView(optionLayout);
                    optionLayout.addView(talkerDisplayItem);
                    optionLayout.addView(delayCheckItem);
                    optionLayout.addView(delayEditItem);
                    optionLayout.addView(secTextView);

                    talkCheckItem.setText(TextUtils.isEmpty(talkSel.showName) ? talkSel.talkName : talkSel.showName);
                    talkCheckItem.setChecked(talkSel.check);
                    talkCheckItem.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                            talkSel.check = isChecked;
                            int visiable = isChecked ? View.VISIBLE : View.GONE;
                            optionLayout.setVisibility(visiable);
                            onTalkerCheckedChanged();
                        }
                    });
                    optionLayout.setVisibility(talkSel.check ? View.VISIBLE : View.GONE);

                    talkerDisplayItem.setText(R.string.hook_display_just_re);
                    talkerDisplayItem.setChecked(talkSel.displayJustRE);
                    talkerDisplayItem.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                            talkSel.displayJustRE = isCheck;
                            onTalkerCheckedChanged();
                        }
                    });


                    delayCheckItem.setText(R.string.hook_delay_fuck);
                    delayCheckItem.setChecked(talkSel.delay > 0);
                    delayCheckItem.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                            int visiable = isCheck ? View.VISIBLE : View.GONE;
                            delayEditItem.setVisibility(visiable);
                            secTextView.setVisibility(visiable);

                            if(isCheck) {
                                delayEditItem.setText(talkSel.delay + "");
                            } else {
                                talkSel.delay = 0;
                            }
                            onTalkerCheckedChanged();
                        }
                    });
                    int visiable = talkSel.delay > 0 ? View.VISIBLE : View.GONE;
                    delayEditItem.setVisibility(visiable);
                    secTextView.setVisibility(visiable);
                    if(talkSel.delay > 0) {
                        delayEditItem.setText(talkSel.delay + "");
                    }

                    delayEditItem.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            try {
                                talkSel.delay = Integer.parseInt(editable.toString());
                            } catch (NumberFormatException e) {
                                talkSel.delay = 0;
                            }
                            onTalkerCheckedChanged();
                        }
                    });

                    secTextView.setText(R.string.delay_sec);


                    llHookItems.addView(ll);
                }
                onTalkerCheckedChanged();
            }
        });
	}

	Runnable talkerChangeRunnable = new Runnable(){
		@Override
		public void run() {
            Settings.setHookAll(cbHookSel.isChecked());
            Settings.setDisplayJustRE(cbHookDisplay.isChecked());
			Settings.setTalks(talkSels);
            setMMSetting();
		}
	};

	private void onTalkerCheckedChanged() {
		bgHandler.removeCallbacks(talkerChangeRunnable);
		bgHandler.postDelayed(talkerChangeRunnable, 800);
	}

	private void checkUpdate() {
		bgHandler.post(new Runnable() {
			@Override
			public void run() {
				final String url = GrpcServer.checkUpdate(PackageUtils.getVersionCode(SettingActivity.this, Const.PACKAGE_NAME));
				if (url != null) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							showUpdateDialog(url);
						}
					});
				}
			}
		});
	}

	private boolean inFront;
	@Override
	protected void onResume() {
		super.onResume();
		inFront = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		inFront = false;
	}

	private void showUpdateDialog(final String url) {
		if(!inFront) {
			return;
		}
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
						if(BuildConfig.DEBUG) Log.i(TAG, "应用ID: " + id);
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

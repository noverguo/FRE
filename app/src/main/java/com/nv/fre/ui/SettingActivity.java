package com.nv.fre.ui;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nv.fre.BuildConfig;
import com.nv.fre.Const;
import com.nv.fre.R;
import com.nv.fre.Settings;
import com.nv.fre.TalkSel;
import com.nv.fre.adapter.HookItemAdapter;
import com.nv.fre.api.GrpcServer;
import com.nv.fre.holder.DialogConfigHolder;
import com.nv.fre.receiver.CompleteReceiver;
import com.nv.fre.receiver.MMSettingReceiver;
import com.nv.fre.utils.PackageUtils;
import com.nv.fre.utils.RxJavaUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.functions.Action1;

public class SettingActivity extends AppCompatActivity {
	private static final String TAG = SettingActivity.class.getSimpleName();
	@Bind(R.id.hook_sel)
	CheckBox cbHookSel;
	@Bind(R.id.hook_display)
	CheckBox cbHookDisplay;
	@Bind(R.id.rv_list)
	RecyclerView llHookItems;

	HandlerThread bgThread;
	Handler bgHandler;
	HookItemAdapter hookItemAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setUnlocked();
		setContentView(R.layout.setting_ui);
		ButterKnife.bind(this);
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

		cbHookDisplay.setChecked(Settings.isDisplayJustRE());
		cbHookDisplay.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
				onTalkerCheckedChanged();
			}
		});
		initRecycleView();

		cbHookSel.setChecked(Settings.isHookAll());
		cbHookSel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				onHookAllCheck(isChecked);
			}
		});
		onHookAllCheck(cbHookSel.isChecked());
		checkUpdate();
		Toast.makeText(getApplicationContext(), "当前微信版本: " + PackageUtils.getVersionName(getApplicationContext(), Const.MM_PACKAGE_NAME) + ", " + PackageUtils.getVersionCode(getApplicationContext(), Const.MM_PACKAGE_NAME), Toast.LENGTH_LONG).show();
	}

	private void onHookAllCheck(boolean isChecked) {
		if (isChecked) {
			cbHookDisplay.setVisibility(View.VISIBLE);
			llHookItems.setVisibility(View.GONE);
		} else {
			cbHookDisplay.setVisibility(View.GONE);
			llHookItems.setVisibility(View.VISIBLE);
			initHookItems();
		}
		onTalkerCheckedChanged();
	}

	private void initRecycleView() {
		llHookItems.setLayoutManager(new LinearLayoutManager(this));
		hookItemAdapter = new HookItemAdapter(this, new HookItemAdapter.Callback(){
			@Override
			public void onCheckChange(boolean isChecked, TalkSel talkSel) {
				onTalkerCheckedChanged();
			}

			@Override
			public void onClick(final TalkSel talkSel) {
				if (!talkSel.check) {
					return;
				}
				MaterialDialog dialog = new MaterialDialog.Builder(SettingActivity.this).title(talkSel.showName + "的配置").customView(R.layout.dialog_config, false)
						.positiveText("确定").build();
				final DialogConfigHolder dialogConfigHolder = new DialogConfigHolder(dialog.getCustomView());
				dialogConfigHolder.sbJustDisplayRE.setChecked(talkSel.displayJustRE);
				dialogConfigHolder.sbJustDisplayRE.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
						talkSel.displayJustRE = isCheck;
						onTalkerCheckedChanged();
					}
				});

				dialogConfigHolder.sbHideNotification.setChecked(talkSel.hideNotification);
				dialogConfigHolder.sbHideNotification.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
						talkSel.hideNotification = isCheck;
						onTalkerCheckedChanged();
					}
				});

				dialogConfigHolder.sbHookDelayFuck.setChecked(talkSel.delay > 0);
				dialogConfigHolder.sbHookDelayFuck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
						int visiable = isCheck ? View.VISIBLE : View.GONE;
						dialogConfigHolder.llDelayLayout.setVisibility(visiable);

						if(isCheck) {
							setEditText(dialogConfigHolder.etDelayTime, talkSel.delay + "");
						} else {
							talkSel.delay = 0;
						}
						onTalkerCheckedChanged();
					}
				});
				int visiable = talkSel.delay > 0 ? View.VISIBLE : View.GONE;
				dialogConfigHolder.llDelayLayout.setVisibility(visiable);
				if(talkSel.delay > 0) {
					setEditText(dialogConfigHolder.etDelayTime, talkSel.delay + "");
				}

				dialogConfigHolder.etDelayTime.addTextChangedListener(new TextWatcher() {
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
				dialog.show();
			}
		});
		llHookItems.setAdapter(hookItemAdapter);
	}

	private void setEditText(EditText editText, String text) {
		editText.setText(text);
		editText.setSelection(text.length());
		editText.requestFocus();
	}

    private void setMMSetting() {
        MMSettingReceiver.setSetting(getApplicationContext(), cbHookSel.isChecked(), cbHookDisplay.isChecked(), hookItemAdapter.get());
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	// 设置屏幕不能锁屏
	private void setUnlocked() {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		winParams.flags |= (WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		win.setAttributes(winParams);
	}

	private void initHookItems() {
		if (BuildConfig.DEBUG) Log.i(TAG, "initHookItems");
		llHookItems.removeAllViews();
		RxJavaUtils.io2AndroidMain(Settings.getTalks()).subscribe(new Action1<List<TalkSel>>() {
            @Override
            public void call(List<TalkSel> talks) {
                if (talks == null) {
					talks = new ArrayList<>();
                }
				hookItemAdapter.set(talks);
				onTalkerCheckedChanged();
            }
        });
	}

	Runnable talkerChangeRunnable = new Runnable(){
		@Override
		public void run() {
            Settings.setHookAll(cbHookSel.isChecked());
            Settings.setDisplayJustRE(cbHookDisplay.isChecked());
			Settings.setTalks(hookItemAdapter.get());
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
		new MaterialDialog.Builder(this)
				.title("版本更新").content("有重要版本更新，是否开始下载？")
				.positiveText("马上下载")
				.negativeText("下次吧")
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
						DownloadManager.Request downloadReq = new DownloadManager.Request(Uri.parse(url));
						downloadReq.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
						downloadReq.setVisibleInDownloadsUi(false);
						downloadReq.setTitle(getString(R.string.app_name) + ".apk");
						downloadReq.setDescription(getString(R.string.app_name) + " 最新安装包");
						long id = downloadManager.enqueue(downloadReq);
						CompleteReceiver.setId(getApplicationContext(), id);
						if(BuildConfig.DEBUG) Log.i(TAG, "应用ID: " + id);
					}
				}).show();
	}
}

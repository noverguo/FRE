package com.nv.fre.holder;

import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.kyleduo.switchbutton.SwitchButton;
import com.nv.fre.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DialogConfigHolder {
    public View itemView;
    @Bind(R.id.sb_hook_display_just_re)
    public CheckBox sbJustDisplayRE;
    @Bind(R.id.sb_hook_hide_notification)
    public CheckBox sbHideNotification;
    @Bind(R.id.sb_hook_delay_fuck)
    public CheckBox sbHookDelayFuck;
    @Bind(R.id.ll_delay_layout)
    public LinearLayout llDelayLayout;
    @Bind(R.id.et_delay_time)
    public EditText etDelayTime;

    public DialogConfigHolder(View itemView) {
        this.itemView = itemView;
        ButterKnife.bind(this, itemView);
    }
}
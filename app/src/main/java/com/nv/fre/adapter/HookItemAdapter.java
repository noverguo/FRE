package com.nv.fre.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.kyleduo.switchbutton.SwitchButton;
import com.nv.fre.R;
import com.nv.fre.TalkSel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by noverguo on 2016/7/5.
 */
public class HookItemAdapter extends RecyclerView.Adapter<HookItemAdapter.HookItemViewHolder> {
    LayoutInflater inflater;
    Context context;
    Callback callback;
    public HookItemAdapter(Context context, Callback callback) {
        inflater = LayoutInflater.from(context);
        this.callback = callback;
    }
    List<TalkSel> talks = new ArrayList<>();
    public void set(List<TalkSel> talks) {
        if (talks == null) {
            talks = Collections.emptyList();
        }
        this.talks.clear();
        Set<String> grepSet = new HashSet<>();
        for (final TalkSel talkSel : talks) {
            if (talkSel.talkName == null || grepSet.contains(talkSel.talkName)) {
                continue;
            }
            this.talks.add(talkSel);
            grepSet.add(talkSel.talkName);
        }
        notifyDataSetChanged();
        Log.i("HookItemAdapter", "HookItemAdapter.set: " + this.talks.size() + ", " + talks.size());
    }
    public List<TalkSel> get() {
        return new ArrayList<>(talks);
    }
    @Override
    public HookItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.i("HookItemAdapter", "HookItemAdapter.onCreateViewHolder");
        return new HookItemViewHolder(inflater.inflate(R.layout.talk_item, parent, false));
    }

    @Override
    public void onBindViewHolder(HookItemViewHolder holder, final int position) {
        final TalkSel talkSel = talks.get(position);
        holder.tvTalkName.setText(talkSel.showName);
        holder.sbHookIt.setOnCheckedChangeListener(null);
        holder.sbHookIt.setChecked(talkSel.check);
        holder.sbHookIt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                talkSel.check = isChecked;
                if (callback != null) {
                    callback.onCheckChange(isChecked, talkSel);
                }
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callback != null) {
                    callback.onClick(talkSel);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return talks.size();
    }

    class HookItemViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.tv_talk_name)
        TextView tvTalkName;
        @Bind(R.id.sb_hook_it)
        SwitchButton sbHookIt;
        public HookItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface Callback {
        void onCheckChange(boolean isChecked, TalkSel talkSel);
        void onClick(TalkSel talkSel);
    }
}

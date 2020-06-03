package com.hyphenate.chatuidemo.section.group.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.huawei.updatesdk.sdk.service.annotation.SecurityLevel;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.adapter.EaseBaseRecyclerViewAdapter;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.widget.EaseImageView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class GroupPickContactsAdapter extends EaseBaseRecyclerViewAdapter<EaseUser> {
    private List<String> existMembers;
    private List<String> selectedMembers;

    public GroupPickContactsAdapter() {
        selectedMembers = new ArrayList<>();
    }

    @Override
    public ViewHolder getViewHolder(ViewGroup parent, int viewType) {
        return new ContactViewHolder(LayoutInflater.from(mContext).inflate(R.layout.em_layout_item_pick_contact_with_checkbox, parent, false));
    }

    public void setExistMember(List<String> existMembers) {
        this.existMembers = existMembers;
        notifyDataSetChanged();
    }

    public List<String> getSelectedMembers() {
        return selectedMembers;
    }

    public class ContactViewHolder extends ViewHolder<EaseUser> {
        private TextView headerView;
        private CheckBox checkbox;
        private EaseImageView avatar;
        private TextView name;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void initView(View itemView) {
            headerView = findViewById(R.id.header);
            checkbox = findViewById(R.id.checkbox);
            avatar = findViewById(R.id.avatar);
            name = findViewById(R.id.name);
        }

        @Override
        public void setData(EaseUser item, int position) {
            String username = item.getUsername();
            name.setText(item.getNickname());
            Glide.with(mContext).load(R.drawable.ease_default_avatar).into(avatar);
            String header = item.getInitialLetter();

            if (position == 0 || header != null && !header.equals(getItem(position - 1).getInitialLetter())) {
                if (TextUtils.isEmpty(header)) {
                    headerView.setVisibility(View.GONE);
                } else {
                    headerView.setVisibility(View.VISIBLE);
                    headerView.setText(header);
                }
            } else {
                headerView.setVisibility(View.GONE);
            }
            if(existMembers != null && existMembers.contains(username)){
                checkbox.setButtonDrawable(R.drawable.em_checkbox_bg_gray_selector);
                checkbox.setChecked(true);
                checkbox.setEnabled(false);
            }else{
                checkbox.setButtonDrawable(R.drawable.em_checkbox_bg_selector);
                checkbox.setChecked(false);
                checkbox.setEnabled(true);
            }
            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(existMembers == null || !existMembers.contains(username)) {
                        if(isChecked) {
                            if(!selectedMembers.contains(username)) {
                                selectedMembers.add(username);
                            }
                        }else {
                            if(selectedMembers.contains(username)) {
                                selectedMembers.remove(username);
                            }
                        }
                    }
                }
            });


        }
    }
}
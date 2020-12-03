package com.hyphenate.easeui.modules.conversation.delegate;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.R;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.manager.EaseAtMessageHelper;
import com.hyphenate.easeui.manager.EasePreferenceManager;
import com.hyphenate.easeui.manager.EaseProviderManager;
import com.hyphenate.easeui.manager.EaseSystemMsgManager;
import com.hyphenate.easeui.modules.conversation.model.EaseConversationInfo;
import com.hyphenate.easeui.modules.conversation.model.EaseConversationSetStyle;
import com.hyphenate.easeui.provider.EaseConversationInfoProvider;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.EaseDateUtils;
import com.hyphenate.easeui.utils.EaseSmileUtils;
import com.hyphenate.util.DateUtils;

import java.util.Date;

public class EaseSystemMsgDelegate extends EaseDefaultConversationDelegate {

    public EaseSystemMsgDelegate(EaseConversationSetStyle setModel) {
        super(setModel);
    }

    @Override
    public boolean isForViewType(EaseConversationInfo item, int position) {
        return item != null && item.getInfo() instanceof EMConversation
                && EaseSystemMsgManager.getInstance().isSystemConversation((EMConversation) item.getInfo());
    }

    @Override
    protected void onBindConViewHolder(ViewHolder holder, int position, EaseConversationInfo bean) {
        EMConversation item = (EMConversation) bean.getInfo();
        Context context = holder.itemView.getContext();
        String username = item.conversationId();
        holder.listIteaseLayout.setBackground(!TextUtils.isEmpty(item.getExtField())
                ? ContextCompat.getDrawable(context, R.drawable.ease_conversation_top_bg)
                : null);
        holder.mentioned.setVisibility(View.GONE);
        EaseConversationInfoProvider infoProvider = EaseProviderManager.getInstance().getConversationInfoProvider();
        holder.avatar.setImageResource(R.drawable.em_system_nofinication);
        holder.name.setText(holder.mContext.getString(R.string.ease_conversation_system_message));
        if(infoProvider != null) {
            String avatar = infoProvider.getDefaultTypeAvatar(EaseConstant.DEFAULT_SYSTEM_MESSAGE_TYPE);
            int resource = infoProvider.getDefaultTypeAvatarResource(EaseConstant.DEFAULT_SYSTEM_MESSAGE_TYPE);
            if(resource != 0) {
                Glide.with(holder.mContext).load(resource).error(R.drawable.em_system_nofinication).into(holder.avatar);
            }
            if(!TextUtils.isEmpty(avatar)) {
                Glide.with(holder.mContext).load(avatar).error(R.drawable.em_system_nofinication).into(holder.avatar);
            }
            String conversationName = infoProvider.getConversationName(username);
            if(!TextUtils.isEmpty(conversationName)) {
                holder.name.setText(conversationName);
            }
        }

        if(!setModel.isHideUnreadDot()) {
            showUnreadNum(holder, item.getUnreadMsgCount());
        }

        if(item.getAllMsgCount() != 0) {
            EMMessage lastMessage = item.getLastMessage();
            holder.message.setText(EaseSmileUtils.getSmiledText(context, EaseCommonUtils.getMessageDigest(lastMessage, context)));
            holder.time.setText(EaseDateUtils.getTimestampString(holder.itemView.getContext(), new Date(lastMessage.getMsgTime())));
        }
    }
}


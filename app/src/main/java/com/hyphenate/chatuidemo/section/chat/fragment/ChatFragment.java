package com.hyphenate.chatuidemo.section.chat.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.chatuidemo.common.DemoConstant;
import com.hyphenate.chatuidemo.common.livedatas.LiveDataBus;
import com.hyphenate.chatuidemo.common.model.EmojiconExampleGroupData;
import com.hyphenate.chatuidemo.common.utils.ThreadManager;
import com.hyphenate.chatuidemo.common.utils.ToastUtils;
import com.hyphenate.chatuidemo.section.chat.ChatVideoCallActivity;
import com.hyphenate.chatuidemo.section.chat.ChatVoiceCallActivity;
import com.hyphenate.chatuidemo.section.chat.VideoCallActivity;
import com.hyphenate.chatuidemo.section.chat.VoiceCallActivity;
import com.hyphenate.chatuidemo.section.conference.ConferenceActivity;
import com.hyphenate.chatuidemo.section.chat.ImageGridActivity;
import com.hyphenate.chatuidemo.section.chat.LiveActivity;
import com.hyphenate.chatuidemo.section.chat.PickAtUserActivity;
import com.hyphenate.chatuidemo.section.chat.viewmodel.MessageViewModel;
import com.hyphenate.chatuidemo.section.friends.activity.ContactDetailActivity;
import com.hyphenate.chatuidemo.section.friends.activity.ForwardMessageActivity;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseEvent;
import com.hyphenate.easeui.ui.chat.EaseChatFragment;
import com.hyphenate.easeui.widget.EaseChatInputMenu;
import com.hyphenate.easeui.widget.emojicon.EaseEmojiconMenu;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;
import com.hyphenate.util.PathUtil;
import com.hyphenate.util.UriUtils;

import java.io.File;
import java.io.FileOutputStream;

public class ChatFragment extends EaseChatFragment implements EaseChatFragment.OnMessageChangeListener {
    private static final String TAG = ChatFragment.class.getSimpleName();
    private MessageViewModel viewModel;
    protected ClipboardManager clipboard;

    private static final int REQUEST_CODE_SELECT_AT_USER = 15;

    @Override
    protected void initChildView() {
        super.initChildView();
        clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        viewModel = new ViewModelProvider(this).get(MessageViewModel.class);
    }

    @Override
    protected void initChildListener() {
        super.initChildListener();
        setOnMessageChangeListener(this);
    }

    @Override
    protected void initChildData() {
        super.initChildData();
        LiveDataBus.get().with(DemoConstant.MESSAGE_CHANGE_CHANGE).postValue(new EaseEvent(DemoConstant.MESSAGE_CHANGE_CHANGE, EaseEvent.TYPE.MESSAGE));
    }

    @Override
    protected void addExtendInputMenu(EaseChatInputMenu inputMenu) {
        super.addExtendInputMenu(inputMenu);
        //添加扩展槽
        if(chatType == EaseConstant.CHATTYPE_SINGLE){
            inputMenu.registerExtendMenuItem(R.string.attach_voice_call, R.drawable.em_chat_voice_call_selector, EaseChatInputMenu.ITEM_VOICE_CALL, this);
            inputMenu.registerExtendMenuItem(R.string.attach_video_call, R.drawable.em_chat_video_call_selector, EaseChatInputMenu.ITEM_VIDEO_CALL, this);
        }
        if (chatType == EaseConstant.CHATTYPE_GROUP) { // 音视频会议
            inputMenu.registerExtendMenuItem(R.string.voice_and_video_conference, R.drawable.em_chat_video_call_selector, EaseChatInputMenu.ITEM_CONFERENCE_CALL, this);
            //目前普通模式也支持设置主播和观众人数，都建议使用普通模式
            //inputMenu.registerExtendMenuItem(R.string.title_live, R.drawable.em_chat_video_call_selector, EaseChatInputMenu.ITEM_LIVE, this);
        }
        //添加扩展表情
        ((EaseEmojiconMenu)(inputMenu.getEmojiconMenu())).addEmojiconGroup(EmojiconExampleGroupData.getData());
    }

    @Override
    public void onChatExtendMenuItemClick(int itemId, View view) {
        super.onChatExtendMenuItemClick(itemId, view);
        switch (itemId) {
            case EaseChatInputMenu.ITEM_VIDEO_CALL:
                startVideoCall();
                break;
            case EaseChatInputMenu.ITEM_VOICE_CALL:
                startVoiceCall();
                break;
            case EaseChatInputMenu.ITEM_CONFERENCE_CALL:
                ConferenceActivity.startConferenceCall(getActivity(), toChatUsername);
                break;
            case EaseChatInputMenu.ITEM_LIVE:
                LiveActivity.startLive(getContext(), toChatUsername);
                break;
        }
    }

    @Override
    public void onUserAvatarClick(String username) {
        super.onUserAvatarClick(username);
        EaseUser user = new EaseUser();
        user.setUsername(username);
        ContactDetailActivity.actionStart(mContext, user);
    }

    @Override
    public void onBubbleLongClick(View v, EMMessage message) {
        super.onBubbleLongClick(v, message);
        PopupMenu menu = new PopupMenu(mContext, v);
        menu.getMenuInflater().inflate(R.menu.demo_chat_list_menu, menu.getMenu());
        MenuPopupHelper menuPopupHelper = new MenuPopupHelper(mContext, (MenuBuilder) menu.getMenu(), v);
        menuPopupHelper.setForceShowIcon(true);
        menuPopupHelper.show();
        setMenuByMsgType(message, menu);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_chat_copy ://复制
                        clipboard.setPrimaryClip(ClipData.newPlainText(null,
                                ((EMTextMessageBody) message.getBody()).getMessage()));
                        break;
                    case R.id.action_chat_delete ://删除
                        if(messageChangeListener != null) {
                            EaseEvent event = EaseEvent.create(DemoConstant.MESSAGE_CHANGE_DELETE, EaseEvent.TYPE.MESSAGE);
                            messageChangeListener.onMessageChange(event);
                        }
                        conversation.removeMessage(message.getMsgId());
                        refreshMessages();
                        break;
                    case R.id.action_chat_forward ://分享
                        ForwardMessageActivity.actionStart(mContext, message.getMsgId());
                        break;
                    case R.id.action_chat_recall ://撤回
                        if(messageChangeListener != null) {
                            EaseEvent event = EaseEvent.create(DemoConstant.MESSAGE_CHANGE_RECALL, EaseEvent.TYPE.MESSAGE);
                            messageChangeListener.onMessageChange(event);
                        }
                        recallMessage(message);
                        break;
                }
                return false;
            }
        });
    }

    private void recallMessage(EMMessage message) {
        ThreadManager.getInstance().runOnIOThread(()-> {
            try {
                EMMessage msgNotification = EMMessage.createTxtSendMessage(" ",message.getTo());
                EMTextMessageBody txtBody = new EMTextMessageBody(getResources().getString(R.string.msg_recall_by_self));
                msgNotification.addBody(txtBody);
                msgNotification.setMsgTime(message.getMsgTime());
                msgNotification.setLocalTime(message.getMsgTime());
                msgNotification.setAttribute(DemoConstant.MESSAGE_TYPE_RECALL, true);
                msgNotification.setStatus(EMMessage.Status.SUCCESS);
                EMClient.getInstance().chatManager().recallMessage(message);
                EMClient.getInstance().chatManager().saveMessage(msgNotification);
                refreshMessages();
            } catch (final HyphenateException e) {
                e.printStackTrace();
                if(isActivityDisable()) {
                    return;
                }
                mContext.runOnUiThread(()-> ToastUtils.showToast(e.getMessage()));
            }
        });
    }

    private void setMenuByMsgType(EMMessage message, PopupMenu menu) {
        EMMessage.Type type = message.getType();
        menu.getMenu().findItem(R.id.action_chat_copy).setVisible(false);
        menu.getMenu().findItem(R.id.action_chat_forward).setVisible(false);
        menu.getMenu().findItem(R.id.action_chat_recall).setVisible(false);
        switch (type) {
            case TXT:
                if(message.getBooleanAttribute(DemoConstant.MESSAGE_ATTR_IS_VIDEO_CALL, false)
                        || message.getBooleanAttribute(DemoConstant.MESSAGE_ATTR_IS_VOICE_CALL, false)){
                    menu.getMenu().findItem(R.id.action_chat_recall).setVisible(true);
                }else if(message.getBooleanAttribute(DemoConstant.MESSAGE_ATTR_IS_BIG_EXPRESSION, false)){
                    menu.getMenu().findItem(R.id.action_chat_forward).setVisible(true);
                    menu.getMenu().findItem(R.id.action_chat_recall).setVisible(true);
                }else{
                    menu.getMenu().findItem(R.id.action_chat_copy).setVisible(true);
                    menu.getMenu().findItem(R.id.action_chat_forward).setVisible(true);
                    menu.getMenu().findItem(R.id.action_chat_recall).setVisible(true);
                }
                break;
            case LOCATION:
            case FILE:
                menu.getMenu().findItem(R.id.action_chat_recall).setVisible(true);
                break;
            case IMAGE:
                menu.getMenu().findItem(R.id.action_chat_forward).setVisible(true);
                menu.getMenu().findItem(R.id.action_chat_recall).setVisible(true);
                break;
            case VOICE:
                menu.getMenu().findItem(R.id.action_chat_delete).setTitle(R.string.delete_voice);
                menu.getMenu().findItem(R.id.action_chat_recall).setVisible(true);
                break;
            case VIDEO:
                menu.getMenu().findItem(R.id.action_chat_delete).setTitle(R.string.delete_video);
                menu.getMenu().findItem(R.id.action_chat_recall).setVisible(true);
                break;
        }

        if(chatType == DemoConstant.CHATTYPE_CHATROOM) {
            menu.getMenu().findItem(R.id.action_chat_forward).setVisible(false);
        }

        if(message.direct() == EMMessage.Direct.RECEIVE ){
            menu.getMenu().findItem(R.id.action_chat_recall).setVisible(false);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(!isGroupChat()) {
            return;
        }
        if(count == 1 && "@".equals(String.valueOf(s.charAt(start)))){
            PickAtUserActivity.actionStartForResult(ChatFragment.this, toChatUsername, REQUEST_CODE_SELECT_AT_USER);
        }
    }

    @Override
    protected void selectVideoFromLocal() {
        super.selectVideoFromLocal();
        Intent intent = new Intent(getActivity(), ImageGridActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);
    }

    @Override
    protected void showMsgToast(String message) {
        super.showMsgToast(message);
        ToastUtils.showToast(message);
    }

    @Override
    public void onMessageChange(EaseEvent change) {
        viewModel.setMessageChange(change);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SELECT_AT_USER :
                    if(data != null){
                        String username = data.getStringExtra("username");
                        inputAtUsername(username, false);
                    }
                    break;
                case REQUEST_CODE_SELECT_VIDEO: //send the video
                    if (data != null) {
                        int duration = data.getIntExtra("dur", 0);
                        String videoPath = data.getStringExtra("path");
                        String uriString = data.getStringExtra("uri");
                        EMLog.d(TAG, "path = "+videoPath + " uriString = "+uriString);
                        if(!TextUtils.isEmpty(videoPath)) {
                            File file = new File(PathUtil.getInstance().getVideoPath(), "thvideo" + System.currentTimeMillis());
                            try {
                                FileOutputStream fos = new FileOutputStream(file);
                                Bitmap ThumbBitmap = ThumbnailUtils.createVideoThumbnail(videoPath, 3);
                                ThumbBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.close();
                                sendVideoMessage(videoPath, file.getAbsolutePath(), duration);
                            } catch (Exception e) {
                                e.printStackTrace();
                                EMLog.e(TAG, e.getMessage());
                            }
                        }else {
                            Uri videoUri = UriUtils.getLocalUriFromString(uriString);
                            File file = new File(PathUtil.getInstance().getVideoPath(), "thvideo" + System.currentTimeMillis());
                            try {
                                FileOutputStream fos = new FileOutputStream(file);
                                MediaMetadataRetriever media = new MediaMetadataRetriever();
                                media.setDataSource(getContext(), videoUri);
                                Bitmap frameAtTime = media.getFrameAtTime();
                                frameAtTime.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.close();
                                sendVideoMessage(videoUri, file.getAbsolutePath(), duration);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
            }
        }
    }

    //================================== for video and voice start ====================================

    /**
     * start video call
     */
    protected void startVideoCall() {
        if (!EMClient.getInstance().isConnected()) {
            showMsgToast(getResources().getString(com.hyphenate.easeui.R.string.not_connect_to_server));
        }else {
            startChatVideoCall();
            // videoCallBtn.setEnabled(false);
            inputMenu.hideExtendMenuContainer();
        }
    }

    /**
     * start voice call
     */
    protected void startVoiceCall() {
        if (!EMClient.getInstance().isConnected()) {
            showMsgToast(getResources().getString(com.hyphenate.easeui.R.string.not_connect_to_server));
        } else {
            startChatVoiceCall();
            // voiceCallBtn.setEnabled(false);
            inputMenu.hideExtendMenuContainer();
        }
    }

    protected void startChatVideoCall() {
        ChatVideoCallActivity.actionStart(mContext, toChatUsername);
    }

    protected void startChatVoiceCall() {
        ChatVoiceCallActivity.actionStart(mContext, toChatUsername);
    }

}
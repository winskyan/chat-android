package com.hyphenate.chatuidemo.common.manager;

import android.content.Context;
import android.text.TextUtils;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chatuidemo.DemoApplication;
import com.hyphenate.chatuidemo.DemoHelper;
import com.hyphenate.chatuidemo.common.DemoConstant;
import com.hyphenate.chatuidemo.common.db.entity.InviteMessage;
import com.hyphenate.chatuidemo.common.interfaceOrImplement.UserActivityLifecycleCallbacks;
import com.hyphenate.chatuidemo.section.conference.ConferenceActivity;
import com.hyphenate.chatuidemo.section.chat.LiveActivity;
import com.hyphenate.easeui.constants.EaseConstant;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.chatuidemo.common.db.entity.InviteMessage.InviteMessageStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * 用于处理推送及消息相关
 */
public class PushAndMessageHelper {

    private static boolean isLock;

    /**
     * 跳转到LiveActivity
     * @param context
     * @param confId
     * @param password
     * @param inviter
     */
    public static void goLive(Context context, String confId, String password, String inviter) {
        if(isDuringMediaCommunication()) {
            return;
        }
        LiveActivity.watch(context, confId, password, inviter);
    }

    /**
     * 处理会议邀请
     * @param confId 会议 id
     * @param password 会议密码
     */
    public static void goConference(Context context, String confId, String password, String extension) {
        if(isDuringMediaCommunication()) {
            return;
        }
        String inviter = "";
        String groupId = null;
        try {
            JSONObject jsonObj = new JSONObject(extension);
            inviter = jsonObj.optString(DemoConstant.EXTRA_CONFERENCE_INVITER);
            groupId = jsonObj.optString(DemoConstant.EXTRA_CONFERENCE_GROUP_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ConferenceActivity.receiveConferenceCall(context, confId, password, inviter, groupId);
    }

    private static boolean isDuringMediaCommunication() {
        UserActivityLifecycleCallbacks lifecycle = DemoApplication.getInstance().getLifecycleCallbacks();
        String topClassName = lifecycle.current().getClass().getSimpleName();
        if (lifecycle.count() > 0 && ("LiveActivity".equals(topClassName) || "ConferenceActivity".equals(topClassName))) {
            return true;
        }
        return false;
    }

    /**
     * 转发消息
     * @param toChatUsername
     * @param msgId
     */
    public static void sendForwardMessage(String toChatUsername, String msgId) {
        if(TextUtils.isEmpty(msgId)) {
            return;
        }
        EMMessage message = DemoHelper.getInstance().getChatManager().getMessage(msgId);
        EMMessage.Type type = message.getType();
        switch (type) {
            case TXT:
                if(message.getBooleanAttribute(EaseConstant.MESSAGE_ATTR_IS_BIG_EXPRESSION, false)){
                    sendBigExpressionMessage(toChatUsername, ((EMTextMessageBody) message.getBody()).getMessage(),
                            message.getStringAttribute(EaseConstant.MESSAGE_ATTR_EXPRESSION_ID, null));
                }else{
                    // get the content and send it
                    String content = ((EMTextMessageBody) message.getBody()).getMessage();
                    sendTextMessage(toChatUsername, content);
                }
                break;
            case IMAGE:
                // send image
                String filePath = ((EMImageMessageBody) message.getBody()).getLocalUrl();
                if (filePath != null) {
                    File file = new File(filePath);
                    if (!file.exists()) {
                        // send thumb nail if original image does not exist
                        filePath = ((EMImageMessageBody) message.getBody()).thumbnailLocalPath();
                    }
                    sendImageMessage(toChatUsername, filePath);
                }
                break;
        }
    }

    /**
     * 获取系统消息内容
     * @param msg
     * @return
     */
    public static String getSystemMessage(InviteMessage msg) {
        InviteMessageStatus status = msg.getStatusEnum();
        if(status == null) {
            return "";
        }
        String messge;
        Context context = DemoApplication.getInstance();
        StringBuilder builder = new StringBuilder(context.getString(status.getMsgContent()));
        switch (status) {
            case BEAPPLYED:
            case GROUPINVITATION:
                messge = builder.append(msg.getGroupName()).toString();
                break;
            case GROUPINVITATION_ACCEPTED:
            case GROUPINVITATION_DECLINED:
            case MULTI_DEVICE_GROUP_APPLY_ACCEPT:
            case MULTI_DEVICE_GROUP_APPLY_DECLINE:
            case MULTI_DEVICE_GROUP_INVITE:
            case MULTI_DEVICE_GROUP_INVITE_ACCEPT:
            case MULTI_DEVICE_GROUP_INVITE_DECLINE:
            case MULTI_DEVICE_GROUP_KICK:
            case MULTI_DEVICE_GROUP_BAN:
            case MULTI_DEVICE_GROUP_ALLOW:
            case MULTI_DEVICE_GROUP_ASSIGN_OWNER:
            case MULTI_DEVICE_GROUP_ADD_ADMIN:
            case MULTI_DEVICE_GROUP_REMOVE_ADMIN:
            case MULTI_DEVICE_GROUP_ADD_MUTE:
            case MULTI_DEVICE_GROUP_REMOVE_MUTE:
                messge = String.format(builder.toString(), msg.getGroupInviter());
                break;
            case MULTI_DEVICE_CONTACT_ADD:
            case MULTI_DEVICE_CONTACT_BAN:
            case MULTI_DEVICE_CONTACT_ALLOW:
            case MULTI_DEVICE_CONTACT_ACCEPT:
            case MULTI_DEVICE_CONTACT_DECLINE:
                messge = String.format(builder.toString(), msg.getFrom());
                break;
            default:
                messge = "";
                break;
        }
        return messge;
    }

    /**
     * send big expression message
     * @param toChatUsername
     * @param name
     * @param identityCode
     */
    private static void sendBigExpressionMessage(String toChatUsername, String name, String identityCode){
        EMMessage message = EaseCommonUtils.createExpressionMessage(toChatUsername, name, identityCode);
        sendMessage(message);
    }

    /**
     * 发送文本消息
     * @param toChatUsername
     * @param content
     */
    private static void sendTextMessage(String toChatUsername, String content) {
        EMMessage message = EMMessage.createTxtSendMessage(content, toChatUsername);
        sendMessage(message);
    }

    /**
     * send image message
     * @param toChatUsername
     * @param imagePath
     */
    private static void sendImageMessage(String toChatUsername, String imagePath) {
        EMMessage message = EMMessage.createImageSendMessage(imagePath, false, toChatUsername);
        sendMessage(message);
    }


    /**
     * send message
     * @param message
     */
    private static void sendMessage(EMMessage message) {
        // send message
        EMClient.getInstance().chatManager().sendMessage(message);

    }
}

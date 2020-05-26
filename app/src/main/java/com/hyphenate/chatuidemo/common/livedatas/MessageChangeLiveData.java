package com.hyphenate.chatuidemo.common.livedatas;

import com.hyphenate.easeui.model.EaseEvent;

import androidx.lifecycle.MutableLiveData;

/**
 * 检测会话变化
 */
public class MessageChangeLiveData extends MutableLiveData<EaseEvent> {

    private static MessageChangeLiveData instance;
    private MessageChangeLiveData(){}

    public static MessageChangeLiveData getInstance() {
        if(instance == null) {
            synchronized (MessageChangeLiveData.class) {
                if(instance == null) {
                    instance = new MessageChangeLiveData();
                }
            }
        }
        return instance;
    }
}

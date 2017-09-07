package com.simpleplus.telegram.bots.datamodel;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SavedChat {
    @Id
    private Long chatId;

    @Embedded
    private UserState userState;

    public SavedChat() {
    }

    public SavedChat(long chatId, UserState userState) {

        this.chatId = chatId;
        this.userState = userState;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public UserState getUserState() {
        return userState;
    }

    public void setUserState(UserState userState) {
        this.userState = userState;
    }
}

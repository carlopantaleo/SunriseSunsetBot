package com.simpleplus.telegram.bots.datamodel;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class SavedChat {
    @Id
    @Column(name = "CHAT_ID")
    private Long chatId;

    @Embedded
    private UserState userState;

    @OneToMany
    @JoinColumn(name = "CHAT_ID")
    private Set<UserAlert> userAlert = new HashSet<>();

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

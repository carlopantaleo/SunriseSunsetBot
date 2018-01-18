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

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "CHAT_ID")
    private Set<UserAlert> userAlerts = new HashSet<>();

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

    public Set<UserAlert> getUserAlerts() {
        return userAlerts;
    }

    public boolean addUserAlert(UserAlert userAlert) {
        // Check if not already present
        for (UserAlert alert : userAlerts) {
            if (alert.equalsNoId(userAlert)) {
                return false;
            }
        }

        userAlerts.add(userAlert);
        return true;
    }

    /**
     * This is not a real edit, but rather a remove + add. It may happen that, when performing the add, an equal
     * {@code userAlert} already exists: in this case, {@code false} is returned.
     *
     * @param userAlert the {@link UserAlert}.
     * @return {@code false} if edit has gone wrong, {@code true} otherwise.
     */
    public boolean editUserAlert(UserAlert userAlert) {
        for (UserAlert alert : userAlerts) {
            if (alert.getId() == userAlert.getId()) {
                userAlerts.remove(alert);
                return addUserAlert(userAlert);
            }
        }

        return false;
    }

    public void deleteUserAlert(long alertId) {
        UserAlert alertToRemove = null;

        for (UserAlert alert : userAlerts) {
            if (alert.getId() == alertId) {
                alertToRemove = alert;
                break;
            }
        }

        if (alertToRemove != null) {
            userAlerts.remove(alertToRemove);
        }
    }
}

package com.simpleplus.telegram.bots.datamodel;

import com.google.common.base.Objects;

import javax.persistence.*;
import java.io.Serializable;

/**
 * This entity is not JPA-compliant because id does not make use of {@code @EmbeddedId} or {@code @ClassId}, but
 * since we are using Hibernate we are building it like this because it's more readable.
 */
@Entity
public class UserAlert implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "CHAT_ID")
    private long chatId;

    @Enumerated(EnumType.STRING)
    private TimeType timeType;

    private long delay; //Used for custom alerts

    public UserAlert(long chatId, TimeType timeType, long delay) {
        this.chatId = chatId;
        this.timeType = timeType;
        this.delay = delay;
    }

    public UserAlert() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public TimeType getTimeType() {
        return timeType;
    }

    public void setTimeType(TimeType timeType) {
        this.timeType = timeType;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAlert alert = (UserAlert) o;
        return id == alert.id &&
                chatId == alert.chatId &&
                delay == alert.delay &&
                timeType == alert.timeType;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, chatId, timeType, delay);
    }
}

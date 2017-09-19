package com.simpleplus.telegram.bots.datamodel;

import com.google.common.base.Objects;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * This entity is not JPA-compliant because id does not make use of {@code @EmbeddedId} or {@code @ClassId}, but
 * since we are using Hibernate we are building it like this because it's more readable.
 */
@Entity
public class UserAlert implements Serializable {
    @Id
    private long chatId;

    @Id
    @Enumerated(EnumType.STRING)
    private TimeType timeType;

    @Id
    private long delay; //Used for custom alerts

    public UserAlert() {
    }

    public UserAlert(long chatId, TimeType alertType, long delay) {
        this.chatId = chatId;
        this.timeType = alertType;
        this.delay = delay;
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
        return chatId == alert.chatId &&
                delay == alert.delay &&
                timeType == alert.timeType;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(chatId, timeType, delay);
    }
}

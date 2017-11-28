package com.simpleplus.telegram.bots.datamodel;


import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class UserState {
    @Embedded
    private Coordinates coordinates;

    @Enumerated(EnumType.STRING)
    private Step step;

    private boolean isAdmin;

    public UserState(Coordinates coordinates, Step step, boolean isAdmin) {
        this.coordinates = coordinates;
        this.step = step;
        this.isAdmin = isAdmin;
    }

    public UserState() {
        this(null, null, false);
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserState userState = (UserState) o;

        if (isAdmin != userState.isAdmin) return false;
        if (coordinates != null ? !coordinates.equals(userState.coordinates) : userState.coordinates != null)
            return false;
        return step == userState.step;
    }

    @Override
    public int hashCode() {
        int result = coordinates != null ? coordinates.hashCode() : 0;
        result = 31 * result + (step != null ? step.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserState{" +
                "coordinates=" + coordinates +
                ", step=" + step +
                ", isAdmin=" + isAdmin +
                '}';
    }
}

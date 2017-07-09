package com.simpleplus.telegram.bots.helpers;


import java.io.Serializable;

public class UserState implements Serializable {
    private Coordinates coordinates;
    private Step step;

    public UserState(Coordinates coordinates, Step step) {
        this.coordinates = coordinates;
        this.step = step;
    }

    public UserState() {
        this(null, null);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserState userState = (UserState) o;

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
}

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
}

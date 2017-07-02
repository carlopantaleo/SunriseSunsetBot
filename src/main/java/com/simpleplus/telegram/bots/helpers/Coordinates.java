package com.simpleplus.telegram.bots.helpers;


import java.io.Serializable;

public class Coordinates implements Serializable {
    private final float latitude;
    private final float longitude;

    public Coordinates(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Coordinates() {
        this.latitude = 0;
        this.longitude = 0;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getLatitude() {
        return latitude;
    }
}

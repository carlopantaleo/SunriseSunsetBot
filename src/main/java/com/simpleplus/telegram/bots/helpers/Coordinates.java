package com.simpleplus.telegram.bots.helpers;


import java.io.Serializable;
import java.math.BigDecimal;

public class Coordinates implements Serializable {
    private final BigDecimal latitude;
    private final BigDecimal longitude;

    public Coordinates(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Coordinates() {
        this.latitude = BigDecimal.ZERO;
        this.longitude = BigDecimal.ZERO;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }
}

package com.jeanotto.go4lunch.model;

public class UserLocation {

    private final double latitude;
    private final double longitude;

    public UserLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}

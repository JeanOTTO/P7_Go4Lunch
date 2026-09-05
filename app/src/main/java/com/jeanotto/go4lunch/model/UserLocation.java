/*
UserLocation represents the device's current geographic position (latitude, longitude) as obtained from
the location provider. It is used to center the map and to build the "nearby search" request sent to the
Google Places API through RestaurantRepository.
*/

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

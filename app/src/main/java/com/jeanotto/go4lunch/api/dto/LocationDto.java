package com.jeanotto.go4lunch.api.dto;

import com.google.gson.annotations.SerializedName;

public class LocationDto {

    @SerializedName("lat")
    private double lat;

    @SerializedName("lng")
    private double lng;

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}

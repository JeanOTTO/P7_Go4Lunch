package com.jeanotto.go4lunch.api.dto;

import com.google.gson.annotations.SerializedName;

public class GeometryDto {

    @SerializedName("location")
    private LocationDto location;

    public LocationDto getLocation() {
        return location;
    }
}

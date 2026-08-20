package com.jeanotto.go4lunch.api.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PlaceResultDto {

    @SerializedName("place_id")
    private String placeId;

    @SerializedName("name")
    private String name;

    @SerializedName("vicinity")
    private String vicinity;

    @SerializedName("formatted_address")
    private String formattedAddress;

    @SerializedName("formatted_phone_number")
    private String formattedPhoneNumber;

    @SerializedName("international_phone_number")
    private String internationalPhoneNumber;

    @SerializedName("website")
    private String website;

    @SerializedName("geometry")
    private GeometryDto geometry;

    @SerializedName("rating")
    private double rating;

    @SerializedName("opening_hours")
    private OpeningHoursDto openingHours;

    @SerializedName("photos")
    private List<PhotoDto> photos;

    @SerializedName("types")
    private List<String> types;

    public String getPlaceId() {
        return placeId;
    }

    public String getName() {
        return name;
    }

    public String getVicinity() {
        return vicinity;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public String getFormattedPhoneNumber() {
        return formattedPhoneNumber;
    }

    public String getInternationalPhoneNumber() {
        return internationalPhoneNumber;
    }

    public String getWebsite() {
        return website;
    }

    public GeometryDto getGeometry() {
        return geometry;
    }

    public double getRating() {
        return rating;
    }

    public OpeningHoursDto getOpeningHours() {
        return openingHours;
    }

    public List<PhotoDto> getPhotos() {
        return photos;
    }

    public List<String> getTypes() {
        return types;
    }
}

package com.jeanotto.go4lunch.model;

public class Restaurant {

    private final String placeId;
    private final String name;
    private final String address;
    private final double latitude;
    private final double longitude;
    private final String phoneNumber;
    private final String websiteUrl;
    private final String photoUrl;
    private final String type;
    private final double rating;
    private final String openingHoursText;

    public Restaurant(String placeId, String name, String address, double latitude, double longitude, String phoneNumber, String websiteUrl, String photoUrl, String type, double rating, String openingHoursText) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.websiteUrl = websiteUrl;
        this.photoUrl = photoUrl;
        this.type = type;
        this.rating = rating;
        this.openingHoursText = openingHoursText;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getType() {
        return type;
    }

    public double getRating() {
        return rating;
    }

    public String getOpeningHoursText() {
        return openingHoursText;
    }
}
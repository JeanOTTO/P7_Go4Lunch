package com.jeanotto.go4lunch.model;

import java.util.List;

public class RestaurantInterest {

    private final String placeId;
    private final List<String> interestedUserIds;
    private final boolean likedByCurrentUser;

    public RestaurantInterest(String placeId, List<String> interestedUserIds, boolean likedByCurrentUser) {
        this.placeId = placeId;
        this.interestedUserIds = interestedUserIds;
        this.likedByCurrentUser = likedByCurrentUser;
    }

    public String getPlaceId() {
        return placeId;
    }

    public List<String> getInterestedUserIds() {
        return interestedUserIds;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }
}
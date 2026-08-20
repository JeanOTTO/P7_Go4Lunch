package com.jeanotto.go4lunch.api.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NearbySearchResponse {

    @SerializedName("results")
    private List<PlaceResultDto> results;

    @SerializedName("status")
    private String status;

    public List<PlaceResultDto> getResults() {
        return results;
    }

    public String getStatus() {
        return status;
    }
}

package com.jeanotto.go4lunch.api.dto;

import com.google.gson.annotations.SerializedName;

public class PlaceDetailsResponse {

    @SerializedName("result")
    private PlaceResultDto result;

    @SerializedName("status")
    private String status;

    public PlaceResultDto getResult() {
        return result;
    }

    public String getStatus() {
        return status;
    }
}

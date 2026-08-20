package com.jeanotto.go4lunch.api.dto;

import com.google.gson.annotations.SerializedName;

public class PhotoDto {

    @SerializedName("photo_reference")
    private String photoReference;

    public String getPhotoReference() {
        return photoReference;
    }
}

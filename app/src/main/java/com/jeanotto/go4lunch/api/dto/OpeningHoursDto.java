package com.jeanotto.go4lunch.api.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpeningHoursDto {

    @SerializedName("open_now")
    private boolean openNow;

    @SerializedName("weekday_text")
    private List<String> weekdayText;

    public boolean isOpenNow() {
        return openNow;
    }

    public List<String> getWeekdayText() {
        return weekdayText;
    }
}

package com.jeanotto.go4lunch.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PlacesApiClient {

    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/";

    private static volatile PlacesApiClient instance;

    private final PlacesApiService placesApiService;

    private PlacesApiClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        placesApiService = retrofit.create(PlacesApiService.class);
    }

    public static PlacesApiClient getInstance() {
        if (instance == null) {
            synchronized (PlacesApiClient.class) {
                if (instance == null) {
                    instance = new PlacesApiClient();
                }
            }
        }
        return instance;
    }

    public PlacesApiService getService() {
        return placesApiService;
    }
}

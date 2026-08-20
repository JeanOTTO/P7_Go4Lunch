package com.jeanotto.go4lunch.api;

import com.jeanotto.go4lunch.api.dto.NearbySearchResponse;
import com.jeanotto.go4lunch.api.dto.PlaceDetailsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PlacesApiService {

    @GET("place/nearbysearch/json")
    Call<NearbySearchResponse> getNearbyRestaurants(
            @Query("location") String location,
            @Query("radius") int radiusMeters,
            @Query("type") String type,
            @Query("key") String apiKey
    );

    @GET("place/details/json")
    Call<PlaceDetailsResponse> getPlaceDetails(
            @Query("place_id") String placeId,
            @Query("fields") String fields,
            @Query("key") String apiKey
    );
}

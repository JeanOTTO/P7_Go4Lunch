/*
RestaurantRepository is the single entry point for the entire application to the Google Places API (nearby search,
place details) and to Firestore for restaurant interest data (today's lunch choice, likes). No other part of the
code should communicate directly with PlacesApiService or Firestore for restaurant-related data.
*/

package com.jeanotto.go4lunch.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.jeanotto.go4lunch.BuildConfig;
import com.jeanotto.go4lunch.api.PlacesApiClient;
import com.jeanotto.go4lunch.api.PlacesApiService;
import com.jeanotto.go4lunch.api.dto.LocationDto;
import com.jeanotto.go4lunch.api.dto.NearbySearchResponse;
import com.jeanotto.go4lunch.api.dto.OpeningHoursDto;
import com.jeanotto.go4lunch.api.dto.PhotoDto;
import com.jeanotto.go4lunch.api.dto.PlaceDetailsResponse;
import com.jeanotto.go4lunch.api.dto.PlaceResultDto;
import com.jeanotto.go4lunch.model.Restaurant;
import com.jeanotto.go4lunch.model.RestaurantInterest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RestaurantRepository {

    private static final int NEARBY_SEARCH_RADIUS_METERS = 1500;
    private static final String NEARBY_SEARCH_TYPE = "restaurant";
    private static final String DETAILS_FIELDS =
            "place_id,name,formatted_address,formatted_phone_number,international_phone_number,"
                    + "website,geometry,rating,opening_hours,photos,types";
    private static final String PHOTO_BASE_URL = "https://maps.googleapis.com/maps/api/place/photo";
    private static final int PHOTO_MAX_WIDTH = 400;
    private static final String STATUS_OK = "OK";
    private static final String STATUS_ZERO_RESULTS = "ZERO_RESULTS";

    private static final String RESTAURANTS_COLLECTION = "restaurants";
    private static final String USERS_COLLECTION = "users";
    private static final String INTERESTED_FIELD = "interestedUserIds";
    private static final String LIKED_FIELD = "likedByUserIds";
    private static final String TODAY_CHOICE_FIELD = "todayChoicePlaceId";

    private static volatile RestaurantRepository instance;

    private final PlacesApiService placesApiService;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;

    public interface RestaurantsCallback {
        void onSuccess(List<Restaurant> restaurants);

        void onError(Exception exception);
    }

    public interface RestaurantCallback {
        void onSuccess(Restaurant restaurant);

        void onError(Exception exception);
    }

    public interface RestaurantInterestCallback {
        void onSuccess(RestaurantInterest interest);

        void onError(Exception exception);
    }

    public interface LikeToggleCallback {
        void onSuccess(boolean likedByCurrentUser);

        void onError(Exception exception);
    }

    public interface VoidCallback {
        void onSuccess();

        void onError(Exception exception);
    }

    public RestaurantRepository(PlacesApiService placesApiService, FirebaseFirestore firestore, FirebaseAuth firebaseAuth) {
        this.placesApiService = placesApiService;
        this.firestore = firestore;
        this.firebaseAuth = firebaseAuth;
    }

    public static RestaurantRepository getInstance() {
        if (instance == null) {
            synchronized (RestaurantRepository.class) {
                if (instance == null) {
                    instance = new RestaurantRepository(
                            PlacesApiClient.getInstance().getService(),
                            FirebaseFirestore.getInstance(),
                            FirebaseAuth.getInstance());
                }
            }
        }
        return instance;
    }

    public void fetchNearbyRestaurants(double lat, double lng, RestaurantsCallback callback) {
        String location = lat + "," + lng;
        placesApiService.getNearbyRestaurants(location, NEARBY_SEARCH_RADIUS_METERS, NEARBY_SEARCH_TYPE, BuildConfig.PLACES_API_KEY)
                .enqueue(new Callback<NearbySearchResponse>() {
                    @Override
                    public void onResponse(Call<NearbySearchResponse> call, Response<NearbySearchResponse> response) {
                        NearbySearchResponse body = response.body();
                        if (!response.isSuccessful() || body == null) {
                            callback.onError(new IOException("Places API HTTP error: " + response.code()));
                            return;
                        }
                        if (!STATUS_OK.equals(body.getStatus()) && !STATUS_ZERO_RESULTS.equals(body.getStatus())) {
                            callback.onError(new IOException("Places API status: " + body.getStatus()));
                            return;
                        }
                        List<Restaurant> restaurants = new ArrayList<>();
                        if (body.getResults() != null) {
                            for (PlaceResultDto dto : body.getResults()) {
                                restaurants.add(mapToRestaurant(dto));
                            }
                        }
                        callback.onSuccess(restaurants);
                    }

                    @Override
                    public void onFailure(Call<NearbySearchResponse> call, Throwable t) {
                        callback.onError(toException(t));
                    }
                });
    }

    public void fetchRestaurantDetails(String placeId, RestaurantCallback callback) {
        placesApiService.getPlaceDetails(placeId, DETAILS_FIELDS, BuildConfig.PLACES_API_KEY)
                .enqueue(new Callback<PlaceDetailsResponse>() {
                    @Override
                    public void onResponse(Call<PlaceDetailsResponse> call, Response<PlaceDetailsResponse> response) {
                        PlaceDetailsResponse body = response.body();
                        if (!response.isSuccessful() || body == null || body.getResult() == null
                                || !STATUS_OK.equals(body.getStatus())) {
                            callback.onError(new IOException("Places API details error: "
                                    + (body != null ? body.getStatus() : response.code())));
                            return;
                        }
                        callback.onSuccess(mapToRestaurant(body.getResult()));
                    }

                    @Override
                    public void onFailure(Call<PlaceDetailsResponse> call, Throwable t) {
                        callback.onError(toException(t));
                    }
                });
    }

    public void getRestaurantInterest(String placeId, RestaurantInterestCallback callback) {
        firestore.collection(RESTAURANTS_COLLECTION).document(placeId).get()
                .addOnSuccessListener(snapshot -> {
                    List<String> interested = extractStringList(snapshot, INTERESTED_FIELD);
                    List<String> liked = extractStringList(snapshot, LIKED_FIELD);
                    String currentUserId = getCurrentUserId();
                    boolean likedByCurrentUser = currentUserId != null && liked.contains(currentUserId);
                    callback.onSuccess(new RestaurantInterest(placeId, interested, likedByCurrentUser));
                })
                .addOnFailureListener(callback::onError);
    }

    public void setUserChoice(String placeId, VoidCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError(new IllegalStateException("setUserChoice requires an authenticated user"));
            return;
        }
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);
        DocumentReference newRestaurantRef = firestore.collection(RESTAURANTS_COLLECTION).document(placeId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot userSnapshot = transaction.get(userRef);
            String previousPlaceId = userSnapshot.contains(TODAY_CHOICE_FIELD)
                    ? userSnapshot.getString(TODAY_CHOICE_FIELD) : null;

            if (previousPlaceId != null && !previousPlaceId.equals(placeId)) {
                DocumentReference previousRestaurantRef =
                        firestore.collection(RESTAURANTS_COLLECTION).document(previousPlaceId);
                transaction.set(previousRestaurantRef,
                        Collections.singletonMap(INTERESTED_FIELD, FieldValue.arrayRemove(userId)),
                        SetOptions.merge());
            }
            transaction.set(newRestaurantRef,
                    Collections.singletonMap(INTERESTED_FIELD, FieldValue.arrayUnion(userId)),
                    SetOptions.merge());
            transaction.set(userRef,
                    Collections.singletonMap(TODAY_CHOICE_FIELD, placeId),
                    SetOptions.merge());
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void toggleLike(String placeId, LikeToggleCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError(new IllegalStateException("toggleLike requires an authenticated user"));
            return;
        }
        DocumentReference restaurantRef = firestore.collection(RESTAURANTS_COLLECTION).document(placeId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(restaurantRef);
            List<String> likedByUserIds = extractStringList(snapshot, LIKED_FIELD);
            boolean currentlyLiked = likedByUserIds.contains(userId);
            transaction.set(restaurantRef,
                    Collections.singletonMap(LIKED_FIELD,
                            currentlyLiked ? FieldValue.arrayRemove(userId) : FieldValue.arrayUnion(userId)),
                    SetOptions.merge());
            return !currentlyLiked;
        }).addOnSuccessListener(newLikedState -> callback.onSuccess((Boolean) newLikedState))
                .addOnFailureListener(callback::onError);
    }

    private Restaurant mapToRestaurant(PlaceResultDto dto) {
        String address = dto.getFormattedAddress() != null ? dto.getFormattedAddress() : dto.getVicinity();
        String phoneNumber = dto.getFormattedPhoneNumber() != null
                ? dto.getFormattedPhoneNumber() : dto.getInternationalPhoneNumber();
        LocationDto location = dto.getGeometry() != null ? dto.getGeometry().getLocation() : null;
        double lat = location != null ? location.getLat() : 0;
        double lng = location != null ? location.getLng() : 0;
        List<String> types = dto.getTypes();

        return new Restaurant(
                dto.getPlaceId(),
                dto.getName(),
                address,
                lat,
                lng,
                phoneNumber,
                dto.getWebsite(),
                buildPhotoUrl(dto.getPhotos()),
                types != null && !types.isEmpty() ? types.get(0) : null,
                dto.getRating(),
                buildOpeningHoursText(dto.getOpeningHours()));
    }

    private String buildPhotoUrl(List<PhotoDto> photos) {
        if (photos == null || photos.isEmpty()) {
            return null;
        }
        return PHOTO_BASE_URL + "?maxwidth=" + PHOTO_MAX_WIDTH
                + "&photo_reference=" + photos.get(0).getPhotoReference()
                + "&key=" + BuildConfig.PLACES_API_KEY;
    }

    private String buildOpeningHoursText(OpeningHoursDto openingHours) {
        if (openingHours == null) {
            return null;
        }
        List<String> weekdayText = openingHours.getWeekdayText();
        if (weekdayText != null && !weekdayText.isEmpty()) {
            return String.join("\n", weekdayText);
        }
        return openingHours.isOpenNow() ? "Open now" : "Closed";
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringList(DocumentSnapshot snapshot, String field) {
        Object value = snapshot.get(field);
        return value instanceof List ? (List<String>) value : new ArrayList<>();
    }

    private String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private Exception toException(Throwable t) {
        return t instanceof Exception ? (Exception) t : new Exception(t);
    }
}

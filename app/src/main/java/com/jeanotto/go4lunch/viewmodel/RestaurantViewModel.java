/*
RestaurantViewModel is the intermediary between the restaurant screens (list, map, detail) and RestaurantRepository.
It prepares the data to be displayed and reacts to user actions, but it never knows anything about Android's UI.

loadNearbyRestaurants is guarded against repeated network calls (Green Code requirement): once the list is LOADING
or has SUCCESS data, further calls are no-ops. This relies on RestaurantViewModel being shared at Activity scope
so the guard holds across fragment navigation, not just within a single fragment's lifecycle.
*/

package com.jeanotto.go4lunch.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jeanotto.go4lunch.model.Resource;
import com.jeanotto.go4lunch.model.Restaurant;
import com.jeanotto.go4lunch.model.RestaurantInterest;
import com.jeanotto.go4lunch.repository.RestaurantRepository;

import java.util.List;

public class RestaurantViewModel extends ViewModel {

    private final RestaurantRepository restaurantRepository;

    private final MutableLiveData<Resource<List<Restaurant>>> nearbyRestaurants = new MutableLiveData<>();
    private final MutableLiveData<Resource<Restaurant>> restaurantDetails = new MutableLiveData<>();
    private final MutableLiveData<Resource<RestaurantInterest>> restaurantInterest = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>> chooseRestaurantResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> toggleLikeResult = new MutableLiveData<>();

    public RestaurantViewModel(@NonNull RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public LiveData<Resource<List<Restaurant>>> getNearbyRestaurants() {
        return nearbyRestaurants;
    }

    public LiveData<Resource<Restaurant>> getRestaurantDetails() {
        return restaurantDetails;
    }

    public LiveData<Resource<RestaurantInterest>> getRestaurantInterest() {
        return restaurantInterest;
    }

    public LiveData<Resource<Void>> getChooseRestaurantResult() {
        return chooseRestaurantResult;
    }

    public LiveData<Resource<Boolean>> getToggleLikeResult() {
        return toggleLikeResult;
    }

    public void loadNearbyRestaurants(double lat, double lng) {
        Resource<List<Restaurant>> current = nearbyRestaurants.getValue();
        if (current != null && (current.getStatus() == Resource.Status.LOADING
                || current.getStatus() == Resource.Status.SUCCESS)) {
            return;
        }
        nearbyRestaurants.setValue(Resource.loading());
        restaurantRepository.fetchNearbyRestaurants(lat, lng, new RestaurantRepository.RestaurantsCallback() {
            @Override
            public void onSuccess(List<Restaurant> restaurants) {
                nearbyRestaurants.setValue(Resource.success(restaurants));
            }

            @Override
            public void onError(Exception exception) {
                nearbyRestaurants.setValue(Resource.error(exception.getMessage()));
            }
        });
    }

    public void loadRestaurantDetails(String placeId) {
        restaurantDetails.setValue(Resource.loading());
        restaurantRepository.fetchRestaurantDetails(placeId, new RestaurantRepository.RestaurantCallback() {
            @Override
            public void onSuccess(Restaurant restaurant) {
                restaurantDetails.setValue(Resource.success(restaurant));
            }

            @Override
            public void onError(Exception exception) {
                restaurantDetails.setValue(Resource.error(exception.getMessage()));
            }
        });
    }

    public void loadRestaurantInterest(String placeId) {
        restaurantInterest.setValue(Resource.loading());
        restaurantRepository.getRestaurantInterest(placeId, new RestaurantRepository.RestaurantInterestCallback() {
            @Override
            public void onSuccess(RestaurantInterest interest) {
                restaurantInterest.setValue(Resource.success(interest));
            }

            @Override
            public void onError(Exception exception) {
                restaurantInterest.setValue(Resource.error(exception.getMessage()));
            }
        });
    }

    public void chooseRestaurant(String placeId) {
        chooseRestaurantResult.setValue(Resource.loading());
        restaurantRepository.setUserChoice(placeId, new RestaurantRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                chooseRestaurantResult.setValue(Resource.success(null));
            }

            @Override
            public void onError(Exception exception) {
                chooseRestaurantResult.setValue(Resource.error(exception.getMessage()));
            }
        });
    }

    public void toggleLike(String placeId) {
        toggleLikeResult.setValue(Resource.loading());
        restaurantRepository.toggleLike(placeId, new RestaurantRepository.LikeToggleCallback() {
            @Override
            public void onSuccess(boolean likedByCurrentUser) {
                toggleLikeResult.setValue(Resource.success(likedByCurrentUser));
            }

            @Override
            public void onError(Exception exception) {
                toggleLikeResult.setValue(Resource.error(exception.getMessage()));
            }
        });
    }
}

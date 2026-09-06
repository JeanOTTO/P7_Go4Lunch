package com.jeanotto.go4lunch.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.jeanotto.go4lunch.model.Resource;
import com.jeanotto.go4lunch.model.Restaurant;
import com.jeanotto.go4lunch.model.RestaurantInterest;
import com.jeanotto.go4lunch.repository.RestaurantRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class RestaurantViewModelTest {

    private static final String PLACE_ID = "place-id";

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private Restaurant restaurant;

    @Captor
    private ArgumentCaptor<RestaurantRepository.RestaurantCallback> restaurantCallbackCaptor;

    @Captor
    private ArgumentCaptor<RestaurantRepository.RestaurantInterestCallback> restaurantInterestCallbackCaptor;

    @Captor
    private ArgumentCaptor<RestaurantRepository.VoidCallback> voidCallbackCaptor;

    @Captor
    private ArgumentCaptor<RestaurantRepository.LikeToggleCallback> likeToggleCallbackCaptor;

    private RestaurantViewModel restaurantViewModel;

    @Before
    public void setUp() {
        restaurantViewModel = new RestaurantViewModel(restaurantRepository);
    }

    @Test
    public void loadRestaurantDetails_emitsLoadingThenSuccess() {
        List<Resource<Restaurant>> emissions = new ArrayList<>();
        restaurantViewModel.getRestaurantDetails().observeForever(emissions::add);

        restaurantViewModel.loadRestaurantDetails(PLACE_ID);

        verify(restaurantRepository).fetchRestaurantDetails(eq(PLACE_ID), restaurantCallbackCaptor.capture());
        restaurantCallbackCaptor.getValue().onSuccess(restaurant);

        assertEquals(2, emissions.size());
        assertEquals(Resource.Status.LOADING, emissions.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, emissions.get(1).getStatus());
        assertEquals(restaurant, emissions.get(1).getData());
    }

    @Test
    public void loadRestaurantDetails_emitsLoadingThenError() {
        List<Resource<Restaurant>> emissions = new ArrayList<>();
        restaurantViewModel.getRestaurantDetails().observeForever(emissions::add);

        restaurantViewModel.loadRestaurantDetails(PLACE_ID);

        verify(restaurantRepository).fetchRestaurantDetails(eq(PLACE_ID), restaurantCallbackCaptor.capture());
        restaurantCallbackCaptor.getValue().onError(new Exception("boom"));

        assertEquals(2, emissions.size());
        assertEquals(Resource.Status.LOADING, emissions.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, emissions.get(1).getStatus());
        assertEquals("boom", emissions.get(1).getMessage());
    }

    @Test
    public void loadRestaurantInterest_emitsLoadingThenSuccess() {
        RestaurantInterest interest = new RestaurantInterest(PLACE_ID, Collections.singletonList("user-1"), true);
        List<Resource<RestaurantInterest>> emissions = new ArrayList<>();
        restaurantViewModel.getRestaurantInterest().observeForever(emissions::add);

        restaurantViewModel.loadRestaurantInterest(PLACE_ID);

        verify(restaurantRepository).getRestaurantInterest(eq(PLACE_ID), restaurantInterestCallbackCaptor.capture());
        restaurantInterestCallbackCaptor.getValue().onSuccess(interest);

        assertEquals(2, emissions.size());
        assertEquals(Resource.Status.LOADING, emissions.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, emissions.get(1).getStatus());
        assertEquals(interest, emissions.get(1).getData());
    }

    @Test
    public void loadRestaurantInterest_emitsLoadingThenError() {
        List<Resource<RestaurantInterest>> emissions = new ArrayList<>();
        restaurantViewModel.getRestaurantInterest().observeForever(emissions::add);

        restaurantViewModel.loadRestaurantInterest(PLACE_ID);

        verify(restaurantRepository).getRestaurantInterest(eq(PLACE_ID), restaurantInterestCallbackCaptor.capture());
        restaurantInterestCallbackCaptor.getValue().onError(new Exception("boom"));

        assertEquals(2, emissions.size());
        assertEquals(Resource.Status.LOADING, emissions.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, emissions.get(1).getStatus());
        assertEquals("boom", emissions.get(1).getMessage());
    }

    @Test
    public void chooseRestaurant_emitsLoadingThenSuccess() {
        List<Resource<Void>> emissions = new ArrayList<>();
        restaurantViewModel.getChooseRestaurantResult().observeForever(emissions::add);

        restaurantViewModel.chooseRestaurant(PLACE_ID);

        verify(restaurantRepository).setUserChoice(eq(PLACE_ID), voidCallbackCaptor.capture());
        voidCallbackCaptor.getValue().onSuccess();

        assertEquals(2, emissions.size());
        assertEquals(Resource.Status.LOADING, emissions.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, emissions.get(1).getStatus());
        assertNull(emissions.get(1).getData());
    }

    @Test
    public void chooseRestaurant_emitsLoadingThenError() {
        List<Resource<Void>> emissions = new ArrayList<>();
        restaurantViewModel.getChooseRestaurantResult().observeForever(emissions::add);

        restaurantViewModel.chooseRestaurant(PLACE_ID);

        verify(restaurantRepository).setUserChoice(eq(PLACE_ID), voidCallbackCaptor.capture());
        voidCallbackCaptor.getValue().onError(new Exception("boom"));

        assertEquals(2, emissions.size());
        assertEquals(Resource.Status.LOADING, emissions.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, emissions.get(1).getStatus());
        assertEquals("boom", emissions.get(1).getMessage());
    }

    @Test
    public void toggleLike_emitsLoadingThenSuccess() {
        List<Resource<Boolean>> emissions = new ArrayList<>();
        restaurantViewModel.getToggleLikeResult().observeForever(emissions::add);

        restaurantViewModel.toggleLike(PLACE_ID);

        verify(restaurantRepository).toggleLike(eq(PLACE_ID), likeToggleCallbackCaptor.capture());
        likeToggleCallbackCaptor.getValue().onSuccess(true);

        assertEquals(2, emissions.size());
        assertEquals(Resource.Status.LOADING, emissions.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, emissions.get(1).getStatus());
        assertEquals(Boolean.TRUE, emissions.get(1).getData());
    }

    @Test
    public void toggleLike_emitsLoadingThenError() {
        List<Resource<Boolean>> emissions = new ArrayList<>();
        restaurantViewModel.getToggleLikeResult().observeForever(emissions::add);

        restaurantViewModel.toggleLike(PLACE_ID);

        verify(restaurantRepository).toggleLike(eq(PLACE_ID), likeToggleCallbackCaptor.capture());
        likeToggleCallbackCaptor.getValue().onError(new Exception("boom"));

        assertEquals(2, emissions.size());
        assertEquals(Resource.Status.LOADING, emissions.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, emissions.get(1).getStatus());
        assertEquals("boom", emissions.get(1).getMessage());
    }
}

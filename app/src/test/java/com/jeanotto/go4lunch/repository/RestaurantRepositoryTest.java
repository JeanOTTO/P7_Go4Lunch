package com.jeanotto.go4lunch.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.gson.Gson;
import com.jeanotto.go4lunch.api.PlacesApiService;
import com.jeanotto.go4lunch.api.dto.PlaceDetailsResponse;
import com.jeanotto.go4lunch.model.Restaurant;
import com.jeanotto.go4lunch.model.RestaurantInterest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class RestaurantRepositoryTest {

    private static final String PLACE_ID = "place-id";
    private static final String USER_ID = "user-1";
    private static final String INTERESTED_FIELD = "interestedUserIds";
    private static final String LIKED_FIELD = "likedByUserIds";
    private static final String TODAY_CHOICE_FIELD = "todayChoicePlaceId";

    @Mock
    private PlacesApiService placesApiService;
    @Mock
    private FirebaseFirestore firestore;
    @Mock
    private FirebaseAuth firebaseAuth;
    @Mock
    private FirebaseUser firebaseUser;

    @Mock
    private Call<PlaceDetailsResponse> detailsCall;

    @Mock
    private CollectionReference restaurantsCollection;
    @Mock
    private CollectionReference usersCollection;
    @Mock
    private DocumentReference restaurantDocRef;
    @Mock
    private DocumentReference userDocRef;

    @Mock
    private Task<DocumentSnapshot> getSnapshotTask;
    @Mock
    private DocumentSnapshot documentSnapshot;

    @Mock
    private Task<Object> transactionTask;
    @Mock
    private Transaction transaction;
    @Mock
    private DocumentSnapshot userSnapshot;

    @Captor
    private ArgumentCaptor<Callback<PlaceDetailsResponse>> detailsCallbackCaptor;
    @Captor
    private ArgumentCaptor<OnSuccessListener<DocumentSnapshot>> snapshotSuccessCaptor;
    @Captor
    private ArgumentCaptor<OnFailureListener> snapshotFailureCaptor;
    @Captor
    private ArgumentCaptor<Transaction.Function> transactionFunctionCaptor;
    @Captor
    private ArgumentCaptor<OnSuccessListener<Object>> transactionSuccessCaptor;
    @Captor
    private ArgumentCaptor<OnFailureListener> transactionFailureCaptor;

    private RestaurantRepository restaurantRepository;

    @Before
    public void setUp() {
        restaurantRepository = new RestaurantRepository(placesApiService, firestore, firebaseAuth);

        when(placesApiService.getPlaceDetails(any(), any(), any())).thenReturn(detailsCall);

        when(firestore.collection("restaurants")).thenReturn(restaurantsCollection);
        when(firestore.collection("users")).thenReturn(usersCollection);
        when(restaurantsCollection.document(PLACE_ID)).thenReturn(restaurantDocRef);
        when(usersCollection.document(USER_ID)).thenReturn(userDocRef);

        when(restaurantDocRef.get()).thenReturn(getSnapshotTask);
        when(getSnapshotTask.addOnSuccessListener(any())).thenReturn(getSnapshotTask);
        when(getSnapshotTask.addOnFailureListener(any())).thenReturn(getSnapshotTask);

        when(firestore.runTransaction((Transaction.Function) any())).thenReturn(transactionTask);
        when(transactionTask.addOnSuccessListener(any())).thenReturn(transactionTask);
        when(transactionTask.addOnFailureListener(any())).thenReturn(transactionTask);
    }

    // ---- fetchRestaurantDetails ----

    @Test
    public void fetchRestaurantDetails_success_mapsFullRestaurant() {
        String json = "{\"result\":{"
                + "\"place_id\":\"place-id\","
                + "\"name\":\"Le Bouchon\","
                + "\"formatted_address\":\"12 rue de la Republique\","
                + "\"formatted_phone_number\":\"+33123456789\","
                + "\"website\":\"https://example.com\","
                + "\"geometry\":{\"location\":{\"lat\":45.75,\"lng\":4.85}},"
                + "\"rating\":4.5,"
                + "\"opening_hours\":{\"open_now\":true,\"weekday_text\":[\"Monday: 9AM-6PM\"]},"
                + "\"photos\":[{\"photo_reference\":\"photo-ref\"}],"
                + "\"types\":[\"restaurant\",\"food\"]"
                + "},\"status\":\"OK\"}";
        PlaceDetailsResponse response = new Gson().fromJson(json, PlaceDetailsResponse.class);
        RestaurantRepository.RestaurantCallback callback = mock(RestaurantRepository.RestaurantCallback.class);

        restaurantRepository.fetchRestaurantDetails(PLACE_ID, callback);

        verify(detailsCall).enqueue(detailsCallbackCaptor.capture());
        detailsCallbackCaptor.getValue().onResponse(detailsCall, Response.success(response));

        ArgumentCaptor<Restaurant> restaurantCaptor = ArgumentCaptor.forClass(Restaurant.class);
        verify(callback).onSuccess(restaurantCaptor.capture());
        Restaurant restaurant = restaurantCaptor.getValue();
        assertEquals(PLACE_ID, restaurant.getPlaceId());
        assertEquals("Le Bouchon", restaurant.getName());
        assertEquals("12 rue de la Republique", restaurant.getAddress());
        assertEquals("+33123456789", restaurant.getPhoneNumber());
        assertEquals("https://example.com", restaurant.getWebsiteUrl());
        assertEquals(45.75, restaurant.getLatitude(), 0.0001);
        assertEquals(4.85, restaurant.getLongitude(), 0.0001);
        assertEquals(4.5, restaurant.getRating(), 0.0001);
        assertEquals("restaurant", restaurant.getType());
        assertEquals("Monday: 9AM-6PM", restaurant.getOpeningHoursText());
        assertTrue(restaurant.getPhotoUrl().contains("photo-ref"));
    }

    @Test
    public void fetchRestaurantDetails_success_mapsNullPhotoAndFallbackFields() {
        String json = "{\"result\":{"
                + "\"place_id\":\"place-id\","
                + "\"name\":\"Le Bouchon\","
                + "\"vicinity\":\"Lyon\","
                + "\"international_phone_number\":\"+33123456789\","
                + "\"geometry\":{\"location\":{\"lat\":45.75,\"lng\":4.85}},"
                + "\"rating\":3.0,"
                + "\"opening_hours\":{\"open_now\":false},"
                + "\"types\":[]"
                + "},\"status\":\"OK\"}";
        PlaceDetailsResponse response = new Gson().fromJson(json, PlaceDetailsResponse.class);
        RestaurantRepository.RestaurantCallback callback = mock(RestaurantRepository.RestaurantCallback.class);

        restaurantRepository.fetchRestaurantDetails(PLACE_ID, callback);

        verify(detailsCall).enqueue(detailsCallbackCaptor.capture());
        detailsCallbackCaptor.getValue().onResponse(detailsCall, Response.success(response));

        ArgumentCaptor<Restaurant> restaurantCaptor = ArgumentCaptor.forClass(Restaurant.class);
        verify(callback).onSuccess(restaurantCaptor.capture());
        Restaurant restaurant = restaurantCaptor.getValue();
        assertEquals("Lyon", restaurant.getAddress());
        assertEquals("+33123456789", restaurant.getPhoneNumber());
        assertNull(restaurant.getPhotoUrl());
        assertNull(restaurant.getType());
        assertEquals("Closed", restaurant.getOpeningHoursText());
    }

    @Test
    public void fetchRestaurantDetails_nonOkStatus_notifiesError() {
        String json = "{\"result\":{\"place_id\":\"place-id\"},\"status\":\"NOT_FOUND\"}";
        PlaceDetailsResponse response = new Gson().fromJson(json, PlaceDetailsResponse.class);
        RestaurantRepository.RestaurantCallback callback = mock(RestaurantRepository.RestaurantCallback.class);

        restaurantRepository.fetchRestaurantDetails(PLACE_ID, callback);

        verify(detailsCall).enqueue(detailsCallbackCaptor.capture());
        detailsCallbackCaptor.getValue().onResponse(detailsCall, Response.success(response));

        verify(callback).onError(any(Exception.class));
    }

    @Test
    public void fetchRestaurantDetails_onFailure_notifiesError() {
        RestaurantRepository.RestaurantCallback callback = mock(RestaurantRepository.RestaurantCallback.class);
        Exception exception = new Exception("network error");

        restaurantRepository.fetchRestaurantDetails(PLACE_ID, callback);

        verify(detailsCall).enqueue(detailsCallbackCaptor.capture());
        detailsCallbackCaptor.getValue().onFailure(detailsCall, exception);

        verify(callback).onError(exception);
    }

    // ---- getRestaurantInterest ----

    @Test
    public void getRestaurantInterest_success_likedByCurrentUser() {
        when(firebaseAuth.getCurrentUser()).thenReturn(firebaseUser);
        when(firebaseUser.getUid()).thenReturn(USER_ID);
        when(documentSnapshot.get(INTERESTED_FIELD)).thenReturn(Arrays.asList(USER_ID, "user-2"));
        when(documentSnapshot.get(LIKED_FIELD)).thenReturn(Collections.singletonList(USER_ID));
        RestaurantRepository.RestaurantInterestCallback callback = mock(RestaurantRepository.RestaurantInterestCallback.class);

        restaurantRepository.getRestaurantInterest(PLACE_ID, callback);

        verify(getSnapshotTask).addOnSuccessListener(snapshotSuccessCaptor.capture());
        snapshotSuccessCaptor.getValue().onSuccess(documentSnapshot);

        ArgumentCaptor<RestaurantInterest> interestCaptor = ArgumentCaptor.forClass(RestaurantInterest.class);
        verify(callback).onSuccess(interestCaptor.capture());
        RestaurantInterest interest = interestCaptor.getValue();
        assertEquals(PLACE_ID, interest.getPlaceId());
        assertEquals(Arrays.asList(USER_ID, "user-2"), interest.getInterestedUserIds());
        assertTrue(interest.isLikedByCurrentUser());
    }

    @Test
    public void getRestaurantInterest_success_noCurrentUser_notLiked() {
        when(firebaseAuth.getCurrentUser()).thenReturn(null);
        when(documentSnapshot.get(INTERESTED_FIELD)).thenReturn(null);
        when(documentSnapshot.get(LIKED_FIELD)).thenReturn(null);
        RestaurantRepository.RestaurantInterestCallback callback = mock(RestaurantRepository.RestaurantInterestCallback.class);

        restaurantRepository.getRestaurantInterest(PLACE_ID, callback);

        verify(getSnapshotTask).addOnSuccessListener(snapshotSuccessCaptor.capture());
        snapshotSuccessCaptor.getValue().onSuccess(documentSnapshot);

        ArgumentCaptor<RestaurantInterest> interestCaptor = ArgumentCaptor.forClass(RestaurantInterest.class);
        verify(callback).onSuccess(interestCaptor.capture());
        RestaurantInterest interest = interestCaptor.getValue();
        assertTrue(interest.getInterestedUserIds().isEmpty());
        assertFalse(interest.isLikedByCurrentUser());
    }

    @Test
    public void getRestaurantInterest_failure_notifiesError() {
        Exception exception = new Exception("firestore error");
        RestaurantRepository.RestaurantInterestCallback callback = mock(RestaurantRepository.RestaurantInterestCallback.class);

        restaurantRepository.getRestaurantInterest(PLACE_ID, callback);

        verify(getSnapshotTask).addOnFailureListener(snapshotFailureCaptor.capture());
        snapshotFailureCaptor.getValue().onFailure(exception);

        verify(callback).onError(exception);
    }

    // ---- setUserChoice ----

    @Test
    public void setUserChoice_noAuthenticatedUser_notifiesErrorWithoutStartingTransaction() {
        when(firebaseAuth.getCurrentUser()).thenReturn(null);
        RestaurantRepository.VoidCallback callback = mock(RestaurantRepository.VoidCallback.class);

        restaurantRepository.setUserChoice(PLACE_ID, callback);

        verify(callback).onError(any(IllegalStateException.class));
        verify(firestore, never()).runTransaction((Transaction.Function) any());
    }

    @Test
    public void setUserChoice_firstChoice_setsInterestAndTodayChoice() throws Exception {
        when(firebaseAuth.getCurrentUser()).thenReturn(firebaseUser);
        when(firebaseUser.getUid()).thenReturn(USER_ID);
        when(transaction.get(userDocRef)).thenReturn(userSnapshot);
        when(userSnapshot.contains(TODAY_CHOICE_FIELD)).thenReturn(false);
        RestaurantRepository.VoidCallback callback = mock(RestaurantRepository.VoidCallback.class);

        restaurantRepository.setUserChoice(PLACE_ID, callback);

        verify(firestore).runTransaction(transactionFunctionCaptor.capture());
        transactionFunctionCaptor.getValue().apply(transaction);

        verify(transaction).set(eq(restaurantDocRef), any(), any(SetOptions.class));
        verify(transaction).set(eq(userDocRef), (Map) any(), any(SetOptions.class));

        verify(transactionTask).addOnSuccessListener(transactionSuccessCaptor.capture());
        transactionSuccessCaptor.getValue().onSuccess(null);

        verify(callback).onSuccess();
    }

    @Test
    public void setUserChoice_switchingRestaurant_removesInterestFromPrevious() throws Exception {
        when(firebaseAuth.getCurrentUser()).thenReturn(firebaseUser);
        when(firebaseUser.getUid()).thenReturn(USER_ID);
        when(transaction.get(userDocRef)).thenReturn(userSnapshot);
        when(userSnapshot.contains(TODAY_CHOICE_FIELD)).thenReturn(true);
        when(userSnapshot.getString(TODAY_CHOICE_FIELD)).thenReturn("previous-place-id");
        DocumentReference previousRestaurantDocRef = mock(DocumentReference.class);
        when(restaurantsCollection.document("previous-place-id")).thenReturn(previousRestaurantDocRef);
        RestaurantRepository.VoidCallback callback = mock(RestaurantRepository.VoidCallback.class);

        restaurantRepository.setUserChoice(PLACE_ID, callback);

        verify(firestore).runTransaction(transactionFunctionCaptor.capture());
        transactionFunctionCaptor.getValue().apply(transaction);

        verify(transaction).set(eq(previousRestaurantDocRef), any(), any(SetOptions.class));
        verify(transaction).set(eq(restaurantDocRef), any(), any(SetOptions.class));
        verify(transaction).set(eq(userDocRef), any(), any(SetOptions.class));
    }

    @Test
    public void setUserChoice_transactionFailure_notifiesError() {
        when(firebaseAuth.getCurrentUser()).thenReturn(firebaseUser);
        when(firebaseUser.getUid()).thenReturn(USER_ID);
        Exception exception = new Exception("transaction failed");
        RestaurantRepository.VoidCallback callback = mock(RestaurantRepository.VoidCallback.class);

        restaurantRepository.setUserChoice(PLACE_ID, callback);

        verify(transactionTask).addOnFailureListener(transactionFailureCaptor.capture());
        transactionFailureCaptor.getValue().onFailure(exception);

        verify(callback).onError(exception);
    }

    // ---- toggleLike ----

    @Test
    public void toggleLike_noAuthenticatedUser_notifiesErrorWithoutStartingTransaction() {
        when(firebaseAuth.getCurrentUser()).thenReturn(null);
        RestaurantRepository.LikeToggleCallback callback = mock(RestaurantRepository.LikeToggleCallback.class);

        restaurantRepository.toggleLike(PLACE_ID, callback);

        verify(callback).onError(any(IllegalStateException.class));
        verify(firestore, never()).runTransaction((Transaction.Function) any());
    }

    @Test
    public void toggleLike_notCurrentlyLiked_addsLikeAndReturnsTrue() throws Exception {
        when(firebaseAuth.getCurrentUser()).thenReturn(firebaseUser);
        when(firebaseUser.getUid()).thenReturn(USER_ID);
        when(transaction.get(restaurantDocRef)).thenReturn(documentSnapshot);
        when(documentSnapshot.get(LIKED_FIELD)).thenReturn(Collections.emptyList());
        RestaurantRepository.LikeToggleCallback callback = mock(RestaurantRepository.LikeToggleCallback.class);

        restaurantRepository.toggleLike(PLACE_ID, callback);

        verify(firestore).runTransaction(transactionFunctionCaptor.capture());
        Object result = transactionFunctionCaptor.getValue().apply(transaction);
        assertEquals(Boolean.TRUE, result);

        verify(transactionTask).addOnSuccessListener(transactionSuccessCaptor.capture());
        transactionSuccessCaptor.getValue().onSuccess(true);

        verify(callback).onSuccess(true);
    }

    @Test
    public void toggleLike_alreadyLiked_removesLikeAndReturnsFalse() throws Exception {
        when(firebaseAuth.getCurrentUser()).thenReturn(firebaseUser);
        when(firebaseUser.getUid()).thenReturn(USER_ID);
        when(transaction.get(restaurantDocRef)).thenReturn(documentSnapshot);
        when(documentSnapshot.get(LIKED_FIELD)).thenReturn(Collections.singletonList(USER_ID));
        RestaurantRepository.LikeToggleCallback callback = mock(RestaurantRepository.LikeToggleCallback.class);

        restaurantRepository.toggleLike(PLACE_ID, callback);

        verify(firestore).runTransaction(transactionFunctionCaptor.capture());
        Object result = transactionFunctionCaptor.getValue().apply(transaction);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void toggleLike_transactionFailure_notifiesError() {
        when(firebaseAuth.getCurrentUser()).thenReturn(firebaseUser);
        when(firebaseUser.getUid()).thenReturn(USER_ID);
        Exception exception = new Exception("transaction failed");
        RestaurantRepository.LikeToggleCallback callback = mock(RestaurantRepository.LikeToggleCallback.class);

        restaurantRepository.toggleLike(PLACE_ID, callback);

        verify(transactionTask).addOnFailureListener(transactionFailureCaptor.capture());
        transactionFailureCaptor.getValue().onFailure(exception);

        verify(callback).onError(exception);
    }
}

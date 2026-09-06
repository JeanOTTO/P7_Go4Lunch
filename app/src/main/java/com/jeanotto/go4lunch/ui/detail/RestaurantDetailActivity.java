package com.jeanotto.go4lunch.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.jeanotto.go4lunch.R;
import com.jeanotto.go4lunch.model.Resource;
import com.jeanotto.go4lunch.model.Restaurant;
import com.jeanotto.go4lunch.model.RestaurantInterest;
import com.jeanotto.go4lunch.viewmodel.RestaurantViewModel;
import com.jeanotto.go4lunch.viewmodel.ViewModelFactory;

public class RestaurantDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE_ID = "extra_place_id";

    private RestaurantViewModel restaurantViewModel;
    private String placeId;
    private Restaurant currentRestaurant;

    private View rootView;
    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private TextView errorView;
    private Button retryButton;
    private NestedScrollView contentScroll;

    private ImageView photoView;
    private TextView nameView;
    private RatingBar ratingBar;
    private TextView typeView;
    private TextView addressView;
    private TextView hoursView;
    private TextView interestCountView;

    private View callButtonContainer;
    private View websiteButtonContainer;
    private ImageButton callButton;
    private ImageButton websiteButton;
    private ImageButton likeButton;
    private Button chooseButton;

    public static void start(@NonNull Context context, @NonNull String placeId) {
        Intent intent = new Intent(context, RestaurantDetailActivity.class);
        intent.putExtra(EXTRA_PLACE_ID, placeId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);
        placeId = getIntent().getStringExtra(EXTRA_PLACE_ID);
        if (placeId == null) {
            finish();
            return;
        }

        restaurantViewModel = new ViewModelProvider(this, ViewModelFactory.getInstance())
                .get(RestaurantViewModel.class);

        bindViews();
        setupListeners();
        observeRestaurantDetails();
        observeRestaurantInterest();
        observeToggleLikeResult();
        observeChooseRestaurantResult();

        if (savedInstanceState == null) {
            restaurantViewModel.loadRestaurantDetails(placeId);
            restaurantViewModel.loadRestaurantInterest(placeId);
        }
    }

    private void bindViews() {
        rootView = findViewById(android.R.id.content);
        toolbar = findViewById(R.id.toolbar_restaurant_detail);

        progressBar = findViewById(R.id.progress_bar_restaurant_detail);
        errorView = findViewById(R.id.text_restaurant_detail_error);
        retryButton = findViewById(R.id.button_restaurant_detail_retry);
        contentScroll = findViewById(R.id.scroll_restaurant_detail_content);

        photoView = findViewById(R.id.image_restaurant_photo);
        nameView = findViewById(R.id.text_restaurant_detail_name);
        ratingBar = findViewById(R.id.rating_bar_restaurant_detail);
        typeView = findViewById(R.id.text_restaurant_detail_type);
        addressView = findViewById(R.id.text_restaurant_detail_address);
        hoursView = findViewById(R.id.text_restaurant_detail_hours);
        interestCountView = findViewById(R.id.text_restaurant_detail_interest_count);

        callButtonContainer = findViewById(R.id.button_restaurant_call);
        websiteButtonContainer = findViewById(R.id.button_restaurant_website);
        callButton = findViewById(R.id.image_button_restaurant_call);
        websiteButton = findViewById(R.id.image_button_restaurant_website);
        likeButton = findViewById(R.id.image_button_restaurant_like);
        chooseButton = findViewById(R.id.button_choose_restaurant);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        retryButton.setOnClickListener(v -> restaurantViewModel.loadRestaurantDetails(placeId));
        callButton.setOnClickListener(v -> dialPhone());
        websiteButton.setOnClickListener(v -> openWebsite());
        likeButton.setOnClickListener(v -> restaurantViewModel.toggleLike(placeId));
        chooseButton.setOnClickListener(v -> restaurantViewModel.chooseRestaurant(placeId));
    }

    private void observeRestaurantDetails() {
        restaurantViewModel.getRestaurantDetails().observe(this, this::onRestaurantDetailsChanged);
    }

    private void onRestaurantDetailsChanged(Resource<Restaurant> resource) {
        switch (resource.getStatus()) {
            case LOADING:
                progressBar.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
                retryButton.setVisibility(View.GONE);
                contentScroll.setVisibility(View.GONE);
                break;
            case SUCCESS:
                progressBar.setVisibility(View.GONE);
                errorView.setVisibility(View.GONE);
                retryButton.setVisibility(View.GONE);
                contentScroll.setVisibility(View.VISIBLE);
                bindRestaurant(resource.getData());
                break;
            case ERROR:
                progressBar.setVisibility(View.GONE);
                contentScroll.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.VISIBLE);
                errorView.setText(resource.getMessage() != null
                        ? resource.getMessage() : getString(R.string.error_restaurant_details_unavailable));
                break;
        }
    }

    private void bindRestaurant(@NonNull Restaurant restaurant) {
        currentRestaurant = restaurant;
        toolbar.setTitle(restaurant.getName());
        nameView.setText(restaurant.getName());
        typeView.setText(restaurant.getType());
        addressView.setText(restaurant.getAddress());
        hoursView.setText(restaurant.getOpeningHoursText());
        ratingBar.setRating((float) restaurant.getRating());

        Glide.with(this)
                .load(restaurant.getPhotoUrl())
                .placeholder(R.drawable.ic_restaurant_placeholder)
                .error(R.drawable.ic_restaurant_placeholder)
                .centerCrop()
                .into(photoView);

        callButtonContainer.setVisibility(restaurant.getPhoneNumber() != null ? View.VISIBLE : View.GONE);
        websiteButtonContainer.setVisibility(restaurant.getWebsiteUrl() != null ? View.VISIBLE : View.GONE);
    }

    private void observeRestaurantInterest() {
        restaurantViewModel.getRestaurantInterest().observe(this, this::onRestaurantInterestChanged);
    }

    private void onRestaurantInterestChanged(Resource<RestaurantInterest> resource) {
        switch (resource.getStatus()) {
            case LOADING:
                break;
            case SUCCESS:
                RestaurantInterest interest = resource.getData();
                int count = interest.getInterestedUserIds().size();
                interestCountView.setText(getResources().getQuantityString(
                        R.plurals.colleagues_interested, count, count));
                likeButton.setImageResource(interest.isLikedByCurrentUser()
                        ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
                break;
            case ERROR:
                interestCountView.setText(null);
                break;
        }
    }

    private void observeToggleLikeResult() {
        restaurantViewModel.getToggleLikeResult().observe(this, this::onToggleLikeResultChanged);
    }

    private void onToggleLikeResultChanged(Resource<Boolean> resource) {
        switch (resource.getStatus()) {
            case LOADING:
                likeButton.setEnabled(false);
                break;
            case SUCCESS:
                likeButton.setEnabled(true);
                likeButton.setImageResource(Boolean.TRUE.equals(resource.getData())
                        ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
                break;
            case ERROR:
                likeButton.setEnabled(true);
                Snackbar.make(rootView, resource.getMessage() != null
                                ? resource.getMessage() : getString(R.string.error_toggle_like),
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    private void observeChooseRestaurantResult() {
        restaurantViewModel.getChooseRestaurantResult().observe(this, this::onChooseRestaurantResultChanged);
    }

    private void onChooseRestaurantResultChanged(Resource<Void> resource) {
        switch (resource.getStatus()) {
            case LOADING:
                chooseButton.setEnabled(false);
                break;
            case SUCCESS:
                chooseButton.setEnabled(true);
                Snackbar.make(rootView, R.string.restaurant_chosen_confirmation, Snackbar.LENGTH_LONG).show();
                restaurantViewModel.loadRestaurantInterest(placeId);
                break;
            case ERROR:
                chooseButton.setEnabled(true);
                Snackbar.make(rootView, resource.getMessage() != null
                                ? resource.getMessage() : getString(R.string.error_choose_restaurant),
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    private void dialPhone() {
        if (currentRestaurant == null || currentRestaurant.getPhoneNumber() == null) {
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentRestaurant.getPhoneNumber())));
    }

    private void openWebsite() {
        if (currentRestaurant == null || currentRestaurant.getWebsiteUrl() == null) {
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentRestaurant.getWebsiteUrl())));
    }
}

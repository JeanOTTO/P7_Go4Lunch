/*
RestaurantAdapter binds a List<Restaurant> to the RecyclerView used by ListFragment.
Distance is computed here (not in the ViewModel) because it depends on the user's current
position, which can change independently of the restaurant data itself, and because this is
UI-layer formatting logic with no test coverage requirement.
*/

package com.jeanotto.go4lunch.ui.list;

import android.annotation.SuppressLint;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.jeanotto.go4lunch.R;
import com.jeanotto.go4lunch.model.Restaurant;
import com.jeanotto.go4lunch.model.UserLocation;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {

    public interface OnRestaurantClickListener {
        void onRestaurantClick(@NonNull Restaurant restaurant);
    }

    private final OnRestaurantClickListener onRestaurantClickListener;
    private List<Restaurant> restaurants = Collections.emptyList();
    @Nullable
    private UserLocation userLocation;

    public RestaurantAdapter(@NonNull OnRestaurantClickListener onRestaurantClickListener) {
        this.onRestaurantClickListener = onRestaurantClickListener;
    }

    // Full list replacement/re-render on every update; DiffUtil would be the next step if this
    // list becomes large enough for the naive full rebind to be noticeable.
    @SuppressLint("NotifyDataSetChanged")
    public void setRestaurants(@NonNull List<Restaurant> restaurants) {
        this.restaurants = restaurants;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setUserLocation(@NonNull UserLocation userLocation) {
        this.userLocation = userLocation;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant, parent, false);
        return new RestaurantViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        holder.bind(restaurants.get(position), userLocation, onRestaurantClickListener);
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    static class RestaurantViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameView;
        private final TextView typeView;
        private final TextView addressView;
        private final TextView hoursView;
        private final TextView distanceView;
        private final RatingBar ratingBar;

        RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.text_restaurant_name);
            typeView = itemView.findViewById(R.id.text_restaurant_type);
            addressView = itemView.findViewById(R.id.text_restaurant_address);
            hoursView = itemView.findViewById(R.id.text_restaurant_hours);
            distanceView = itemView.findViewById(R.id.text_restaurant_distance);
            ratingBar = itemView.findViewById(R.id.rating_bar_restaurant);
        }

        void bind(@NonNull Restaurant restaurant, @Nullable UserLocation userLocation,
                  @NonNull OnRestaurantClickListener onRestaurantClickListener) {
            nameView.setText(restaurant.getName());
            typeView.setText(restaurant.getType());
            addressView.setText(restaurant.getAddress());
            hoursView.setText(restaurant.getOpeningHoursText());
            ratingBar.setRating((float) restaurant.getRating());
            distanceView.setText(formatDistance(restaurant, userLocation));
            itemView.setOnClickListener(v -> onRestaurantClickListener.onRestaurantClick(restaurant));
        }

        private String formatDistance(@NonNull Restaurant restaurant, @Nullable UserLocation userLocation) {
            if (userLocation == null) {
                return "";
            }
            float[] results = new float[1];
            Location.distanceBetween(
                    userLocation.getLatitude(), userLocation.getLongitude(),
                    restaurant.getLatitude(), restaurant.getLongitude(),
                    results);
            float distanceInMeters = results[0];
            if (distanceInMeters < 1000f) {
                return Math.round(distanceInMeters) + " m";
            }
            return String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000f);
        }
    }
}

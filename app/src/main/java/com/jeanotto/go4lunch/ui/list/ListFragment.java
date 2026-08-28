package com.jeanotto.go4lunch.ui.list;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.snackbar.Snackbar;
import com.jeanotto.go4lunch.R;
import com.jeanotto.go4lunch.model.Resource;
import com.jeanotto.go4lunch.model.Restaurant;
import com.jeanotto.go4lunch.model.UserLocation;
import com.jeanotto.go4lunch.viewmodel.RestaurantViewModel;
import com.jeanotto.go4lunch.viewmodel.ViewModelFactory;

import java.util.List;
import java.util.Map;

public class ListFragment extends Fragment {

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult);

    private RestaurantViewModel restaurantViewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource cancellationTokenSource;
    private RestaurantAdapter adapter;
    private View rootView;
    private ProgressBar progressBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restaurantViewModel = new ViewModelProvider(requireActivity(), ViewModelFactory.getInstance())
                .get(RestaurantViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        progressBar = view.findViewById(R.id.progress_bar_list);

        adapter = new RestaurantAdapter();
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_restaurants);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        observeUserLocation();
        observeNearbyRestaurants();
        loadRestaurantsIfNeeded();
    }

    private void observeUserLocation() {
        restaurantViewModel.getUserLocation().observe(getViewLifecycleOwner(), location -> {
            if (location != null) {
                adapter.setUserLocation(location);
            }
        });
    }

    private void observeNearbyRestaurants() {
        restaurantViewModel.getNearbyRestaurants().observe(getViewLifecycleOwner(), this::onRestaurantsChanged);
    }

    private void onRestaurantsChanged(Resource<List<Restaurant>> resource) {
        switch (resource.getStatus()) {
            case LOADING:
                progressBar.setVisibility(View.VISIBLE);
                break;
            case SUCCESS:
                progressBar.setVisibility(View.GONE);
                adapter.setRestaurants(resource.getData());
                break;
            case ERROR:
                progressBar.setVisibility(View.GONE);
                Snackbar.make(rootView, resource.getMessage() != null
                        ? resource.getMessage() : getString(R.string.error_restaurants_unavailable),
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    private void loadRestaurantsIfNeeded() {
        if (restaurantViewModel.getNearbyRestaurants().getValue() != null) {
            return;
        }
        UserLocation knownLocation = restaurantViewModel.getUserLocation().getValue();
        if (knownLocation != null) {
            restaurantViewModel.loadNearbyRestaurants(knownLocation.getLatitude(), knownLocation.getLongitude());
        } else {
            checkLocationPermissionAndProceed();
        }
    }

    private void checkLocationPermissionAndProceed() {
        if (hasLocationPermission()) {
            fetchCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void onLocationPermissionResult(Map<String, Boolean> grants) {
        boolean granted = Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_FINE_LOCATION))
                || Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_COARSE_LOCATION));
        if (granted) {
            fetchCurrentLocation();
        } else {
            showLocationPermissionDeniedMessage();
        }
    }

    private void showLocationPermissionDeniedMessage() {
        boolean permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
        Snackbar.make(rootView, R.string.error_location_permission_denied, Snackbar.LENGTH_LONG)
                .setAction(permanentlyDenied ? R.string.settings : R.string.retry, v -> {
                    if (permanentlyDenied) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", requireContext().getPackageName(), null));
                        startActivity(intent);
                    } else {
                        checkLocationPermissionAndProceed();
                    }
                })
                .show();
    }

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocation() {
        if (!hasLocationPermission()) {
            return;
        }
        cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationTokenSource.getToken())
                .addOnSuccessListener(this::onLocationReady)
                .addOnFailureListener(e -> showLocationUnavailableMessage());
    }

    private void onLocationReady(@Nullable Location location) {
        if (location == null) {
            showLocationUnavailableMessage();
            return;
        }
        restaurantViewModel.setUserLocation(location.getLatitude(), location.getLongitude());
        restaurantViewModel.loadNearbyRestaurants(location.getLatitude(), location.getLongitude());
    }

    private void showLocationUnavailableMessage() {
        Snackbar.make(rootView, R.string.error_location_unavailable, Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, v -> fetchCurrentLocation())
                .show();
    }

    @Override
    public void onDestroyView() {
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
            cancellationTokenSource = null;
        }
        rootView = null;
        progressBar = null;
        adapter = null;
        super.onDestroyView();
    }
}

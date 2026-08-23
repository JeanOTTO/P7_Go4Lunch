package com.jeanotto.go4lunch.ui.map;

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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.jeanotto.go4lunch.R;
import com.jeanotto.go4lunch.model.Resource;
import com.jeanotto.go4lunch.model.Restaurant;
import com.jeanotto.go4lunch.viewmodel.RestaurantViewModel;
import com.jeanotto.go4lunch.viewmodel.ViewModelFactory;

import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final float DEFAULT_ZOOM = 16f;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult);

    private RestaurantViewModel restaurantViewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource cancellationTokenSource;
    private GoogleMap googleMap;
    private View rootView;
    private LatLng lastKnownUserLatLng;

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
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;

        FloatingActionButton fabRecenter = view.findViewById(R.id.fab_recenter);
        fabRecenter.setOnClickListener(v -> recenterOnMyLocation());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        observeNearbyRestaurants();
        checkLocationPermissionAndProceed();
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
        if (googleMap == null || !hasLocationPermission()) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
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
        lastKnownUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownUserLatLng, DEFAULT_ZOOM));
        restaurantViewModel.loadNearbyRestaurants(location.getLatitude(), location.getLongitude());
    }

    private void showLocationUnavailableMessage() {
        Snackbar.make(rootView, R.string.error_location_unavailable, Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, v -> fetchCurrentLocation())
                .show();
    }

    private void recenterOnMyLocation() {
        if (lastKnownUserLatLng != null && googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastKnownUserLatLng, DEFAULT_ZOOM));
        } else {
            checkLocationPermissionAndProceed();
        }
    }

    private void observeNearbyRestaurants() {
        restaurantViewModel.getNearbyRestaurants().observe(getViewLifecycleOwner(), this::onRestaurantsChanged);
    }

    private void onRestaurantsChanged(Resource<List<Restaurant>> resource) {
        switch (resource.getStatus()) {
            case LOADING:
                break;
            case SUCCESS:
                googleMap.clear();
                for (Restaurant restaurant : resource.getData()) {
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(restaurant.getLatitude(), restaurant.getLongitude()))
                            .title(restaurant.getName()));
                }
                break;
            case ERROR:
                Snackbar.make(rootView, resource.getMessage() != null
                        ? resource.getMessage() : getString(R.string.error_restaurants_unavailable),
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void onDestroyView() {
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
            cancellationTokenSource = null;
        }
        googleMap = null;
        rootView = null;
        super.onDestroyView();
    }
}

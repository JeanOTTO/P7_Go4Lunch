package com.jeanotto.go4lunch.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jeanotto.go4lunch.repository.AuthRepository;
import com.jeanotto.go4lunch.repository.RestaurantRepository;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private static volatile ViewModelFactory instance;

    private final AuthRepository authRepository;
    private final RestaurantRepository restaurantRepository;

    private ViewModelFactory(AuthRepository authRepository, RestaurantRepository restaurantRepository) {
        this.authRepository = authRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public static ViewModelFactory getInstance() {
        if (instance == null) {
            synchronized (ViewModelFactory.class) {
                if (instance == null) {
                    instance = new ViewModelFactory(AuthRepository.getInstance(), RestaurantRepository.getInstance());
                }
            }
        }
        return instance;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            return (T) new AuthViewModel(authRepository);
        }
        if (modelClass.isAssignableFrom(RestaurantViewModel.class)) {
            return (T) new RestaurantViewModel(restaurantRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}

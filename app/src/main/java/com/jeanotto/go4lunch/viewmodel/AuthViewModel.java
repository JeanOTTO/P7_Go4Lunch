package com.jeanotto.go4lunch.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.jeanotto.go4lunch.model.Resource;
import com.jeanotto.go4lunch.repository.AuthRepository;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Resource<FirebaseUser>> authResult = new MutableLiveData<>();

    public AuthViewModel(@NonNull AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Resource<FirebaseUser>> getAuthResult() {
        return authResult;
    }

    public boolean isUserLoggedIn() {
        return authRepository.isUserLoggedIn();
    }

    public void signInWithGoogleIdToken(String idToken) {
        authResult.setValue(Resource.loading());
        authRepository.signInWithGoogleIdToken(idToken, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                authResult.setValue(Resource.success(user));
            }

            @Override
            public void onError(Exception exception) {
                authResult.setValue(Resource.error(exception.getMessage()));
            }
        });
    }
}

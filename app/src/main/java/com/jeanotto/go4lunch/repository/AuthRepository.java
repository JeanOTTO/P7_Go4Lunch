/*
AuthRepository is the single entry point for the entire application to Firebase for anything related to user
authentication and sign-out. No other part of the code should communicate directly with Firebase for authentication.
*/


package com.jeanotto.go4lunch.repository;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthRepository {

    private static volatile AuthRepository instance;

    private final FirebaseAuth firebaseAuth;

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);

        void onError(Exception exception);
    }

    public AuthRepository(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    public static AuthRepository getInstance() {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository(FirebaseAuth.getInstance());
                }
            }
        }
        return instance;
    }

    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public void signInWithGoogleIdToken(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
                .addOnFailureListener(callback::onError);
    }

    public void signOut() {
        firebaseAuth.signOut();
    }
}

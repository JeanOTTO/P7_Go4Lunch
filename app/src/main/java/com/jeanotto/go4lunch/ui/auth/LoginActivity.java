package com.jeanotto.go4lunch.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.jeanotto.go4lunch.MainActivity;
import com.jeanotto.go4lunch.R;
import com.jeanotto.go4lunch.model.Resource;
import com.jeanotto.go4lunch.viewmodel.AuthViewModel;
import com.jeanotto.go4lunch.viewmodel.ViewModelFactory;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private CredentialManager credentialManager;
    private ProgressBar progressBar;
    private Button signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authViewModel = new ViewModelProvider(this, ViewModelFactory.getInstance()).get(AuthViewModel.class);

        if (authViewModel.isUserLoggedIn()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        credentialManager = CredentialManager.create(this);

        progressBar = findViewById(R.id.progress_bar);
        signInButton = findViewById(R.id.button_google_sign_in);
        signInButton.setOnClickListener(v -> requestGoogleSignIn());

        authViewModel.getAuthResult().observe(this, this::onAuthResultChanged);
    }

    private void requestGoogleSignIn() {
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleCredentialResult(result);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        if (e instanceof NoCredentialException) {
                            showError(getString(R.string.error_no_google_account));
                        } else {
                            showError(e.getMessage());
                        }
                    }
                });
    }

    private void handleCredentialResult(GetCredentialResponse result) {
        Credential credential = result.getCredential();
        if (credential instanceof CustomCredential
                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            GoogleIdTokenCredential googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
            authViewModel.signInWithGoogleIdToken(googleIdTokenCredential.getIdToken());
        } else {
            showError(getString(R.string.error_google_sign_in));
        }
    }

    private void onAuthResultChanged(Resource<FirebaseUser> resource) {
        switch (resource.getStatus()) {
            case LOADING:
                setLoading(true);
                break;
            case SUCCESS:
                setLoading(false);
                navigateToMain();
                break;
            case ERROR:
                setLoading(false);
                showError(resource.getMessage());
                break;
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        signInButton.setEnabled(!loading);
    }

    private void showError(String message) {
        Snackbar.make(
                signInButton,
                message != null ? message : getString(R.string.error_google_sign_in),
                Snackbar.LENGTH_LONG
        ).show();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

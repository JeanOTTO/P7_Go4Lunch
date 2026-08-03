package com.jeanotto.go4lunch.repository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuthRepositoryTest {

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private Task<AuthResult> task;

    @Mock
    private AuthResult authResult;

    @Mock
    private FirebaseUser firebaseUser;

    @Captor
    private ArgumentCaptor<OnSuccessListener<AuthResult>> successCaptor;

    @Captor
    private ArgumentCaptor<OnFailureListener> failureCaptor;

    private AuthRepository authRepository;

    @Before
    public void setUp() {
        authRepository = new AuthRepository(firebaseAuth);
        when(firebaseAuth.signInWithCredential(any())).thenReturn(task);
        when(task.addOnSuccessListener(any())).thenReturn(task);
        when(task.addOnFailureListener(any())).thenReturn(task);
    }

    @Test
    public void signInWithGoogleIdToken_success_notifiesCallbackWithUser() {
        when(authResult.getUser()).thenReturn(firebaseUser);
        AuthRepository.AuthCallback callback = mock(AuthRepository.AuthCallback.class);

        authRepository.signInWithGoogleIdToken("id-token", callback);

        verify(task).addOnSuccessListener(successCaptor.capture());
        successCaptor.getValue().onSuccess(authResult);

        verify(callback).onSuccess(firebaseUser);
    }

    @Test
    public void signInWithGoogleIdToken_failure_notifiesCallbackWithError() {
        Exception exception = new Exception("network error");
        AuthRepository.AuthCallback callback = mock(AuthRepository.AuthCallback.class);

        authRepository.signInWithGoogleIdToken("id-token", callback);

        verify(task).addOnFailureListener(failureCaptor.capture());
        failureCaptor.getValue().onFailure(exception);

        verify(callback).onError(exception);
    }

    @Test
    public void isUserLoggedIn_returnsTrueWhenCurrentUserPresent() {
        when(firebaseAuth.getCurrentUser()).thenReturn(firebaseUser);

        assertTrue(authRepository.isUserLoggedIn());
    }

    @Test
    public void isUserLoggedIn_returnsFalseWhenCurrentUserAbsent() {
        when(firebaseAuth.getCurrentUser()).thenReturn(null);

        assertFalse(authRepository.isUserLoggedIn());
    }

    @Test
    public void signOut_delegatesToFirebaseAuth() {
        authRepository.signOut();

        verify(firebaseAuth).signOut();
    }
}

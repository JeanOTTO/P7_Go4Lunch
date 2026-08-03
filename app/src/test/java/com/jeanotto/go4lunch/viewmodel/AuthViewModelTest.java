package com.jeanotto.go4lunch.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.firebase.auth.FirebaseUser;
import com.jeanotto.go4lunch.model.Resource;
import com.jeanotto.go4lunch.repository.AuthRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AuthViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private AuthRepository authRepository;

    @Mock
    private FirebaseUser firebaseUser;

    @Captor
    private ArgumentCaptor<AuthRepository.AuthCallback> callbackCaptor;

    private AuthViewModel authViewModel;

    @Before
    public void setUp() {
        authViewModel = new AuthViewModel(authRepository);
    }

    @Test
    public void signInWithGoogleIdToken_emitsLoadingThenSuccess() {
        List<Resource<FirebaseUser>> emissions = new ArrayList<>();
        authViewModel.getAuthResult().observeForever(emissions::add);

        authViewModel.signInWithGoogleIdToken("id-token");

        verify(authRepository).signInWithGoogleIdToken(eq("id-token"), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess(firebaseUser);

        assertEquals(2, emissions.size());
        assertEquals(Resource.Status.LOADING, emissions.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, emissions.get(1).getStatus());
        assertEquals(firebaseUser, emissions.get(1).getData());
    }

    @Test
    public void signInWithGoogleIdToken_emitsLoadingThenError() {
        List<Resource<FirebaseUser>> emissions = new ArrayList<>();
        authViewModel.getAuthResult().observeForever(emissions::add);

        authViewModel.signInWithGoogleIdToken("id-token");

        verify(authRepository).signInWithGoogleIdToken(eq("id-token"), callbackCaptor.capture());
        callbackCaptor.getValue().onError(new Exception("boom"));

        assertEquals(2, emissions.size());
        assertEquals(Resource.Status.LOADING, emissions.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, emissions.get(1).getStatus());
        assertEquals("boom", emissions.get(1).getMessage());
    }

    @Test
    public void isUserLoggedIn_delegatesToRepository() {
        when(authRepository.isUserLoggedIn()).thenReturn(true);

        assertTrue(authViewModel.isUserLoggedIn());
    }
}

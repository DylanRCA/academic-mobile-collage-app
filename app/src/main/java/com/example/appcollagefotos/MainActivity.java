package com.example.appcollagefotos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.appcollagefotos.data.model.Event;
import com.example.appcollagefotos.data.model.Post;
import com.example.appcollagefotos.data.model.User;
import com.example.appcollagefotos.data.repository.FirebaseRepository;
import com.example.appcollagefotos.databinding.ActivityMainBinding;
import com.example.appcollagefotos.ui.fragment.CameraCaptureFragment;
import com.example.appcollagefotos.ui.fragment.CollageFragment;
import com.example.appcollagefotos.ui.fragment.LoginFragment;
import com.example.appcollagefotos.ui.fragment.PostReviewFragment;
import com.example.appcollagefotos.ui.fragment.QRScannerFragment;

public class MainActivity extends AppCompatActivity implements
        QRScannerFragment.QRScannerListener,
        LoginFragment.LoginListener,
        CameraCaptureFragment.CameraCaptureListener,
        PostReviewFragment.PostReviewListener {

    private ActivityMainBinding binding;
    private FirebaseRepository repository;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new FirebaseRepository();
        repository.createDemoEventsIfNotExist(null);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        binding.layoutPermissionRequest.setVisibility(View.GONE);
                        binding.fragmentContainer.setVisibility(View.VISIBLE);
                        handleInitialFlow();
                    } else {
                        binding.layoutPermissionRequest.setVisibility(View.VISIBLE);
                        binding.fragmentContainer.setVisibility(View.GONE);
                    }
                }
        );

        binding.btnGrantPermission.setOnClickListener(v -> checkAndRequestCameraPermission());

        checkAndRequestCameraPermission();
    }

    private void checkAndRequestCameraPermission() {
        boolean granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;

        if (granted) {
            binding.layoutPermissionRequest.setVisibility(View.GONE);
            binding.fragmentContainer.setVisibility(View.VISIBLE);
            if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) == null) {
                handleInitialFlow();
            }
        } else {
            binding.layoutPermissionRequest.setVisibility(View.VISIBLE);
            binding.fragmentContainer.setVisibility(View.GONE);
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void handleInitialFlow() {
        Intent intent = getIntent();
        Uri data = intent != null ? intent.getData() : null;

        if (data != null) {
            String eventId = data.getQueryParameter("eventId");
            if (eventId == null || eventId.trim().isEmpty()) {
                eventId = data.getLastPathSegment();
            }

            if (eventId != null && !eventId.trim().isEmpty()) {
                repository.getEvent(eventId.trim(), new FirebaseRepository.SimpleCallback<Event>() {
                    @Override
                    public void onSuccess(Event event) {
                        if (event != null && event.isActive()) {
                            runOnUiThread(() -> checkUserAndNavigate(event));
                        } else {
                            runOnUiThread(() -> showQRScannerFragment());
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> showQRScannerFragment());
                    }
                });
            } else {
                showQRScannerFragment();
            }
        } else {
            showQRScannerFragment();
        }
    }

    private void checkUserAndNavigate(Event event) {
        String currentUserId = repository.getCurrentUserId();
        if (currentUserId != null) {
            repository.getUserProfile(currentUserId, new FirebaseRepository.SimpleCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    runOnUiThread(() -> {
                        if (user != null && user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                            showCameraCaptureFragment(event);
                        } else {
                            showLoginFragment(event);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> showLoginFragment(event));
                }
            });
        } else {
            showLoginFragment(event);
        }
    }

    private void showQRScannerFragment() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        QRScannerFragment fragment = new QRScannerFragment();
        fragment.setListener(this);
        replaceFragment(fragment);
    }

    private void showLoginFragment(Event event) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        LoginFragment fragment = LoginFragment.newInstance(event);
        fragment.setListener(this);
        replaceFragment(fragment);
    }

    private void showCameraCaptureFragment(Event event) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        CameraCaptureFragment fragment = CameraCaptureFragment.newInstance(event);
        fragment.setListener(this);
        replaceFragment(fragment);
    }

    private void showPostReviewFragment(Event event, Bitmap bitmap) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        PostReviewFragment fragment = PostReviewFragment.newInstance(event, bitmap);
        fragment.setListener(this);
        replaceFragment(fragment);
    }

    private void showCollageFragment(Event event, Post publishedPost) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        CollageFragment fragment = CollageFragment.newInstance(event, publishedPost);
        replaceFragment(fragment);
    }

    @Override
    public void onEventLoaded(Event event) {
        checkUserAndNavigate(event);
    }

    @Override
    public void onUserRegistered(Event event, User user) {
        showCameraCaptureFragment(event);
    }

    @Override
    public void onPhotoCaptured(Bitmap bitmap, Event event) {
        showPostReviewFragment(event, bitmap);
    }

    @Override
    public void onRetry(Event event) {
        showCameraCaptureFragment(event);
    }

    @Override
    public void onPostPublished(Event event, Post publishedPost) {
        showCollageFragment(event, publishedPost);
    }

    private void replaceFragment(Fragment fragment) {
        if (isFinishing() || isDestroyed()) return;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss();
    }
}

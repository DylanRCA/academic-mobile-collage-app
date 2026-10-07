package com.example.appcollagefotos.ui.fragment;

import android.media.Image;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.appcollagefotos.data.model.Event;
import com.example.appcollagefotos.data.model.User;
import com.example.appcollagefotos.data.repository.FirebaseRepository;
import com.example.appcollagefotos.databinding.FragmentQrScannerBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScannerFragment extends Fragment {

    public interface QRScannerListener {
        void onEventLoaded(Event event);
    }

    private FragmentQrScannerBinding binding;
    private QRScannerListener listener;
    private final FirebaseRepository repository = new FirebaseRepository();
    private ExecutorService cameraExecutor;
    private boolean isProcessing = false;

    public void setListener(QRScannerListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof QRScannerListener) {
            this.listener = (QRScannerListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQrScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        updateUserSessionState();

        binding.btnLogout.setOnClickListener(v -> {
            repository.signOut();
            if (binding != null) {
                binding.btnLogout.setVisibility(View.GONE);
                binding.tvUserPhone.setText("👤 Sin sesión activa");
                binding.cardUserBadge.setVisibility(View.VISIBLE);
            }
            if (getContext() != null) {
                Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
            }
        });

        startCamera();
    }

    private void updateUserSessionState() {
        if (binding == null) return;
        String currentUserId = repository.getCurrentUserId();
        if (currentUserId != null) {
            repository.getUserProfile(currentUserId, new FirebaseRepository.SimpleCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding == null) return;
                            if (user != null && (!TextUtils.isEmpty(user.getPhoneNumber()) || !TextUtils.isEmpty(user.getEmail()))) {
                                String phoneOrEmail = !TextUtils.isEmpty(user.getPhoneNumber()) ?
                                        "📱 " + user.getPhoneNumber() : "✉️ " + user.getEmail();
                                binding.tvUserPhone.setText(phoneOrEmail);
                                binding.cardUserBadge.setVisibility(View.VISIBLE);
                                binding.btnLogout.setVisibility(View.VISIBLE);
                            } else {
                                binding.tvUserPhone.setText("👤 Sin sesión activa");
                                binding.cardUserBadge.setVisibility(View.VISIBLE);
                                binding.btnLogout.setVisibility(View.GONE);
                            }
                        });
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null) {
                                binding.tvUserPhone.setText("👤 Sin sesión activa");
                                binding.cardUserBadge.setVisibility(View.VISIBLE);
                                binding.btnLogout.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            });
        } else {
            binding.tvUserPhone.setText("👤 Sin sesión activa");
            binding.cardUserBadge.setVisibility(View.VISIBLE);
            binding.btnLogout.setVisibility(View.GONE);
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera() {
        if (getContext() == null) return;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                if (binding == null || !isAdded()) return;
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                BarcodeScanner scanner = BarcodeScanning.getClient();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null && !isProcessing) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        scanner.process(image)
                                .addOnSuccessListener(barcodes -> {
                                    for (Barcode barcode : barcodes) {
                                        String rawValue = barcode.getRawValue();
                                        if (!TextUtils.isEmpty(rawValue) && !isProcessing) {
                                            isProcessing = true;
                                            String eventId = parseEventId(rawValue);
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(() -> {
                                                    showLoading(true);
                                                    processScannedEvent(eventId);
                                                });
                                            }
                                            break;
                                        }
                                    }
                                })
                                .addOnCompleteListener(task -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );
            } catch (Exception e) {
                showError("Camera error: " + e.getLocalizedMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void processScannedEvent(String eventId) {
        repository.signInAnonymouslyIfNeeded(new FirebaseRepository.SimpleCallback<String>() {
            @Override
            public void onSuccess(String userId) {
                repository.getEvent(eventId, new FirebaseRepository.SimpleCallback<Event>() {
                    @Override
                    public void onSuccess(Event event) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (event == null) {
                                    showError("Event not found");
                                    showLoading(false);
                                    isProcessing = false;
                                } else if (!event.isActive()) {
                                    showError("This event is no longer active");
                                    showLoading(false);
                                    isProcessing = false;
                                } else {
                                    showLoading(false);
                                    if (listener != null) {
                                        listener.onEventLoaded(event);
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showError("Error fetching event: " + e.getLocalizedMessage());
                                showLoading(false);
                                isProcessing = false;
                            });
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showError("Authentication error: " + e.getLocalizedMessage());
                        showLoading(false);
                        isProcessing = false;
                    });
                }
            }
        });
    }

    private String parseEventId(String rawValue) {
        try {
            String trimmed = rawValue.trim();
            if (trimmed.startsWith("{")) {
                JSONObject json = new JSONObject(trimmed);
                return json.optString("eventId", trimmed);
            } else {
                return trimmed;
            }
        } catch (Exception e) {
            return rawValue.trim();
        }
    }

    private void showLoading(boolean isLoading) {
        if (binding != null) {
            binding.progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        if (binding != null) {
            binding.tvErrorMessage.setText(message);
            binding.tvErrorMessage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView() ;
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        binding = null;
    }
}

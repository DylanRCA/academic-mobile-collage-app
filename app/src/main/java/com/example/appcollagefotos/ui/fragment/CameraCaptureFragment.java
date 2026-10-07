package com.example.appcollagefotos.ui.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.appcollagefotos.data.model.Event;
import com.example.appcollagefotos.databinding.FragmentCameraCaptureBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraCaptureFragment extends Fragment {

    private static final String ARG_EVENT = "arg_event";

    public interface CameraCaptureListener {
        void onPhotoCaptured(Bitmap bitmap, Event event);
    }

    private FragmentCameraCaptureBinding binding;
    private Event event;
    private CameraCaptureListener listener;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private ActivityResultLauncher<String> galleryLauncher;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean isCapturing = false;

    public static CameraCaptureFragment newInstance(Event event) {
        CameraCaptureFragment fragment = new CameraCaptureFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(CameraCaptureListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof CameraCaptureListener) {
            this.listener = (CameraCaptureListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
        }

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        processGalleryUri(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCameraCaptureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (event != null && !TextUtils.isEmpty(event.getName())) {
            binding.tvEventName.setText(event.getName());
            binding.tvEventName.setVisibility(View.VISIBLE);
        } else {
            binding.tvEventName.setVisibility(View.GONE);
        }

        startCamera();

        binding.btnShutter.setOnClickListener(v -> {
            if (imageCapture == null || isCapturing) return;
            isCapturing = true;
            showCapturing(true);
            capturePhoto(imageCapture);
        });

        binding.btnSwitchCamera.setOnClickListener(v -> {
            if (isCapturing) return;
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK) ?
                    CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            startCamera();
        });

        binding.btnGallery.setOnClickListener(v -> {
            if (isCapturing) return;
            galleryLauncher.launch("image/*");
        });
    }

    private void processGalleryUri(Uri uri) {
        if (cameraExecutor == null || getContext() == null) return;
        isCapturing = true;
        showCapturing(true);

        android.content.ContentResolver resolver = requireContext().getContentResolver();
        cameraExecutor.execute(() -> {
            Bitmap bitmap = null;
            try {
                InputStream inputBounds = resolver.openInputStream(uri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputBounds, null, options);
                if (inputBounds != null) inputBounds.close();

                int targetWidth = 1024;
                int targetHeight = 768;
                int sampleSize = 1;
                while (options.outWidth / sampleSize > targetWidth || options.outHeight / sampleSize > targetHeight) {
                    sampleSize *= 2;
                }

                BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
                decodeOptions.inSampleSize = sampleSize;

                InputStream inputBitmap = resolver.openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(inputBitmap, null, decodeOptions);
                if (inputBitmap != null) inputBitmap.close();
            } catch (Exception e) {
                bitmap = null;
            }

            Bitmap finalBitmap = bitmap;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null || !isAdded()) return;
                    isCapturing = false;
                    showCapturing(false);
                    if (finalBitmap != null && event != null) {
                        if (listener != null) {
                            listener.onPhotoCaptured(finalBitmap, event);
                        }
                    } else {
                        showError("Failed to process selected gallery image");
                    }
                });
            }
        });
    }

    private void startCamera() {
        if (getContext() == null) return;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                if (binding == null || !isAdded()) return;
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview,
                        imageCapture
                );
            } catch (Exception e) {
                showError("Camera error: " + e.getLocalizedMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void capturePhoto(ImageCapture capture) {
        if (cameraExecutor == null) return;
        capture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                Bitmap bitmap = imageProxyToBitmap(imageProxy);
                imageProxy.close();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding == null || !isAdded()) return;
                        isCapturing = false;
                        showCapturing(false);
                        if (bitmap != null && event != null) {
                            if (listener != null) {
                                listener.onPhotoCaptured(bitmap, event);
                            }
                        } else {
                            showError("Failed to process captured image (event=" + (event != null ? event.getId() : "null") + ")");
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding == null || !isAdded()) return;
                        isCapturing = false;
                        showCapturing(false);
                        showError(exception.getMessage() != null ? exception.getMessage() : "Capture error");
                    });
                }
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

            int targetWidth = 1024;
            int targetHeight = 768;
            int sampleSize = 1;
            while (options.outWidth / sampleSize > targetWidth || options.outHeight / sampleSize > targetHeight) {
                sampleSize *= 2;
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;

            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, decodeOptions);
            if (bitmap == null) return null;

            int rotation = image.getImageInfo().getRotationDegrees();
            if (rotation != 0 || lensFacing == CameraSelector.LENS_FACING_FRONT) {
                Matrix matrix = new Matrix();
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    matrix.postScale(-1f, 1f, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
                }
                if (rotation != 0) {
                    matrix.postRotate((float) rotation);
                }
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            } else {
                return bitmap;
            }
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    private void showCapturing(boolean isCapturing) {
        if (binding != null) {
            binding.progressBarCapturing.setVisibility(isCapturing ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        if (binding != null) {
            binding.tvCaptureError.setText(message);
            binding.tvCaptureError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        binding = null;
    }
}

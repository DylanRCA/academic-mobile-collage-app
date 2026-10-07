package com.example.appcollagefotos.ui.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appcollagefotos.data.model.Event;
import com.example.appcollagefotos.data.model.Post;
import com.example.appcollagefotos.data.repository.FirebaseRepository;
import com.example.appcollagefotos.databinding.FragmentPostReviewBinding;
import com.example.appcollagefotos.util.ImageProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostReviewFragment extends Fragment {

    private static final String ARG_EVENT = "arg_event";
    private static Bitmap pendingBitmap;

    public interface PostReviewListener {
        void onRetry(Event event);
        void onPostPublished(Event event, Post publishedPost);
    }

    private FragmentPostReviewBinding binding;
    private Event event;
    private Bitmap capturedBitmap;
    private PostReviewListener listener;
    private final FirebaseRepository repository = new FirebaseRepository();
    private ExecutorService bgExecutor;

    public static PostReviewFragment newInstance(Event event, Bitmap bitmap) {
        PostReviewFragment fragment = new PostReviewFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        fragment.setArguments(args);
        pendingBitmap = bitmap;
        fragment.capturedBitmap = bitmap;
        return fragment;
    }

    public void setListener(PostReviewListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof PostReviewListener) {
            this.listener = (PostReviewListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
        }
        if (capturedBitmap == null && pendingBitmap != null) {
            capturedBitmap = pendingBitmap;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPostReviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bgExecutor = Executors.newSingleThreadExecutor();

        if (capturedBitmap != null) {
            binding.ivCapturedPhoto.setImageBitmap(capturedBitmap);
        }

        if (event != null && !TextUtils.isEmpty(event.getLogoBase64())) {
            Bitmap logoBitmap = ImageProcessor.decodeBase64ToBitmap(event.getLogoBase64());
            if (logoBitmap != null) {
                binding.ivEventLogo.setImageBitmap(logoBitmap);
                binding.ivEventLogo.setVisibility(View.VISIBLE);
            }
        }

        binding.btnRetry.setOnClickListener(v -> {
            if (event != null && listener != null) {
                listener.onRetry(event);
            }
        });

        binding.btnSend.setOnClickListener(v -> {
            if (capturedBitmap == null || event == null) return;

            String message = binding.etDedication.getText() != null ?
                    binding.etDedication.getText().toString().trim() : null;
            if (TextUtils.isEmpty(message)) {
                message = null;
            }

            setPublishingState(true);

            String finalMessage = message;
            bgExecutor.execute(() -> {
                String base64Image = ImageProcessor.compressAndEncodeBase64(capturedBitmap, 65, 1024, 768);
                String userId = repository.getCurrentUserId() != null ? repository.getCurrentUserId() : "";

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> repository.createPost(
                            event.getId(),
                            userId,
                            base64Image,
                            finalMessage,
                            null,
                            new FirebaseRepository.SimpleCallback<String>() {
                                @Override
                                public void onSuccess(String postId) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            setPublishingState(false);
                                            pendingBitmap = null;
                                            Post newPost = new Post(postId, event.getId(), userId, base64Image, finalMessage, System.currentTimeMillis(), null);
                                            if (listener != null) {
                                                listener.onPostPublished(event, newPost);
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            setPublishingState(false);
                                            showError(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Failed to publish post");
                                        });
                                    }
                                }
                            }
                    ));
                }
            });
        });
    }

    private void setPublishingState(boolean isSending) {
        if (binding != null) {
            binding.btnSend.setEnabled(!isSending);
            binding.btnRetry.setEnabled(!isSending);
            binding.progressBarSending.setVisibility(isSending ? View.VISIBLE : View.GONE);
            binding.tvPublishError.setVisibility(View.GONE);
        }
    }

    private void showError(String msg) {
        if (binding != null) {
            binding.tvPublishError.setText(msg);
            binding.tvPublishError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bgExecutor != null) {
            bgExecutor.shutdown();
        }
        binding = null;
    }
}

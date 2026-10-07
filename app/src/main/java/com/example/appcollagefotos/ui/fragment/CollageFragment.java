package com.example.appcollagefotos.ui.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.appcollagefotos.data.model.Event;
import com.example.appcollagefotos.data.model.Post;
import com.example.appcollagefotos.data.repository.FirebaseRepository;
import com.example.appcollagefotos.databinding.FragmentCollageBinding;
import com.example.appcollagefotos.ui.adapter.CollageAdapter;
import com.example.appcollagefotos.util.ImageProcessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollageFragment extends Fragment {

    private static final String ARG_EVENT = "arg_event";
    private static final String ARG_PUBLISHED_POST = "arg_published_post";
    private static final int MAX_GRID_POSTS = 12;

    private FragmentCollageBinding binding;
    private Event event;
    private Post publishedPost;
    private final FirebaseRepository repository = new FirebaseRepository();
    private final CollageAdapter adapter = new CollageAdapter();

    private final List<Post> gridPosts = new ArrayList<>();
    private final Set<String> knownPostIds = new HashSet<>();
    private boolean isInitialLoad = true;
    private FirebaseRepository.ListenerRegistration listenerRegistration;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static CollageFragment newInstance(Event event, Post publishedPost) {
        CollageFragment fragment = new CollageFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        if (publishedPost != null) {
            args.putSerializable(ARG_PUBLISHED_POST, publishedPost);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static CollageFragment newInstance(Event event) {
        return newInstance(event, null);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
            publishedPost = (Post) getArguments().getSerializable(ARG_PUBLISHED_POST);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCollageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (event == null) return;

        if (event.getName() != null) {
            binding.tvHeaderEventName.setText(event.getName());
        }

        if (!TextUtils.isEmpty(event.getLogoBase64())) {
            Bitmap logoBitmap = ImageProcessor.decodeBase64ToBitmap(event.getLogoBase64());
            if (logoBitmap != null) {
                binding.ivHeaderLogo.setImageBitmap(logoBitmap);
                binding.ivHeaderLogo.setVisibility(View.VISIBLE);
            }
        }

        binding.rvCollageGrid.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        binding.rvCollageGrid.setAdapter(adapter);

        adapter.setOnPhotoClickListener(this::openPhotoViewerModal);

        binding.layoutPhotoViewerModal.setOnClickListener(v -> dismissPhotoViewerModal());
        binding.containerViewerCard.setOnClickListener(v -> {}); // Prevent backdrop dismissal on card click

        listenerRegistration = repository.listenToPosts(event.getId(), new FirebaseRepository.DataListener<List<Post>>() {
            @Override
            public void onDataChanged(List<Post> latestPosts) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> handlePostsUpdate(latestPosts));
                }
            }

            @Override
            public void onError(Exception e) {
                // Ignore or log error
            }
        });
    }

    private void openPhotoViewerModal(Post post) {
        if (binding == null || post == null) return;

        if (!TextUtils.isEmpty(post.getImageBase64())) {
            Bitmap bitmap = ImageProcessor.decodeBase64ToBitmap(post.getImageBase64());
            if (bitmap != null) {
                binding.ivModalPhoto.setImageBitmap(bitmap);
            }
        }

        if (!TextUtils.isEmpty(post.getMessage())) {
            binding.tvModalMessage.setText(post.getMessage());
            binding.cardModalMessage.setVisibility(View.VISIBLE);
        } else {
            binding.cardModalMessage.setVisibility(View.GONE);
        }

        binding.layoutPhotoViewerModal.setVisibility(View.VISIBLE);
        binding.layoutPhotoViewerModal.setAlpha(0f);
        binding.layoutPhotoViewerModal.animate()
                .alpha(1f)
                .setDuration(250)
                .start();
    }

    private void dismissPhotoViewerModal() {
        if (binding == null || binding.layoutPhotoViewerModal.getVisibility() != View.VISIBLE) return;

        binding.layoutPhotoViewerModal.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (binding != null) {
                        binding.layoutPhotoViewerModal.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void handlePostsUpdate(List<Post> latestPosts) {
        if (latestPosts == null || latestPosts.isEmpty()) return;

        if (isInitialLoad) {
            isInitialLoad = false;
            gridPosts.clear();
            knownPostIds.clear();

            for (Post p : latestPosts) {
                if (p.getId() != null) {
                    knownPostIds.add(p.getId());
                }
            }

            if (publishedPost != null) {
                // Initial grid populated without publishedPost so it can animate in!
                for (Post p : latestPosts) {
                    if (publishedPost.getId() != null && !publishedPost.getId().equals(p.getId())) {
                        gridPosts.add(p);
                        if (gridPosts.size() == MAX_GRID_POSTS - 1) break;
                    }
                }
                adapter.submitList(new ArrayList<>(gridPosts));

                Post tempPost = publishedPost;
                publishedPost = null;
                animateSpotlightToGridSlot(tempPost);
            } else {
                for (int i = 0; i < Math.min(latestPosts.size(), MAX_GRID_POSTS); i++) {
                    gridPosts.add(latestPosts.get(i));
                }
                adapter.submitList(new ArrayList<>(gridPosts));
            }
            return;
        }

        List<Post> newPosts = new ArrayList<>();
        for (Post post : latestPosts) {
            if (!TextUtils.isEmpty(post.getId()) && !knownPostIds.contains(post.getId())) {
                newPosts.add(post);
            }
        }

        if (!newPosts.isEmpty()) {
            Post newest = newPosts.get(0);
            for (Post p : latestPosts) {
                if (p.getId() != null) {
                    knownPostIds.add(p.getId());
                }
            }

            animateSpotlightToGridSlot(newest);
        }
    }

    private void animateSpotlightToGridSlot(Post post) {
        if (binding == null) return;

        if (!TextUtils.isEmpty(post.getImageBase64())) {
            Bitmap bitmap = ImageProcessor.decodeBase64ToBitmap(post.getImageBase64());
            if (bitmap != null) {
                binding.ivHeroPhoto.setImageBitmap(bitmap);
            }
        }

        if (!TextUtils.isEmpty(post.getMessage())) {
            binding.tvHeroMessage.setText(post.getMessage());
            binding.tvHeroMessage.setVisibility(View.VISIBLE);
        } else {
            binding.tvHeroMessage.setVisibility(View.GONE);
        }

        // 1. Show spotlight centered immediately
        binding.layoutHeroSpotlight.setVisibility(View.VISIBLE);
        binding.heroCard.setPivotX(binding.heroCard.getWidth() / 2f);
        binding.heroCard.setPivotY(binding.heroCard.getHeight() / 2f);
        binding.heroCard.setScaleX(0.85f);
        binding.heroCard.setScaleY(0.85f);
        binding.heroCard.setTranslationX(0f);
        binding.heroCard.setTranslationY(0f);
        binding.heroCard.setAlpha(0f);

        binding.heroCard.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(400)
                .withEndAction(() -> {
                    // Preview delay for 1.8 seconds before translating
                    mainHandler.postDelayed(() -> performTranslateAndScaleAnimation(post), 1800L);
                })
                .start();
    }

    private void performTranslateAndScaleAnimation(Post post) {
        if (binding == null) return;

        // Set top-left pivot point so scaling aligns exactly with cell's top-left corner
        binding.heroCard.setPivotX(0f);
        binding.heroCard.setPivotY(0f);

        View targetCell = binding.rvCollageGrid.getLayoutManager() != null ?
                binding.rvCollageGrid.getLayoutManager().findViewByPosition(0) : null;

        int[] heroLoc = new int[2];
        binding.heroCard.getLocationOnScreen(heroLoc);

        int[] targetLoc = new int[2];
        float targetWidth = binding.heroCard.getWidth() * 0.25f;
        float targetHeight = binding.heroCard.getHeight() * 0.25f;

        if (targetCell != null) {
            targetCell.getLocationOnScreen(targetLoc);
            targetWidth = targetCell.getWidth();
            targetHeight = targetCell.getHeight();
        } else {
            binding.rvCollageGrid.getLocationOnScreen(targetLoc);
        }

        float scaleX = targetWidth / binding.heroCard.getWidth();
        float scaleY = targetHeight / binding.heroCard.getHeight();
        float translationX = targetLoc[0] - heroLoc[0];
        float translationY = targetLoc[1] - heroLoc[1];

        binding.heroCard.animate()
                .translationX(translationX)
                .translationY(translationY)
                .scaleX(scaleX)
                .scaleY(scaleY)
                .alpha(0.85f)
                .setDuration(900)
                .withEndAction(() -> {
                    if (binding == null) return;

                    binding.layoutHeroSpotlight.setVisibility(View.GONE);
                    binding.heroCard.setTranslationX(0f);
                    binding.heroCard.setTranslationY(0f);
                    binding.heroCard.setScaleX(1f);
                    binding.heroCard.setScaleY(1f);
                    binding.heroCard.setAlpha(1f);
                    binding.heroCard.setPivotX(binding.heroCard.getWidth() / 2f);
                    binding.heroCard.setPivotY(binding.heroCard.getHeight() / 2f);

                    boolean alreadyInGrid = false;
                    for (Post p : gridPosts) {
                        if (p.getId() != null && p.getId().equals(post.getId())) {
                            alreadyInGrid = true;
                            break;
                        }
                    }

                    if (!alreadyInGrid) {
                        gridPosts.add(0, post);
                        if (gridPosts.size() > MAX_GRID_POSTS) {
                            gridPosts.remove(gridPosts.size() - 1);
                        }
                        adapter.submitList(new ArrayList<>(gridPosts));
                    }

                    // Trigger 3-second gold glow border on slot [0,0]
                    adapter.setHighlightedPostId(post.getId());
                    mainHandler.postDelayed(() -> {
                        if (binding != null) {
                            adapter.setHighlightedPostId(null);
                        }
                    }, 3000L);
                })
                .start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}

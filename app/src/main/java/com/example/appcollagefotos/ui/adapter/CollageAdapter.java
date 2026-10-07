package com.example.appcollagefotos.ui.adapter;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appcollagefotos.data.model.Post;
import com.example.appcollagefotos.databinding.ItemCollagePostBinding;
import com.example.appcollagefotos.util.ImageProcessor;

import java.util.Objects;

public class CollageAdapter extends ListAdapter<Post, CollageAdapter.PostViewHolder> {

    public interface OnPhotoClickListener {
        void onPhotoClick(Post post);
    }

    private String highlightedPostId = null;
    private OnPhotoClickListener onPhotoClickListener;

    public CollageAdapter() {
        super(POST_DIFF_CALLBACK);
    }

    public void setHighlightedPostId(String postId) {
        this.highlightedPostId = postId;
        notifyDataSetChanged();
    }

    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.onPhotoClickListener = listener;
    }

    public void flipCardAtPosition(RecyclerView recyclerView, int position, Post newPost) {
        if (recyclerView == null || position < 0 || position >= getItemCount()) return;

        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        if (holder instanceof PostViewHolder) {
            PostViewHolder postViewHolder = (PostViewHolder) holder;
            View card = postViewHolder.binding.cardPost;

            card.animate()
                    .rotationY(90f)
                    .setDuration(250)
                    .withEndAction(() -> {
                        postViewHolder.bind(newPost, highlightedPostId, onPhotoClickListener);
                        card.setRotationY(-90f);
                        card.animate()
                                .rotationY(0f)
                                .setDuration(250)
                                .start();
                    })
                    .start();
        }
    }

    private static final DiffUtil.ItemCallback<Post> POST_DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId()) &&
                    Objects.equals(oldItem.getEventId(), newItem.getEventId()) &&
                    Objects.equals(oldItem.getImageBase64(), newItem.getImageBase64()) &&
                    Objects.equals(oldItem.getMessage(), newItem.getMessage());
        }
    };

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCollagePostBinding binding = ItemCollagePostBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(getItem(position), highlightedPostId, onPhotoClickListener);
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        public final ItemCollagePostBinding binding;

        public PostViewHolder(@NonNull ItemCollagePostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Post post, String highlightedPostId, OnPhotoClickListener listener) {
            if (post.getImageBase64() != null) {
                Bitmap bitmap = ImageProcessor.decodeBase64ToBitmap(post.getImageBase64());
                if (bitmap != null) {
                    binding.ivPostImage.setImageBitmap(bitmap);
                } else {
                    binding.ivPostImage.setImageDrawable(null);
                }
            } else {
                binding.ivPostImage.setImageDrawable(null);
            }

            if (!TextUtils.isEmpty(post.getMessage())) {
                binding.ivMessageIndicator.setVisibility(View.VISIBLE);
            } else {
                binding.ivMessageIndicator.setVisibility(View.GONE);
            }

            if (post.getId() != null && post.getId().equals(highlightedPostId)) {
                int strokePx = (int) (3 * binding.getRoot().getContext().getResources().getDisplayMetrics().density);
                binding.cardPost.setStrokeWidth(strokePx);
                binding.cardPost.setStrokeColor(Color.parseColor("#FFD700")); // Gold
            } else {
                binding.cardPost.setStrokeWidth(0);
            }

            binding.cardPost.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPhotoClick(post);
                }
            });
        }
    }
}

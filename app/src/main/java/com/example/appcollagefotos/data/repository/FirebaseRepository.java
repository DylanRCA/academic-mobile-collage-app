package com.example.appcollagefotos.data.repository;

import androidx.annotation.NonNull;

import com.example.appcollagefotos.data.model.Event;
import com.example.appcollagefotos.data.model.Post;
import com.example.appcollagefotos.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {

    public interface SimpleCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public interface DataListener<T> {
        void onDataChanged(T data);
        void onError(Exception e);
    }

    public interface ListenerRegistration {
        void remove();
    }

    private final FirebaseAuth auth;
    private final DatabaseReference database;

    public FirebaseRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.database = FirebaseDatabase.getInstance().getReference();
    }

    public FirebaseRepository(FirebaseAuth auth, DatabaseReference database) {
        this.auth = auth;
        this.database = database;
    }

    public void signInAnonymouslyIfNeeded(SimpleCallback<String> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            if (callback != null) {
                callback.onSuccess(currentUser.getUid());
            }
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        if (callback != null) {
                            callback.onSuccess(user.getUid());
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(new IllegalStateException("Firebase Anonymous Auth returned null user"));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void createDemoEventsIfNotExist(SimpleCallback<List<String>> callback) {
        List<Event> demoEvents = new ArrayList<>();
        demoEvents.add(new Event("boda_demo", "Boda Mateo & Lucía", "boda", "", "BODA123", true, null));
        demoEvents.add(new Event("graduacion_2026", "Graduación Medicina 2026", "graduacion", "", "GRAD2026", true, null));

        signInAnonymouslyIfNeeded(new SimpleCallback<String>() {
            @Override
            public void onSuccess(String userId) {
                List<String> createdIds = new ArrayList<>();
                int totalEvents = demoEvents.size();
                final int[] processedCount = {0};

                for (Event event : demoEvents) {
                    DatabaseReference eventRef = database.child("events").child(event.getId());
                    eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) {
                                Map<String, Object> eventMap = new HashMap<>();
                                eventMap.put("id", event.getId());
                                eventMap.put("name", event.getName());
                                eventMap.put("eventType", event.getEventType());
                                eventMap.put("eventCode", event.getEventCode());
                                eventMap.put("active", event.isActive());
                                eventMap.put("logoBase64", event.getLogoBase64());
                                eventMap.put("createdAt", ServerValue.TIMESTAMP);

                                eventRef.setValue(eventMap);
                            }
                            createdIds.add(event.getId());
                            processedCount[0]++;
                            if (processedCount[0] == totalEvents && callback != null) {
                                callback.onSuccess(createdIds);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            processedCount[0]++;
                            if (processedCount[0] == totalEvents && callback != null) {
                                callback.onSuccess(createdIds);
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public void getEvent(String eventId, SimpleCallback<Event> callback) {
        if (eventId == null || eventId.trim().isEmpty() ||
                eventId.contains(".") || eventId.contains("#") ||
                eventId.contains("$") || eventId.contains("[") ||
                eventId.contains("]") || eventId.contains("/")) {
            if (callback != null) {
                callback.onSuccess(null);
            }
            return;
        }

        database.child("events").child(eventId.trim()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    return;
                }
                Event event = snapshot.getValue(Event.class);
                if (event != null) {
                    event.setId(snapshot.getKey());
                }
                if (callback != null) {
                    callback.onSuccess(event);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }

    public ListenerRegistration listenToPosts(String eventId, DataListener<List<Post>> listener) {
        Query query = database.child("posts").orderByChild("eventId").equalTo(eventId);

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Post> posts = new ArrayList<>();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Post post = doc.getValue(Post.class);
                    if (post != null) {
                        post.setId(doc.getKey());
                        posts.add(post);
                    }
                }

                // Sort posts descending by createdAt
                Collections.sort(posts, (p1, p2) -> {
                    long t1 = parseTimestamp(p1.getCreatedAt());
                    long t2 = parseTimestamp(p2.getCreatedAt());
                    return Long.compare(t2, t1);
                });

                if (listener != null) {
                    listener.onDataChanged(posts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onError(error.toException());
                }
            }
        };

        query.addValueEventListener(valueEventListener);

        return () -> query.removeEventListener(valueEventListener);
    }

    private long parseTimestamp(Object createdAt) {
        if (createdAt instanceof Long) {
            return (Long) createdAt;
        } else if (createdAt instanceof Number) {
            return ((Number) createdAt).longValue();
        }
        return 0L;
    }

    public void createPost(
            String eventId,
            String userId,
            String imageBase64,
            String message,
            String deviceInfo,
            SimpleCallback<String> callback
    ) {
        DatabaseReference newPostRef = database.child("posts").push();
        String postId = newPostRef.getKey();

        Map<String, Object> postMap = new HashMap<>();
        postMap.put("id", postId);
        postMap.put("eventId", eventId);
        postMap.put("userId", userId);
        postMap.put("imageBase64", imageBase64);
        postMap.put("message", message);
        postMap.put("createdAt", ServerValue.TIMESTAMP);
        postMap.put("deviceInfo", deviceInfo);

        newPostRef.setValue(postMap)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(postId);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    public void saveUserProfile(String userId, String phoneNumber, String email, SimpleCallback<Void> callback) {
        DatabaseReference userRef = database.child("users").child(userId);
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", userId);
        userMap.put("phoneNumber", phoneNumber);
        userMap.put("email", email);
        userMap.put("createdAt", ServerValue.TIMESTAMP);

        userRef.setValue(userMap)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    public void getUserProfile(String userId, SimpleCallback<User> callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) callback.onSuccess(null);
            return;
        }
        database.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    return;
                }
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    user.setId(snapshot.getKey());
                }
                if (callback != null) {
                    callback.onSuccess(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }

    public void findUserByPhoneNumber(String phoneNumber, SimpleCallback<User> callback) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            if (callback != null) callback.onSuccess(null);
            return;
        }

        database.child("users").orderByChild("phoneNumber").equalTo(phoneNumber.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                            return;
                        }
                        User existingUser = null;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            existingUser = child.getValue(User.class);
                            if (existingUser != null) {
                                existingUser.setId(child.getKey());
                                break;
                            }
                        }
                        if (callback != null) {
                            callback.onSuccess(existingUser);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (callback != null) {
                            callback.onError(error.toException());
                        }
                    }
                });
    }

    public void signOut() {
        auth.signOut();
    }
}

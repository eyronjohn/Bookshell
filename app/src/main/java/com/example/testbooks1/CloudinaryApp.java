package com.example.testbooks1;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cloudinary.android.MediaManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryApp extends Application {

    @Nullable
    private Activity currentActivity;

    @Nullable
    private DatabaseReference statsRef;
    @Nullable
    private ValueEventListener statsListener;
    @Nullable
    private String statsUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dr5niyt00");
        MediaManager.init(this, config);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                currentActivity = activity;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                if (currentActivity == activity) {
                    currentActivity = null;
                }
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            detachStatsListener();
            if (user != null) {
                attachStatsListener(user.getUid());
            }
        });
    }

    private void attachStatsListener(@NonNull String uid) {
        if (uid.equals(statsUserId) && statsRef != null && statsListener != null) {
            return;
        }
        detachStatsListener();
        statsUserId = uid;
        statsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("stats");
        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                BadgeMilestoneHelper.processStatsSnapshot(null, CloudinaryApp.this, uid, snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        statsRef.addValueEventListener(statsListener);
    }

    private void detachStatsListener() {
        if (statsRef != null && statsListener != null) {
            statsRef.removeEventListener(statsListener);
        }
        statsRef = null;
        statsListener = null;
        statsUserId = null;
    }
}

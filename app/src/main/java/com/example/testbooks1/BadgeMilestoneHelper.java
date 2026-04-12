package com.example.testbooks1;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public final class BadgeMilestoneHelper {

    private static final String PREFS = "badge_milestones";
    private static final String KEY_BITS_PREFIX = "bits_";
    private static final String KEY_TIER_PREFIX = "tier_";

    private BadgeMilestoneHelper() {
    }

    public static void processStatsSnapshot(@Nullable Activity activity,
                                            @NonNull Context appContext,
                                            @NonNull String uid,
                                            @Nullable DataSnapshot statsSnapshot) {
        processStatsSnapshot(activity, appContext, uid, statsSnapshot, null, false, 0, 0);
    }

    public static void runAfterStatsCelebrations(@Nullable Activity activity,
                                                 @NonNull Context appContext,
                                                 @NonNull String uid,
                                                 @Nullable DataSnapshot statsSnapshot,
                                                 @NonNull Runnable onComplete,
                                                 int baselineBits,
                                                 int baselineTier) {
        processStatsSnapshot(activity, appContext, uid, statsSnapshot, onComplete, true, baselineBits, baselineTier);
    }

    public static int getStoredBadgeBits(@NonNull Context appContext, @NonNull String uid) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_BITS_PREFIX + uid, 0);
    }

    public static int getStoredBadgeTier(@NonNull Context appContext, @NonNull String uid) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_TIER_PREFIX + uid, 0);
    }

    private static void processStatsSnapshot(@Nullable Activity activity,
                                            @NonNull Context appContext,
                                            @NonNull String uid,
                                            @Nullable DataSnapshot statsSnapshot,
                                            @Nullable Runnable onComplete,
                                            boolean useCapturedBaseline,
                                            int capturedBaselineBits,
                                            int capturedBaselineTier) {
        long completed = 0;
        long reviews = 0;
        long readingLists = 0;
        if (statsSnapshot != null && statsSnapshot.exists()) {
            completed = BadgeRules.readStatLong(statsSnapshot, "completed");
            reviews = BadgeRules.readStatLong(statsSnapshot, "reviews");
            readingLists = BadgeRules.readStatLong(statsSnapshot, "readingLists");
        }

        int unlockedCount = BadgeRules.countUnlocked(completed, reviews, readingLists);
        int newTier = BadgeRules.collectorLevelTier(unlockedCount);
        int newBits = badgeMaskFromStats(completed, reviews, readingLists);

        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String keyBits = KEY_BITS_PREFIX + uid;
        String keyTier = KEY_TIER_PREFIX + uid;

        int oldBits = useCapturedBaseline ? capturedBaselineBits : prefs.getInt(keyBits, 0);
        int oldTier = useCapturedBaseline ? capturedBaselineTier : prefs.getInt(keyTier, 0);

        if (oldBits == newBits && oldTier == newTier) {
            completeDeferringToPostedUi(onComplete);
            return;
        }

        List<Integer> newBadgeIndices = new ArrayList<>();
        for (int i = 0; i < BadgeRules.TOTAL_BADGES; i++) {
            int bit = 1 << i;
            if ((newBits & bit) != 0 && (oldBits & bit) == 0) {
                newBadgeIndices.add(i);
            }
        }

        boolean levelUp = newTier > oldTier;

        prefs.edit().putInt(keyBits, newBits).putInt(keyTier, newTier).apply();

        if (newBadgeIndices.isEmpty() && !levelUp) {
            complete(onComplete);
            return;
        }

        if (activity == null || activity.isFinishing()) {
            complete(onComplete);
            return;
        }

        Handler main = new Handler(Looper.getMainLooper());
        main.post(() -> {
            if (activity.isFinishing()) {
                complete(onComplete);
                return;
            }
            runCelebrationChain(activity, newBadgeIndices, levelUp, oldTier, newTier, onComplete);
        });
    }

    private static void complete(@Nullable Runnable onComplete) {
        if (onComplete == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onComplete.run();
        } else {
            new Handler(Looper.getMainLooper()).post(onComplete);
        }
    }

    private static void completeDeferringToPostedUi(@Nullable Runnable onComplete) {
        if (onComplete == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(onComplete);
    }

    private static int badgeMaskFromStats(long completed, long reviews, long readingLists) {
        int mask = 0;
        if (completed >= 1) {
            mask |= 1;
        }
        if (completed >= 10) {
            mask |= 2;
        }
        if (completed >= 50) {
            mask |= 4;
        }
        if (completed >= 100) {
            mask |= 8;
        }
        if (reviews >= 1) {
            mask |= 16;
        }
        if (readingLists >= 1) {
            mask |= 32;
        }
        return mask;
    }

    private static void runCelebrationChain(Activity activity,
                                            List<Integer> badgeIndices,
                                            boolean levelUp,
                                            int oldTier,
                                            int newTier,
                                            @Nullable Runnable onComplete) {
        Runnable afterBadges = () -> {
            if (levelUp) {
                showLevelUpDialog(activity, oldTier, newTier, onComplete);
            } else {
                complete(onComplete);
            }
        };

        if (badgeIndices.isEmpty()) {
            afterBadges.run();
            return;
        }

        showBadgeChain(activity, badgeIndices, 0, afterBadges);
    }

    private static void showBadgeChain(Activity activity, List<Integer> indices, int index, Runnable done) {
        if (index >= indices.size()) {
            done.run();
            return;
        }
        int badgeIndex = indices.get(index);
        showBadgeUnlockDialog(activity, badgeIndex, () ->
                showBadgeChain(activity, indices, index + 1, done));
    }

    private static void showBadgeUnlockDialog(Activity activity, int badgeIndex, Runnable onDismiss) {
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_badge_unlocked, null, false);
        ShapeableImageView iv = root.findViewById(R.id.dialogBadgeImage);
        TextView tvTitle = root.findViewById(R.id.dialogBadgeTitle);
        TextView tvSubtitle = root.findViewById(R.id.dialogBadgeSubtitle);
        Button btn = root.findViewById(R.id.dialogBadgeOk);

        iv.setImageResource(BadgeRules.badgeDrawableRes(badgeIndex, true));
        iv.setImageTintList(null);
        tvTitle.setText(R.string.badge_unlocked_title);
        tvSubtitle.setText(BadgeRules.badgeRowsFromStats(activity, 0, 0, 0).get(badgeIndex).name);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(root)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View content = root.findViewById(R.id.dialogBadgeRoot);
        content.setScaleX(0.85f);
        content.setScaleY(0.85f);
        content.setAlpha(0f);
        content.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(320)
                .setInterpolator(new OvershootInterpolator())
                .start();

        iv.setScaleX(0.5f);
        iv.setScaleY(0.5f);
        iv.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .setStartDelay(80)
                .start();

        btn.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> onDismiss.run());
        dialog.show();
    }

    private static void showLevelUpDialog(Activity activity, int oldTier, int newTier,
                                          @Nullable Runnable onComplete) {
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_level_up, null, false);
        ShapeableImageView ivAvatar = root.findViewById(R.id.dialogLevelAvatar);
        TextView tvLabel = root.findViewById(R.id.dialogLevelLabel);
        TextView tvFrom = root.findViewById(R.id.dialogLevelFrom);
        TextView tvTo = root.findViewById(R.id.dialogLevelTo);
        ImageView ivArrow = root.findViewById(R.id.dialogLevelArrow);
        ProgressBar progressBar = root.findViewById(R.id.dialogLevelProgress);
        Button btn = root.findViewById(R.id.dialogLevelOk);

        tvLabel.setText(R.string.level_up_title);
        tvFrom.setText(tierDisplayName(activity, oldTier));
        tvTo.setText(tierDisplayName(activity, newTier));
        ivAvatar.setImageResource(R.drawable.default_pfp);

        if (!activity.isFinishing()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("profileImageUrl")
                        .get()
                        .addOnSuccessListener(snapshot -> bindAvatarIfAvailable(activity, ivAvatar, snapshot))
                        .addOnFailureListener(e -> ivAvatar.setImageResource(R.drawable.default_pfp));
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(root)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View content = root.findViewById(R.id.dialogLevelRoot);
        tvFrom.setAlpha(1f);
        ivArrow.setAlpha(1f);
        tvTo.setAlpha(1f);
        tvTo.setScaleX(1f);
        tvTo.setScaleY(1f);

        content.setAlpha(0f);
        content.setTranslationY(16f);
        content.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .start();

        progressBar.setMax(3);
        progressBar.setProgress(Math.max(0, Math.min(3, oldTier)));
        ValueAnimator progressAnim = ValueAnimator.ofInt(oldTier, newTier);
        progressAnim.setDuration(500);
        progressAnim.addUpdateListener(anim ->
                progressBar.setProgress((Integer) anim.getAnimatedValue()));
        progressAnim.start();

        btn.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> complete(onComplete));
        dialog.show();
    }

    private static void bindAvatarIfAvailable(@NonNull Activity activity,
                                              @NonNull ShapeableImageView avatarView,
                                              @NonNull DataSnapshot snapshot) {
        if (activity.isFinishing()) {
            return;
        }
        String imageUrl = snapshot.getValue(String.class);
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            avatarView.setImageResource(R.drawable.default_pfp);
            return;
        }
        Glide.with(activity.getApplicationContext())
                .load(imageUrl)
                .placeholder(R.drawable.default_pfp)
                .error(R.drawable.default_pfp)
                .into(avatarView);
    }

    private static String tierDisplayName(Context ctx, int tier) {
        switch (tier) {
            case 1:
                return ctx.getString(R.string.level_explorer);
            case 2:
                return ctx.getString(R.string.level_sea_mover);
            case 3:
                return ctx.getString(R.string.level_ocean_master);
            case 0:
            default:
                return ctx.getString(R.string.level_newcomer);
        }
    }
}

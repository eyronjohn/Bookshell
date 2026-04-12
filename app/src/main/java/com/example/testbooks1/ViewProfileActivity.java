package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ViewProfileActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private TextView tvFullName, tvProfileBio, tvBooksCount, tvBadgesCount, tvStreakDays, tvProfileLevel;
    private RecyclerView recyclerBadges, recyclerCurrentlyReading;
    private ProfileActivity.ProfileBadgeAdapter badgeAdapter;
    private ProfileActivity.CurrentlyReadingAdapter readingAdapter;
    private final ArrayList<BadgeRules.BadgeRow> badgeRows = new ArrayList<>();
    private final ArrayList<ProfileActivity.CurrentlyReadingBook> readingList = new ArrayList<>();
    private View[] streakBars;
    private String userId;
    private Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        c = this;

        final int topAndSides = WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout();
        View mainRoot = findViewById(R.id.main);
        View bottomBar = findViewById(R.id.bottomNavigationView);
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, insets) -> {
            Insets b = insets.getInsets(topAndSides);
            v.setPadding(b.left, b.top, b.right, 0);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 0, 0, nav.bottom);
            return insets;
        });

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            finish();
            return;
        }

        initialize();
        setupPublicProfileBack();
        disableEditingFeatures();
        loadUserProfile();
        loadUserStats();
        listenCurrentlyReading();
    }

    private void initialize() {
        ivProfileImage = findViewById(R.id.profileImage);
        tvFullName = findViewById(R.id.fullName);
        tvProfileBio = findViewById(R.id.profileBio);
        tvBooksCount = findViewById(R.id.booksCount);
        tvBadgesCount = findViewById(R.id.badgesCount);
        tvStreakDays = findViewById(R.id.streakDays);
        tvProfileLevel = findViewById(R.id.profileBadge);

        recyclerBadges = findViewById(R.id.recyclerProfileBadges);
        recyclerCurrentlyReading = findViewById(R.id.recyclerCurrentlyReading);

        streakBars = new View[]{
                findViewById(R.id.streakBar0),
                findViewById(R.id.streakBar1),
                findViewById(R.id.streakBar2),
                findViewById(R.id.streakBar3),
                findViewById(R.id.streakBar4),
                findViewById(R.id.streakBar5),
                findViewById(R.id.streakBar6)
        };

        recyclerBadges.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        badgeAdapter = new ProfileActivity.ProfileBadgeAdapter(badgeRows);
        recyclerBadges.setAdapter(badgeAdapter);

        recyclerCurrentlyReading.setLayoutManager(new LinearLayoutManager(this));

        readingAdapter = new ProfileActivity.CurrentlyReadingAdapter(
                readingList,
                this,
                book -> {
                    Intent intent = new Intent(this, BookDetailActivity.class);
                    intent.putExtra("bookId", book.bookId);
                    intent.putExtra("title", book.title);
                    intent.putExtra("image", book.coverUrl);
                    intent.putExtra("author", book.author);
                    intent.putExtra("description", book.description);
                    intent.putExtra("category", book.category);
                    startActivity(intent);
                }
        );

        recyclerCurrentlyReading.setAdapter(readingAdapter);
    }

    private void setupPublicProfileBack() {
        View btnBack = findViewById(R.id.btnBackPublicProfile);
        btnBack.setVisibility(View.VISIBLE);
        btnBack.setOnClickListener(v -> finish());
    }

    private void disableEditingFeatures() {
        findViewById(R.id.btnEditProfileImage).setVisibility(View.GONE);
        findViewById(R.id.btnDailyCheckIn).setVisibility(View.GONE);
        findViewById(R.id.bottomNavigationView).setVisibility(View.GONE);
        findViewById(R.id.btnSettings).setVisibility(View.GONE);
    }

    private void loadUserProfile() {
        FirebaseDatabase.getInstance().getReference().child("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String first = snapshot.child("firstName").getValue(String.class);
                        String last = snapshot.child("lastName").getValue(String.class);
                        String bio = snapshot.child("bio").getValue(String.class);
                        String image = snapshot.child("profileImageUrl").getValue(String.class);

                        String full = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();

                        tvFullName.setVisibility(View.VISIBLE);
                        tvProfileBio.setVisibility(View.VISIBLE);
                        ivProfileImage.setVisibility(View.VISIBLE);

                        tvFullName.setText(full.isEmpty() ? getString(R.string.profile_name_anonymous) : full);

                        String role = snapshot.child("role").getValue(String.class);
                        String level = snapshot.child("level").getValue(String.class);

                        if (level == null || level.isEmpty()) {
                            level = role != null ? role : "Reader";
                        }

                        tvProfileLevel.setText(level);
                        tvProfileLevel.setVisibility(View.VISIBLE);

                        if (bio == null || bio.trim().isEmpty()) {
                            tvProfileBio.setText(R.string.profile_bio_empty_other);
                        } else {
                            tvProfileBio.setText(bio);
                        }

                        if (image != null && !image.isEmpty()) {
                            Glide.with(ViewProfileActivity.this)
                                    .load(image)
                                    .placeholder(R.drawable.default_pfp)
                                    .into(ivProfileImage);
                        } else {
                            ivProfileImage.setImageResource(R.drawable.default_pfp);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void loadUserStats() {
        FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("stats")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        long completed = getLong(snapshot, "completed");
                        long reviews = getLong(snapshot, "reviews");
                        long readingLists = getLong(snapshot, "readingLists");
                        long streak = getLong(snapshot, "readingStreak");

                        long unlocked = BadgeRules.countUnlocked(completed, reviews, readingLists);

                        badgeRows.clear();
                        badgeRows.addAll(BadgeRules.badgeRowsFromStats(
                                ViewProfileActivity.this,
                                completed,
                                reviews,
                                readingLists
                        ));
                        badgeAdapter.notifyDataSetChanged();

                        tvBooksCount.setText(String.valueOf(completed));
                        tvBadgesCount.setText(getString(R.string.badges_unlocked_count_format,
                                (int) unlocked, BadgeRules.TOTAL_BADGES));
                        tvStreakDays.setText(getString(R.string.view_profile_streak_days, streak));

                        tvProfileLevel.setText(
                                BadgeRules.levelNameForUnlockedCount(ViewProfileActivity.this, (int) unlocked)
                        );

                        String storedWeek = snapshot.child(StreakCalendar.WEEK_KEY_FIELD).getValue(String.class);
                        String currentWeek = StreakCalendar.currentMondayKeyManila();
                        if (storedWeek != null && !storedWeek.equals(currentWeek)) {
                            applyStreakBars(null);
                        } else {
                            applyStreakBars(snapshot.child("weeklyActivity"));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void listenCurrentlyReading() {
        FirebaseDatabase.getInstance().getReference().child("user_books").child(userId)
                .orderByChild("status")
                .equalTo("Currently Reading")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ProfileActivity.CurrentlyReadingBook> list = new ArrayList<>();
                        for (DataSnapshot bookSnap : snapshot.getChildren()) {
                            ProfileActivity.CurrentlyReadingBook row = new ProfileActivity.CurrentlyReadingBook();
                            row.bookId = bookSnap.getKey();
                            row.title = bookSnap.child("title").getValue(String.class);
                            row.author = bookSnap.child("author").getValue(String.class);
                            row.description = bookSnap.child("description").getValue(String.class);
                            row.category = bookSnap.child("category").getValue(String.class);
                            row.coverUrl = bookSnap.child("imageUrl").getValue(String.class);
                            if (row.coverUrl == null || row.coverUrl.isEmpty()) {
                                row.coverUrl = bookSnap.child("coverUrl").getValue(String.class);
                            }
                            list.add(row);
                        }
                        readingAdapter.setBooks(list);
                        boolean empty = list.isEmpty();
                        //tvCurrentlyReadingEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
//                        TextView tv = findViewById(R.id.tvCurrentlyDiving);
//                        tv.setText("VIEW");
                        recyclerCurrentlyReading.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(c, R.string.toast_currently_reading_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyStreakBars(@Nullable DataSnapshot weeklyActivity) {
        for (int i = 0; i < 7; i++) {
            long val = weeklyActivity == null ? 0L : getLong(weeklyActivity, String.valueOf(i));

            ViewGroup.LayoutParams lp = streakBars[i].getLayoutParams();
            lp.height = val > 0 ? 80 : 8;
            streakBars[i].setLayoutParams(lp);
        }
    }

    private long getLong(DataSnapshot snap, String key) {
        if (snap == null) return 0;
        Long val = snap.child(key).getValue(Long.class);
        return val != null ? val : 0;
    }
}

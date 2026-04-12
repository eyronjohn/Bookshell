package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("NotifyDataSetChanged")
public class ProfileActivity extends AppCompatActivity {

    /** Calendar for streak, check-in, and weekly bars (Philippines). */
    private TextView tvFullName, tvProfileBio, tvBooksCount, tvBadgesCount, tvStreakDays, tvViewAllBadges;
    private TextView tvProfileLevel;
    private TextView tvCurrentlyReadingEmpty;
    private Button btnDailyCheckIn;
    private ImageButton btnSettings;
    private ImageView ivProfileImage;
    private ImageButton btnEditProfileImage;
    private View[] streakBars;
    private int streakBarMaxHeightPx;
    private BottomNavigationView bottomNav;
    private RecyclerView recyclerCurrentlyReading;
    private CurrentlyReadingAdapter currentlyReadingAdapter;
    private ProfileBadgeAdapter profileBadgeAdapter;
    private final ArrayList<BadgeRules.BadgeRow> profileBadgeRows = new ArrayList<>();

    private Context context;

    private DatabaseReference mDatabase;
    private String userId;

   
    private String cachedLastStreakDateForCheckIn;
    private final Handler checkInDateHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable checkInMidnightRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        context = this;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        bottomNav = findViewById(R.id.bottomNavigationView);
        tvFullName = findViewById(R.id.fullName);
        tvProfileBio = findViewById(R.id.profileBio);
        tvBooksCount = findViewById(R.id.booksCount);
        tvBadgesCount = findViewById(R.id.badgesCount);
        tvStreakDays = findViewById(R.id.streakDays);
        tvViewAllBadges = findViewById(R.id.viewAllBadges);
        tvProfileLevel = findViewById(R.id.profileBadge);
        ivProfileImage = findViewById(R.id.profileImage);
        btnEditProfileImage = findViewById(R.id.btnEditProfileImage);
        btnSettings = findViewById(R.id.btnSettings);
        btnDailyCheckIn = findViewById(R.id.btnDailyCheckIn);
        streakBars = new View[]{
                findViewById(R.id.streakBar0),
                findViewById(R.id.streakBar1),
                findViewById(R.id.streakBar2),
                findViewById(R.id.streakBar3),
                findViewById(R.id.streakBar4),
                findViewById(R.id.streakBar5),
                findViewById(R.id.streakBar6)
        };
        streakBarMaxHeightPx = (int) (80 * getResources().getDisplayMetrics().density + 0.5f);
        tvCurrentlyReadingEmpty = findViewById(R.id.tvCurrentlyReadingEmpty);
        recyclerCurrentlyReading = findViewById(R.id.recyclerCurrentlyReading);

        recyclerCurrentlyReading.setLayoutManager(new LinearLayoutManager(this));
        currentlyReadingAdapter = new CurrentlyReadingAdapter(new ArrayList<>(), context, book -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("bookId", book.bookId);
            intent.putExtra("title", book.title);
            intent.putExtra("image", book.coverUrl);
            intent.putExtra("author", book.author);
            intent.putExtra("description", book.description);
            intent.putExtra("category", book.category);
            startActivity(intent);
        });
        recyclerCurrentlyReading.setAdapter(currentlyReadingAdapter);

        RecyclerView recyclerProfileBadges = findViewById(R.id.recyclerProfileBadges);
        recyclerProfileBadges.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        profileBadgeAdapter = new ProfileBadgeAdapter(profileBadgeRows);
        recyclerProfileBadges.setAdapter(profileBadgeAdapter);

        bottomNav.setSelectedItemId(R.id.nav_profile);

        setupNavigation();
        listenUserProfile();
        listenUserStats();
        listenCurrentlyReading();
        setupClickListeners();

        final int topAndSides = WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout();
        View mainRoot = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, insets) -> {
            Insets b = insets.getInsets(topAndSides);
            v.setPadding(b.left, b.top, b.right, 0);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 0, 0, nav.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshDailyCheckInButtonAfterCalendarChange();
        scheduleCheckInRefreshAtNextStreakMidnight();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDailyCheckInButtonAfterCalendarChange();
    }

    @Override
    protected void onStop() {
        cancelScheduledCheckInMidnightRefresh();
        super.onStop();
    }

    /**
     * Stats listener only runs when Firebase data changes, so crossing midnight does not fire it.
     * Recompute whether today's streak day key ({@link StreakCalendar#streakDayKey()}) still matches {@link #cachedLastStreakDateForCheckIn}.
     */
    private void refreshDailyCheckInButtonAfterCalendarChange() {
        if (isFinishing() || btnDailyCheckIn == null) {
            return;
        }
        if (cachedLastStreakDateForCheckIn == null) {
            return;
        }
        String today = StreakCalendar.streakDayKey();
        boolean alreadyCheckedInToday = today.equals(cachedLastStreakDateForCheckIn);
        btnDailyCheckIn.setEnabled(!alreadyCheckedInToday);
        btnDailyCheckIn.setText(alreadyCheckedInToday
                ? R.string.action_checked_in_today
                : R.string.action_daily_check_in);
    }

    private void scheduleCheckInRefreshAtNextStreakMidnight() {
        cancelScheduledCheckInMidnightRefresh();
        long delay = StreakCalendar.millisUntilNextStreakMidnight();
        checkInMidnightRunnable = () -> {
            refreshDailyCheckInButtonAfterCalendarChange();
            scheduleCheckInRefreshAtNextStreakMidnight();
        };
        checkInDateHandler.postDelayed(checkInMidnightRunnable, delay);
    }

    private void cancelScheduledCheckInMidnightRefresh() {
        if (checkInMidnightRunnable != null) {
            checkInDateHandler.removeCallbacks(checkInMidnightRunnable);
            checkInMidnightRunnable = null;
        }
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                MainActivity.openHome(context);
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(context, SearchActivity.class));
                return true;
            } else if (id == R.id.nav_community) {
                startActivity(new Intent(context, CommunityActivity.class));
                return true;
            } else if (id == R.id.nav_library) {
                startActivity(new Intent(context, LibraryActivity.class));
                return true;
            }
            return id == R.id.nav_profile;
        });
    }

    private void listenUserProfile() {
        mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    ivProfileImage.setImageResource(R.drawable.default_pfp);
                    ivProfileImage.setVisibility(View.VISIBLE);
                    tvFullName.setVisibility(View.VISIBLE);
                    tvProfileBio.setVisibility(View.VISIBLE);
                    tvProfileLevel.setVisibility(View.VISIBLE);
                    return;
                }
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                String bio = snapshot.child("bio").getValue(String.class);
                String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                String fn = firstName != null ? firstName : "";
                String ln = lastName != null ? lastName : "";
                tvFullName.setText(getString(R.string.profile_name_format, fn, ln).trim());
                if (bio == null || bio.trim().isEmpty()) {
                    tvProfileBio.setText(R.string.bio_default_text);
                } else {
                    tvProfileBio.setText(bio);
                }
                tvFullName.setVisibility(View.VISIBLE);
                tvProfileBio.setVisibility(View.VISIBLE);
                tvProfileLevel.setVisibility(View.VISIBLE);

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context).load(imageUrl).into(ivProfileImage);
                } else {
                    ivProfileImage.setImageResource(R.drawable.default_pfp);
                }
                ivProfileImage.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                ivProfileImage.setImageResource(R.drawable.default_pfp);
                ivProfileImage.setVisibility(View.VISIBLE);
                tvFullName.setVisibility(View.VISIBLE);
                tvProfileBio.setVisibility(View.VISIBLE);
                tvProfileLevel.setVisibility(View.VISIBLE);
                Toast.makeText(context, R.string.toast_profile_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenUserStats() {
        mDatabase.child("users").child(userId).child("stats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvBooksCount.setText(R.string.zero);
                    tvBadgesCount.setText(getString(R.string.badges_unlocked_count_format, 0, BadgeRules.TOTAL_BADGES));
                    tvStreakDays.setText(getResources().getQuantityString(R.plurals.streak_days_format, 0, 0));
                    tvProfileLevel.setText(BadgeRules.levelNameForUnlockedCount(context, 0));
                    tvBooksCount.setVisibility(View.VISIBLE);
                    tvBadgesCount.setVisibility(View.VISIBLE);
                    tvProfileLevel.setVisibility(View.VISIBLE);
                    profileBadgeRows.clear();
                    profileBadgeRows.addAll(BadgeRules.badgeRowsFromStats(context, 0, 0, 0));
                    profileBadgeAdapter.notifyDataSetChanged();
                    applyStreakBars(null);
                    cachedLastStreakDateForCheckIn = null;
                    btnDailyCheckIn.setEnabled(true);
                    btnDailyCheckIn.setText(R.string.action_daily_check_in);
                    return;
                }
                DatabaseReference statsRef = mDatabase.child("users").child(userId).child("stats");
                if (StreakCalendar.applyWeekRolloverIfNeeded(statsRef, snapshot)) {
                    return;
                }
                long completed = BadgeRules.readStatLong(snapshot, "completed");
                long reviews = BadgeRules.readStatLong(snapshot, "reviews");
                long readingLists = BadgeRules.readStatLong(snapshot, "readingLists");
                long streak = BadgeRules.readStatLong(snapshot, "readingStreak");
                String lastStreakDate = snapshot.child("lastStreakDate").getValue(String.class);
                cachedLastStreakDateForCheckIn = lastStreakDate;
                String today = StreakCalendar.streakDayKey();
                int dayGap = StreakCalendar.dayGapStreak(lastStreakDate, today);

                if (dayGap >= 2 && streak != 0L) {
                    statsRef.child("readingStreak").setValue(0L);
                    for (int i = 0; i < 7; i++) {
                        statsRef.child("weeklyActivity").child(String.valueOf(i)).setValue(0L);
                    }
                    statsRef.child(StreakCalendar.WEEK_KEY_FIELD)
                            .setValue(StreakCalendar.currentMondayKeyManila());
                    streak = 0L;
                }

            
                if (streak == 1L && today.equals(lastStreakDate)) {
                    if (enforceSingleDayWeeklyForStreakOne(snapshot.child("weeklyActivity"))) {
                        refreshDailyCheckInButtonAfterCalendarChange();
                        return;
                    }
                }

                if (normalizeWeeklyActivityIfNeeded(streak, lastStreakDate, today, snapshot.child("weeklyActivity"))) {
                    refreshDailyCheckInButtonAfterCalendarChange();
                    return;
                }

                refreshDailyCheckInButtonAfterCalendarChange();

                tvBooksCount.setText(String.valueOf(completed));
                int unlocked = BadgeRules.countUnlocked(completed, reviews, readingLists);
                tvBadgesCount.setText(getString(R.string.badges_unlocked_count_format, unlocked, BadgeRules.TOTAL_BADGES));
                int streakInt = (int) streak;
                tvStreakDays.setText(getResources().getQuantityString(R.plurals.streak_days_format, streakInt, streakInt));
                tvProfileLevel.setText(BadgeRules.levelNameForUnlockedCount(context, unlocked));
                tvBooksCount.setVisibility(View.VISIBLE);
                tvBadgesCount.setVisibility(View.VISIBLE);
                tvProfileLevel.setVisibility(View.VISIBLE);
                profileBadgeRows.clear();
                profileBadgeRows.addAll(BadgeRules.badgeRowsFromStats(context, completed, reviews, readingLists));
                profileBadgeAdapter.notifyDataSetChanged();

                applyStreakBars(snapshot.child("weeklyActivity"));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, R.string.toast_stats_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean enforceSingleDayWeeklyForStreakOne(@NonNull DataSnapshot weeklyActivitySnapshot) {
        int todayIdx = StreakCalendar.dayOfWeekIndexStreak();
        boolean needsFix = false;
        for (int i = 0; i < 7; i++) {
            long v = BadgeRules.readStatLong(weeklyActivitySnapshot, String.valueOf(i));
            if (i == todayIdx) {
                if (v <= 0L) {
                    needsFix = true;
                }
            } else if (v > 0L) {
                needsFix = true;
            }
        }
        if (!needsFix) {
            return false;
        }
        DatabaseReference waRef = mDatabase.child("users").child(userId).child("stats").child("weeklyActivity");
        for (int i = 0; i < 7; i++) {
            waRef.child(String.valueOf(i)).setValue(i == todayIdx ? 1L : 0L);
        }
        return true;
    }

    private boolean normalizeWeeklyActivityIfNeeded(long streak,
                                                    @Nullable String lastStreakDate,
                                                    @NonNull String today,
                                                    @NonNull DataSnapshot weeklyActivitySnapshot) {
        boolean hasAny = false;
        for (int i = 0; i < 7; i++) {
            if (BadgeRules.readStatLong(weeklyActivitySnapshot, String.valueOf(i)) > 0L) {
                hasAny = true;
            }
        }

        if (streak <= 0L) {
            if (!hasAny) {
                return false;
            }
            DatabaseReference waRef = mDatabase.child("users").child(userId).child("stats").child("weeklyActivity");
            for (int i = 0; i < 7; i++) {
                waRef.child(String.valueOf(i)).setValue(0L);
            }
            return true;
        }

        if (!today.equals(lastStreakDate)) {
            return false;
        }

        int todayIndex = StreakCalendar.dayOfWeekIndexStreak();
        if (BadgeRules.readStatLong(weeklyActivitySnapshot, String.valueOf(todayIndex)) > 0L) {
            return false;
        }

        
        mDatabase.child("users").child(userId).child("stats")
                .child("weeklyActivity").child(String.valueOf(todayIndex)).setValue(1L);
        return true;
    }

    private void applyStreakBars(@Nullable DataSnapshot weeklyActivitySnapshot) {
        if (streakBars == null) {
            return;
        }
        for (int i = 0; i < 7; i++) {
            long val = weeklyActivitySnapshot == null
                    ? 0L
                    : BadgeRules.readStatLong(weeklyActivitySnapshot, String.valueOf(i));
            int h = val > 0L ? streakBarMaxHeightPx : 8;
            ViewGroup.LayoutParams lp = streakBars[i].getLayoutParams();
            lp.height = h;
            streakBars[i].setLayoutParams(lp);
        }
    }

    private void listenCurrentlyReading() {
        mDatabase.child("user_books").child(userId)
                .orderByChild("status")
                .equalTo("Currently Reading")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<CurrentlyReadingBook> list = new ArrayList<>();
                        for (DataSnapshot bookSnap : snapshot.getChildren()) {
                            CurrentlyReadingBook row = new CurrentlyReadingBook();
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
                        currentlyReadingAdapter.setBooks(list);
                        boolean empty = list.isEmpty();
                        tvCurrentlyReadingEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                        recyclerCurrentlyReading.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, R.string.toast_currently_reading_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickListeners() {
        tvViewAllBadges.setOnClickListener(v -> startActivity(new Intent(context, BadgesActivity.class)));
        btnEditProfileImage.setOnClickListener(v -> startActivity(new Intent(context, EditProfileActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(context, SettingsActivity.class)));
        btnDailyCheckIn.setOnClickListener(v -> applyDailyCheckIn());
        ivProfileImage.setOnClickListener(v -> {
            // Edit entry is the pencil button in the top bar.
        });
    }

    private void applyDailyCheckIn() {
        DatabaseReference statsRef = mDatabase.child("users").child(userId).child("stats");
        int baselineBits = BadgeMilestoneHelper.getStoredBadgeBits(getApplicationContext(), userId);
        int baselineTier = BadgeMilestoneHelper.getStoredBadgeTier(getApplicationContext(), userId);
        statsRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String today = StreakCalendar.streakDayKey();
                String last = currentData.child("lastStreakDate").getValue(String.class);
                if (today.equals(last)) {
                    return Transaction.success(currentData);
                }

                long currentStreak = readMutableLong(currentData.child("readingStreak"));
                int gap = StreakCalendar.dayGapStreak(last, today);
                long nextStreak = (gap == 1) ? (currentStreak + 1L) : 1L;

                currentData.child("readingStreak").setValue(nextStreak);
                currentData.child("lastStreakDate").setValue(today);

                int dayIndex = StreakCalendar.dayOfWeekIndexStreak();
                if (nextStreak == 1L) {
                    for (int i = 0; i < 7; i++) {
                        currentData.child("weeklyActivity").child(String.valueOf(i)).setValue(0L);
                    }
                }
                currentData.child("weeklyActivity").child(String.valueOf(dayIndex)).setValue(1L);
                currentData.child(StreakCalendar.WEEK_KEY_FIELD)
                        .setValue(StreakCalendar.currentMondayKeyManila());
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(context, R.string.toast_check_in_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                String today = StreakCalendar.streakDayKey();
                String last = currentData != null ? currentData.child("lastStreakDate").getValue(String.class) : null;
                if (!today.equals(last)) {
                    return;
                }
                FirebaseDatabase.getInstance().getReference("users").child(userId).child("stats").get()
                        .addOnCompleteListener(t -> {
                            if (isFinishing()) {
                                return;
                            }
                            DataSnapshot snap = t.isSuccessful() ? t.getResult() : null;
                            BadgeMilestoneHelper.runAfterStatsCelebrations(
                                    ProfileActivity.this,
                                    getApplicationContext(),
                                    userId,
                                    snap,
                                    () -> {
                                        if (isFinishing()) {
                                            return;
                                        }
                                        Toast.makeText(context, R.string.toast_checked_in_today, Toast.LENGTH_SHORT).show();
                                    },
                                    baselineBits,
                                    baselineTier);
                        });
            }
        });
    }

    private static long readMutableLong(@Nullable MutableData node) {
        if (node == null) {
            return 0L;
        }
        Object value = node.getValue();
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Double) {
            return Math.round((Double) value);
        }
        return 0L;
    }

    static class CurrentlyReadingBook {
        String bookId;
        String title;
        String author;
        String description;
        String category;
        String coverUrl;
    }

    @SuppressLint("NotifyDataSetChanged")
    static class CurrentlyReadingAdapter extends RecyclerView.Adapter<CurrentlyReadingAdapter.BookVH> {

        interface OnBookClickListener {
            void onBookClick(CurrentlyReadingBook book);
        }

        private final List<CurrentlyReadingBook> books;
        private final Context context;
        private final OnBookClickListener listener;

        CurrentlyReadingAdapter(List<CurrentlyReadingBook> books, Context context, OnBookClickListener listener) {
            this.books = books;
            this.context = context;
            this.listener = listener;
        }

        void setBooks(List<CurrentlyReadingBook> newBooks) {
            books.clear();
            books.addAll(newBooks);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BookVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_currently_reading, parent, false);
            return new BookVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BookVH holder, int position) {
            CurrentlyReadingBook b = books.get(position);
            holder.tvTitle.setText(b.title != null ? b.title : "");
            holder.tvAuthor.setText(b.author != null ? b.author : "");

            if (b.coverUrl != null && !b.coverUrl.isEmpty()) {
                Glide.with(context).load(b.coverUrl).into(holder.ivCover);
            } else {
                holder.ivCover.setImageResource(R.drawable.sample_book);
            }

            View.OnClickListener openDetail = v -> {
                if (b.bookId != null) {
                    listener.onBookClick(b);
                }
            };
            holder.card.setOnClickListener(openDetail);
            holder.tvContinue.setOnClickListener(openDetail);
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        static class BookVH extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final ImageView ivCover;
            final TextView tvTitle;
            final TextView tvAuthor;
            final View tvContinue;

            BookVH(@NonNull View itemView) {
                super(itemView);
                card = (MaterialCardView) itemView;
                ivCover = itemView.findViewById(R.id.bookCover);
                tvTitle = itemView.findViewById(R.id.bookTitle);
                tvAuthor = itemView.findViewById(R.id.bookAuthor);
                tvContinue = itemView.findViewById(R.id.btnContinue);
            }
        }
    }

    static class ProfileBadgeAdapter extends RecyclerView.Adapter<ProfileBadgeAdapter.BadgeVH> {

        private final List<BadgeRules.BadgeRow> rows;

        ProfileBadgeAdapter(List<BadgeRules.BadgeRow> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public BadgeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge, parent, false);
            float density = parent.getContext().getResources().getDisplayMetrics().density;
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    (int) (118 * density),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            int hMargin = (int) (6 * density);
            lp.setMargins(hMargin, 0, hMargin, 0);
            v.setLayoutParams(lp);
            return new BadgeVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BadgeVH holder, int position) {
            BadgeRules.BadgeRow row = rows.get(position);
            holder.tvName.setText(row.name);
            holder.ivCheck.setVisibility(View.GONE);

            holder.ivIcon.setImageTintList(null);
            holder.ivIcon.setBackgroundTintList(null);
            holder.ivIcon.setImageResource(BadgeRules.badgeDrawableRes(position, row.unlocked));

            if (row.unlocked) {
                holder.itemView.setAlpha(1f);
                holder.tvStatus.setText(R.string.badge_status_unlocked);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_blue));
            } else {
                holder.itemView.setAlpha(1f);
                holder.tvStatus.setText(R.string.badge_status_locked);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class BadgeVH extends RecyclerView.ViewHolder {
            final ShapeableImageView ivIcon;
            final ImageView ivCheck;
            final TextView tvName;
            final TextView tvStatus;

            BadgeVH(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.ivBadgeIcon);
                ivCheck = itemView.findViewById(R.id.ivBadgeCheck);
                tvName = itemView.findViewById(R.id.tvBadgeName);
                tvStatus = itemView.findViewById(R.id.tvBadgeStatus);
            }
        }
    }
}

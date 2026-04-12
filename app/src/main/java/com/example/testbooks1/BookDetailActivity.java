package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Adapter.ReviewAdapter;
import com.example.testbooks1.BadgeMilestoneHelper;
import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.Review;
import com.example.testbooks1.Model.UserBook;
import com.example.testbooks1.Model.UserManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class BookDetailActivity extends AppCompatActivity {
    TextView tvTitle, tvAuthor, tvDescription, tvCategory, tvOverallRating, tvNoReviews;
    ShapeableImageView ivBook;
    DatabaseReference communityRef, reactionsRef, reviewRef;
    RatingBar ratingBar;
    EditText etReview;
    Button btnPreview, btnCreateList, btnAddToList, btnSubmitReview;
    Button btnWantToRead, btnCurrentlyReading, btnCompleted;
    RecyclerView rvReviews;
    ImageView btnBack, btnLike, btnFire, btnHeart, btnSad, btnAngry;
    TextView tvLikeCount, tvFireCount, tvHeartCount, tvSadCount, tvAngryCount;
    private ReviewAdapter reviewAdapter;
    int likeCount = 0, fireCount = 0, heartCount = 0, sadCount = 0, angryCount = 0;
    String bookId;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);
        c = this;
        View bookScroll = findViewById(R.id.book_detail_scroll);
        final int bsL = bookScroll.getPaddingLeft();
        final int bsT = bookScroll.getPaddingTop();
        final int bsR = bookScroll.getPaddingRight();
        final int bsB = bookScroll.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bookScroll, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    bsL + systemBars.left,
                    bsT + systemBars.top,
                    bsR + systemBars.right,
                    bsB + systemBars.bottom
            );
            return insets;
        });
        attachKeyboardScrollPadding(bookScroll, findViewById(R.id.main));
        initialize();
    }

    public void initialize(){
        ivBook = findViewById(R.id.ivDetailImage);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDescription = findViewById(R.id.tvDescription);
        tvCategory = findViewById(R.id.tvCategory);
        tvOverallRating = findViewById(R.id.tvOverallRating);
        tvNoReviews = findViewById(R.id.tvNoReviews);

        //etListTitle = findViewById(R.id.etListTitle);
        //btnAddToCommunity = findViewById(R.id.btnAddToCommunity);
        btnWantToRead = findViewById(R.id.btnWantToRead);
        btnCurrentlyReading = findViewById(R.id.btnCurrentlyReading);
        btnCompleted = findViewById(R.id.btnCompleted);

        communityRef = FirebaseDatabase.getInstance().getReference("communityLists");
        ratingBar = findViewById(R.id.ratingBar);
        etReview = findViewById(R.id.etReview);
        View bookDetailScroll = findViewById(R.id.book_detail_scroll);
        etReview.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ensureFieldVisibleScroll(bookDetailScroll, v);
            }
        });

        btnPreview = findViewById(R.id.btnPreview);
        btnCreateList = findViewById(R.id.btnCreateList);
        btnAddToList = findViewById(R.id.btnAddToList);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        rvReviews = findViewById(R.id.rvReviews);

        btnBack = findViewById(R.id.btnBack);
        btnLike = findViewById(R.id.btnLike);
        btnFire = findViewById(R.id.btnFire);
        btnHeart = findViewById(R.id.btnHeart);
        btnSad = findViewById(R.id.btnSad);
        btnAngry = findViewById(R.id.btnAngry);

        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvFireCount = findViewById(R.id.tvFireCount);
        tvHeartCount = findViewById(R.id.tvHeartCount);
        tvSadCount = findViewById(R.id.tvSadCount);
        tvAngryCount = findViewById(R.id.tvAngryCount);

        bookId = getIntent().getStringExtra("bookId");
        if (bookId == null || bookId.trim().isEmpty()) {
            Toast.makeText(this, "Missing book ID.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadBookStatus(btnWantToRead, btnCurrentlyReading, btnCompleted);

        reviewRef = FirebaseDatabase.getInstance()
                .getReference("reviews")
                .child(bookId);

        String title = getIntent().getStringExtra("title");
        String image = getIntent().getStringExtra("image");
        String author = getIntent().getStringExtra("author");
        String description = getIntent().getStringExtra("description");
        String category = getIntent().getStringExtra("category");
        bindBookDetails(title, image, author, description, category);
        if (title == null || title.trim().isEmpty() || image == null || image.trim().isEmpty()) {
            loadBookDetailsFromUserBooks();
        }

        loadOverallRating();

        reactionsRef = FirebaseDatabase.getInstance()
                .getReference("book_reactions")
                .child(bookId);

        reactionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                likeCount = snapshot.child("like").getChildrenCount() > 0 ? (int) snapshot.child("like").getChildrenCount() : 0;
                fireCount = snapshot.child("fire").getChildrenCount() > 0 ? (int) snapshot.child("fire").getChildrenCount() : 0;
                heartCount = snapshot.child("heart").getChildrenCount() > 0 ? (int) snapshot.child("heart").getChildrenCount() : 0;
                sadCount = snapshot.child("sad").getChildrenCount() > 0 ? (int) snapshot.child("sad").getChildrenCount() : 0;
                angryCount = snapshot.child("angry").getChildrenCount() > 0 ? (int) snapshot.child("angry").getChildrenCount() : 0;

                tvLikeCount.setText(String.valueOf(likeCount));
                tvFireCount.setText(String.valueOf(fireCount));
                tvHeartCount.setText(String.valueOf(heartCount));
                tvSadCount.setText(String.valueOf(sadCount));
                tvAngryCount.setText(String.valueOf(angryCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        btnCreateList.setOnClickListener(v -> {
            Intent intent = new Intent(c, AddToListActivity.class);
            intent.putExtra("bookId", bookId);
            intent.putExtra("title", tvTitle.getText().toString());
            intent.putExtra("author", tvAuthor.getText().toString());
            intent.putExtra("image", getIntent().getStringExtra("image"));
            intent.putExtra("category", tvCategory.getText().toString());
            intent.putExtra("description", tvDescription.getText().toString());
            startActivity(intent);
        });

        btnAddToList.setOnClickListener(v -> {
            String uid = AuthManager.getUid();
            if (uid == null) return;

            DatabaseReference listsRef = FirebaseDatabase.getInstance()
                    .getReference("communityLists")
                    .child(uid);

            listsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(c, "No current existing list.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayList<String> listTitles = new ArrayList<>();
                    ArrayList<String> listIds = new ArrayList<>();

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String listId = ds.getKey();
                        if (!ds.child("books").hasChild(bookId)) {
                            listIds.add(listId);
                            String title = ds.child("title").getValue(String.class);
                            listTitles.add(title != null ? title : listId);
                        }
                    }

                    if (listTitles.isEmpty()) {
                        Toast.makeText(c, "Book is already in all your lists.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean[] checkedItems = new boolean[listTitles.size()];
                    ArrayList<Integer> selectedIndices = new ArrayList<>();

                    new AlertDialog.Builder(c)
                            .setTitle("Add Book to...")
                            .setMultiChoiceItems(listTitles.toArray(new String[0]), checkedItems,
                                    (dialog, which, isChecked) -> {
                                        if (isChecked) {
                                            selectedIndices.add(which);
                                        } else {
                                            selectedIndices.remove(Integer.valueOf(which));
                                        }
                                    })
                            .setPositiveButton("Add", (dialog, which) -> {
                                for (int index : selectedIndices) {
                                    String selectedListId = listIds.get(index);
                                    addBookToUserList(selectedListId);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(c, "Failed to load lists", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnLike.setOnClickListener(v -> incrementReaction("like"));
        btnFire.setOnClickListener(v -> incrementReaction("fire"));
        btnHeart.setOnClickListener(v -> incrementReaction("heart"));
        btnSad.setOnClickListener(v -> incrementReaction("sad"));
        btnAngry.setOnClickListener(v -> incrementReaction("angry"));

        btnPreview.setOnClickListener(v -> openBookPreview());

        btnWantToRead.setOnClickListener(v -> {
            selectStatus(btnWantToRead, btnWantToRead, btnCurrentlyReading, btnCompleted);
            addBookToUserBooks("Want to Read");
        });

        btnCurrentlyReading.setOnClickListener(v -> {
            selectStatus(btnCurrentlyReading, btnWantToRead, btnCurrentlyReading, btnCompleted);
            addBookToUserBooks("Currently Reading");
        });

        btnCompleted.setOnClickListener(v -> {
            selectStatus(btnCompleted, btnWantToRead, btnCurrentlyReading, btnCompleted);
            addBookToUserBooks("Read");
        });

        btnSubmitReview.setOnClickListener(v -> {
            String uid = AuthManager.getUid();
            String fullName = UserManager.getFullName();
            if (uid == null) return;
            btnSubmitReview.setEnabled(false);

            String comment = etReview.getText().toString().trim();
            int rating = (int) ratingBar.getRating();
            if (comment.isEmpty()) {
                Toast.makeText(c, "Please add a review", Toast.LENGTH_SHORT).show();
                btnSubmitReview.setEnabled(true);
                return;
            } else if(rating == 0){
                Toast.makeText(c, "Please add a rating.", Toast.LENGTH_SHORT).show();
                btnSubmitReview.setEnabled(true);
                return;
            }

            String reviewId = reviewRef.push().getKey();
            if (reviewId == null) {
                Toast.makeText(c, "Could not create review ID.", Toast.LENGTH_SHORT).show();
                btnSubmitReview.setEnabled(true);
                return;
            }
            Review review = new Review(
                    uid,
                    fullName,
                    rating,
                    comment,
                    System.currentTimeMillis()
            );

            reviewRef.child(reviewId).setValue(review).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DatabaseReference reviewStatsRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(uid)
                            .child("stats")
                            .child("reviews");
                    int baselineBits = BadgeMilestoneHelper.getStoredBadgeBits(getApplicationContext(), uid);
                    int baselineTier = BadgeMilestoneHelper.getStoredBadgeTier(getApplicationContext(), uid);
                    reviewStatsRef.setValue(ServerValue.increment(1)).addOnCompleteListener(t2 -> {
                        if (!t2.isSuccessful()) {
                            btnSubmitReview.setEnabled(true);
                            Toast.makeText(BookDetailActivity.this, R.string.toast_review_stats_failed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FirebaseDatabase.getInstance().getReference("users").child(uid).child("stats").get()
                                .addOnCompleteListener(t3 -> {
                                    Runnable after = () -> {
                                        if (isFinishing()) {
                                            return;
                                        }
                                        Toast.makeText(BookDetailActivity.this, R.string.toast_review_added, Toast.LENGTH_SHORT).show();
                                        etReview.setText("");
                                        ratingBar.setRating(0);
                                        btnSubmitReview.setEnabled(true);
                                    };
                                    if (!isFinishing() && t3.isSuccessful() && t3.getResult() != null) {
                                        BadgeMilestoneHelper.runAfterStatsCelebrations(
                                                BookDetailActivity.this,
                                                getApplicationContext(),
                                                uid,
                                                t3.getResult(),
                                                after,
                                                baselineBits,
                                                baselineTier);
                                    } else {
                                        after.run();
                                    }
                                });
                    });
                } else {
                    btnSubmitReview.setEnabled(true);
                    Toast.makeText(BookDetailActivity.this, R.string.toast_review_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });

        ArrayList<Review> reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);

        reviewRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int previousSize = reviewList.size();
                reviewList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Review r = ds.getValue(Review.class);
                    if (r != null) {
                        reviewList.add(r);
                    }
                }
                if (previousSize > 0) {
                    reviewAdapter.notifyItemRangeRemoved(0, previousSize);
                }
                reviewAdapter.notifyItemRangeInserted(0, reviewList.size());
                if (reviewList.isEmpty()) {
                    tvNoReviews.setVisibility(View.VISIBLE);
                    rvReviews.setVisibility(View.GONE);
                } else {
                    tvNoReviews.setVisibility(View.GONE);
                    rvReviews.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BookDetailActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (reviewAdapter != null) {
            reviewAdapter.detachUserProfileListeners();
        }
        super.onDestroy();
    }


    private void openBookPreview() {
        String link = firstNonEmptyTrimmed(
                getIntent().getStringExtra("previewLink"),
                getIntent().getStringExtra("readerLink"));
        if (link != null) {
            if (link.startsWith("http://")) {
                link = link.replace("http://", "https://");
            }
            openExternalPreviewUrl(link);
            return;
        }
        String id = getIntent().getStringExtra("bookId");
        if (id != null && !id.trim().isEmpty()) {
            String url = "https://books.google.com/books?id=" + id.trim()
                    + "&printsec=frontcover&dq&hl=en&source=gbs_api";
            openExternalPreviewUrl(url);
        } else {
            Toast.makeText(c, "Preview not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private static String firstNonEmptyTrimmed(@Nullable String a, @Nullable String b) {
        if (a != null) {
            String t = a.trim();
            if (!t.isEmpty()) {
                return t;
            }
        }
        if (b != null) {
            String t = b.trim();
            if (!t.isEmpty()) {
                return t;
            }
        }
        return null;
    }

    private void openExternalPreviewUrl(@NonNull String url) {
        Uri uri = Uri.parse(url);
        try {
            CustomTabColorSchemeParams scheme = new CustomTabColorSchemeParams.Builder().build();
            CustomTabsIntent tabsIntent = new CustomTabsIntent.Builder()
                    .setDefaultColorSchemeParams(scheme)
                    .setShowTitle(true)
                    .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                    .build();
            tabsIntent.launchUrl(this, uri);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(c, "Preview not available", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e2) {
                Toast.makeText(c, "Preview not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void bindBookDetails(String title, String image, String author, String description, String category) {
        tvTitle.setText(title != null ? title : "");
        tvAuthor.setText(author != null ? author : "Unknown");
        tvDescription.setText(description != null ? description : "No description available");
        tvCategory.setText(category != null ? category : "Unknown");

        String safeImage = image;
        if (safeImage != null && safeImage.startsWith("http://")) {
            safeImage = safeImage.replace("http://", "https://");
        }
        if (safeImage == null || safeImage.trim().isEmpty()) {
            ivBook.setImageResource(R.drawable.sample_book);
        } else {
            Glide.with(this).load(safeImage).into(ivBook);
        }
    }

    private void loadBookDetailsFromUserBooks() {
        String uid = AuthManager.getUid();
        if (uid == null || bookId == null || bookId.trim().isEmpty()) {
            return;
        }
        FirebaseDatabase.getInstance()
                .getReference("user_books")
                .child(uid)
                .child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            return;
                        }
                        String title = snapshot.child("title").getValue(String.class);
                        String image = snapshot.child("imageUrl").getValue(String.class);
                        if (image == null || image.isEmpty()) {
                            image = snapshot.child("coverUrl").getValue(String.class);
                        }
                        String author = snapshot.child("author").getValue(String.class);
                        String description = snapshot.child("description").getValue(String.class);
                        String category = snapshot.child("category").getValue(String.class);
                        bindBookDetails(title, image, author, description, category);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void addBookToUserBooks(String status) {
        String uid = AuthManager.getUid();
        if (uid == null) return;

        long timestamp = System.currentTimeMillis();
        DatabaseReference userBooksRef = FirebaseDatabase.getInstance()
                .getReference("user_books")
                .child(uid);

        String bookId = getIntent().getStringExtra("bookId");
        if (bookId == null || bookId.trim().isEmpty()) {
            Toast.makeText(this, "Missing book ID.", Toast.LENGTH_SHORT).show();
            return;
        }
        String imageUrl = getIntent().getStringExtra("image");
        if (imageUrl != null && imageUrl.startsWith("http://")) {
            imageUrl = imageUrl.replace("http://", "https://");
        }

        String author = getIntent().getStringExtra("author");
        String description = getIntent().getStringExtra("description");
        String category = getIntent().getStringExtra("category");

        UserBook userBook = new UserBook(
                tvTitle.getText().toString(),
                status,
                imageUrl,
                timestamp,
                author,
                description,
                category
        );

        userBooksRef.child(bookId).setValue(userBook).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                //Toast.makeText(this, "Book added to " + status, Toast.LENGTH_SHORT).show();
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("stats");
                if (status.equals("Read")) {
                    int baselineBits = BadgeMilestoneHelper.getStoredBadgeBits(getApplicationContext(), uid);
                    int baselineTier = BadgeMilestoneHelper.getStoredBadgeTier(getApplicationContext(), uid);
                    userRef.child("completed").get().addOnSuccessListener(snapshot -> {
                        long completed = readLeafLong(snapshot);
                        userRef.child("completed").setValue(completed + 1).addOnCompleteListener(setTask -> {
                            if (!setTask.isSuccessful()) {
                                return;
                            }
                            FirebaseDatabase.getInstance().getReference("users").child(uid).child("stats").get()
                                    .addOnCompleteListener(statGet -> {
                                        if (isFinishing()) {
                                            return;
                                        }
                                        if (!statGet.isSuccessful() || statGet.getResult() == null) {
                                            return;
                                        }
                                        BadgeMilestoneHelper.runAfterStatsCelebrations(
                                                BookDetailActivity.this,
                                                getApplicationContext(),
                                                uid,
                                                statGet.getResult(),
                                                () -> { },
                                                baselineBits,
                                                baselineTier);
                                    });
                        });
                    });
                } else if (status.equals("Currently Reading")) {
                    userRef.child("reading").get().addOnSuccessListener(snapshot -> {
                        int reading = 0;
                        if (snapshot.getValue() != null) {
                            Integer readingValue = snapshot.getValue(Integer.class);
                            if (readingValue != null) {
                                reading = readingValue;
                            }
                        }
                        userRef.child("reading").setValue(reading + 1);
                    });
                }
            } else {
                Toast.makeText(this, "Failed to add book", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static long readLeafLong(@Nullable DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists() || snapshot.getValue() == null) {
            return 0L;
        }
        Long l = snapshot.getValue(Long.class);
        if (l != null) {
            return l;
        }
        Integer i = snapshot.getValue(Integer.class);
        return i != null ? i.longValue() : 0L;
    }

    private void incrementReaction(String reactionType) {
        String uid = AuthManager.getUid();
        if (uid == null) return;

        String[] reactions = {"like", "fire", "heart", "sad", "angry"};
        DatabaseReference selectedRef = reactionsRef.child(reactionType).child(uid);

        selectedRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            boolean alreadySelected = task.getResult().exists();
            for (String type : reactions) {
                reactionsRef.child(type).child(uid).removeValue();
            }
            if (!alreadySelected) {
                selectedRef.setValue(true);
            }
        });
    }
    private void selectStatus(Button selected, Button ... buttons) {
        for (Button btn : buttons) {
            btn.setBackgroundTintList(getColorStateList(android.R.color.white));
            btn.setTextColor(getColor(android.R.color.black));
        }

        // highlight selected status button
        selected.setBackgroundTintList(getColorStateList(R.color.navyBlue));
        selected.setTextColor(getColor(android.R.color.white));
    }

    private void loadBookStatus(Button btnWantToRead, Button btnCurrentlyReading, Button btnCompleted) {
        String uid = AuthManager.getUid();
        if (uid == null) return;

        DatabaseReference userBooksRef = FirebaseDatabase.getInstance()
                .getReference("user_books")
                .child(uid)
                .child(bookId);

        userBooksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);

                    if (status != null) {
                        switch (status) {
                            case "Want to Read":
                                selectStatus(btnWantToRead, btnWantToRead, btnCurrentlyReading, btnCompleted);
                                break;
                            case "Currently Reading":
                                selectStatus(btnCurrentlyReading, btnWantToRead, btnCurrentlyReading, btnCompleted);
                                break;
                            case "Read":
                                selectStatus(btnCompleted, btnWantToRead, btnCurrentlyReading, btnCompleted);
                                break;
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addBookToUserList(String listId) {
        String uid = AuthManager.getUid();
        if (uid == null) return;

        String bookId = getIntent().getStringExtra("bookId");
        if (bookId == null || bookId.trim().isEmpty()) {
            Toast.makeText(this, "Missing book ID.", Toast.LENGTH_SHORT).show();
            return;
        }
        String imageUrl = getIntent().getStringExtra("image");
        if (imageUrl != null && imageUrl.startsWith("http://")) {
            imageUrl = imageUrl.replace("http://", "https://");
        }

        DatabaseReference listBooksRef = FirebaseDatabase.getInstance()
                .getReference("communityLists")
                .child(uid)
                .child(listId)
                .child("books")
                .child(bookId);

        UserBook userBook = new UserBook(
                tvTitle.getText().toString(),
                imageUrl ,
                System.currentTimeMillis(),
                getIntent().getStringExtra("author"),
                getIntent().getStringExtra("description"),
                getIntent().getStringExtra("category")
        );

        listBooksRef.setValue(userBook).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(c, "Book added to the list.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(c, "Failed to add book to the list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOverallRating() {
        reviewRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double total = 0;
                int count = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Integer rating = ds.child("rating").getValue(Integer.class);
                    if (rating != null) {
                        total += rating;
                        count++;
                    }
                }
                if (count == 0) {
                    tvOverallRating.setText(R.string.no_ratings);
                    return;
                }
                double avg = total / count;
                String formatted = String.format(Locale.US, "%.1f (%d)", avg, count);

                tvOverallRating.setText(formatted);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvOverallRating.setText("0.0");
            }
        });
    }

    private void attachKeyboardScrollPadding(View scroll, View content) {
        final int pl = content.getPaddingLeft();
        final int pt = content.getPaddingTop();
        final int pr = content.getPaddingRight();
        final int pbBase = content.getPaddingBottom();
        final View root = scroll.getRootView();
        final float density = getResources().getDisplayMetrics().density;
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            scroll.getWindowVisibleDisplayFrame(r);
            int rootH = root.getHeight();
            int keypad = Math.max(0, rootH - r.bottom);
            int slack = (int) (24 * density);
            int minKeyboard = Math.max((int) (rootH * 0.13f), (int) (180 * density));
            int extra = keypad > minKeyboard ? keypad + slack : 0;
            int want = pbBase + extra;
            if (content.getPaddingBottom() != want) {
                content.setPadding(pl, pt, pr, want);
            }
        });
    }

    private void ensureFieldVisibleScroll(View scrollView, View field) {
        final int gapPx = (int) (48 * getResources().getDisplayMetrics().density);
        field.post(() -> {
            int[] fLoc = new int[2];
            int[] sLoc = new int[2];
            field.getLocationOnScreen(fLoc);
            scrollView.getLocationOnScreen(sLoc);
            int fieldBottom = fLoc[1] + field.getHeight();
            int visibleBottom = sLoc[1] + scrollView.getHeight() - scrollView.getPaddingBottom();
            int delta = fieldBottom - visibleBottom + gapPx;
            if (delta <= 0) {
                return;
            }
            if (scrollView instanceof ScrollView) {
                ((ScrollView) scrollView).smoothScrollBy(0, delta);
            } else if (scrollView instanceof NestedScrollView) {
                ((NestedScrollView) scrollView).smoothScrollBy(0, delta);
            }
        });
    }
}
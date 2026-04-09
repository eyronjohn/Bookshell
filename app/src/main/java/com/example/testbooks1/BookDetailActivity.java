package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Adapter.ReviewAdapter;
import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.Review;
import com.example.testbooks1.Model.UserBook;
import com.example.testbooks1.Model.UserManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class BookDetailActivity extends AppCompatActivity {
    TextView tvTitle, tvAuthor, tvDescription, tvCategory, tvNoReviews;
    ShapeableImageView ivBook;
    DatabaseReference communityRef, reactionsRef, reviewRef;
    RatingBar ratingBar;
    EditText etReview;
    Button btnPreview, btnCreateList, btnAddToList, btnSubmitReview;
    Button btnWantToRead, btnCurrentlyReading, btnCompleted;
    RecyclerView rvReviews;
    ImageView btnBack, btnLike, btnFire, btnHeart, btnSad, btnAngry;
    TextView tvLikeCount, tvFireCount, tvHeartCount, tvSadCount, tvAngryCount;
    int likeCount = 0, fireCount = 0, heartCount = 0, sadCount = 0, angryCount = 0;
    String bookId;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);
        c = this;
        initialize();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int originalLeft = v.getPaddingLeft();
            int originalTop = v.getPaddingTop();
            int originalRight = v.getPaddingRight();
            int originalBottom = v.getPaddingBottom();
            v.setPadding(
                    originalLeft + systemBars.left,
                    originalTop + systemBars.top,
                    originalRight + systemBars.right,
                    originalBottom + systemBars.bottom
            );
            return insets;
        });
    }

    public void initialize(){
        ivBook = findViewById(R.id.ivDetailImage);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDescription = findViewById(R.id.tvDescription);
        tvCategory = findViewById(R.id.tvCategory);
        tvNoReviews = findViewById(R.id.tvNoReviews);

        //etListTitle = findViewById(R.id.etListTitle);
        //btnAddToCommunity = findViewById(R.id.btnAddToCommunity);
        btnWantToRead = findViewById(R.id.btnWantToRead);
        btnCurrentlyReading = findViewById(R.id.btnCurrentlyReading);
        btnCompleted = findViewById(R.id.btnCompleted);

        communityRef = FirebaseDatabase.getInstance().getReference("communityLists");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        ratingBar = findViewById(R.id.ratingBar);
        etReview = findViewById(R.id.etReview);

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
        loadBookStatus(btnWantToRead, btnCurrentlyReading, btnCompleted);

        reviewRef = FirebaseDatabase.getInstance()
                .getReference("reviews")
                .child(bookId);


        String title = getIntent().getStringExtra("title");
        String image = getIntent().getStringExtra("image");
        String author = getIntent().getStringExtra("author");
        String description = getIntent().getStringExtra("description");
        String category = getIntent().getStringExtra("category");

        tvTitle.setText(title);
        tvAuthor.setText(author != null ? author : "Unknown");
        tvDescription.setText(description != null ? description : "No description available");
        tvCategory.setText(category);

        Glide.with(this)
                .load(image)
                .into(ivBook);

        reactionsRef = FirebaseDatabase.getInstance()
                .getReference("book_reactions")
                .child(bookId);

        // real-time listener for reaction counts
        /*
        reactionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                likeCount = snapshot.child("like").getValue(Integer.class) != null ? snapshot.child("like").getValue(Integer.class) : 0;
                fireCount = snapshot.child("fire").getValue(Integer.class) != null ? snapshot.child("fire").getValue(Integer.class) : 0;
                heartCount = snapshot.child("heart").getValue(Integer.class) != null ? snapshot.child("heart").getValue(Integer.class) : 0;
                sadCount = snapshot.child("sad").getValue(Integer.class) != null ? snapshot.child("sad").getValue(Integer.class) : 0;
                angryCount = snapshot.child("angry").getValue(Integer.class) != null ? snapshot.child("angry").getValue(Integer.class) : 0;

                tvLikeCount.setText(String.valueOf(likeCount));
                tvFireCount.setText(String.valueOf(fireCount));
                tvHeartCount.setText(String.valueOf(heartCount));
                tvSadCount.setText(String.valueOf(sadCount));
                tvAngryCount.setText(String.valueOf(angryCount));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(BookDetailActivity.this, "Failed to load reactions", Toast.LENGTH_SHORT).show();
            }
        });
        */
        reactionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
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
            public void onCancelled(DatabaseError error) {}
        });

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(c, MainActivity.class));
        });

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
                public void onDataChange(DataSnapshot snapshot) {
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
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(c, "Failed to load lists", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnLike.setOnClickListener(v -> incrementReaction("like"));
        btnFire.setOnClickListener(v -> incrementReaction("fire"));
        btnHeart.setOnClickListener(v -> incrementReaction("heart"));
        btnSad.setOnClickListener(v -> incrementReaction("sad"));
        btnAngry.setOnClickListener(v -> incrementReaction("angry"));

        btnPreview.setOnClickListener(v -> {
            String previewLink = getIntent().getStringExtra("previewLink");

            if (previewLink != null && !previewLink.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse(previewLink));
                startActivity(intent);
            } else {
                String bookId = getIntent().getStringExtra("bookId");
                if (bookId != null && !bookId.isEmpty()) {
                    String url = "https://books.google.com/books?id=" + bookId + "&printsec=frontcover&dq&hl=en&source=gbs_api";
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(android.net.Uri.parse(url));
                    startActivity(intent);
                } else {
                    Toast.makeText(c, "Preview not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

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

            String comment = etReview.getText().toString().trim();
            int rating = (int) ratingBar.getRating();
            if (comment.isEmpty()) {
                Toast.makeText(c, "Please add a review", Toast.LENGTH_SHORT).show();
                return;
            } else if(rating == 0){
                Toast.makeText(c, "Please add a rating.", Toast.LENGTH_SHORT).show();
                return;
            }

            String reviewId = reviewRef.push().getKey();
            Review review = new Review(
                    uid,
                    fullName,
                    rating,
                    comment,
                    System.currentTimeMillis()
            );

            reviewRef.child(reviewId).setValue(review).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(BookDetailActivity.this, "Review added!", Toast.LENGTH_SHORT).show();
                    etReview.setText("");
                    ratingBar.setRating(0);
                }
            });
        });

        ArrayList<Review> reviewList = new ArrayList<>();
        ReviewAdapter adapter = new ReviewAdapter(this, reviewList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(adapter);

        reviewRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                reviewList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Review r = ds.getValue(Review.class);
                    if (r != null) {
                        reviewList.add(r);
                    }
                }
                adapter.notifyDataSetChanged();
                if (reviewList.isEmpty()) {
                    tvNoReviews.setVisibility(View.VISIBLE);
                    rvReviews.setVisibility(View.GONE);
                } else {
                    tvNoReviews.setVisibility(View.GONE);
                    rvReviews.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(BookDetailActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
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
                    userRef.child("completed").get().addOnSuccessListener(snapshot -> {
                        int completed = 0;
                        if (snapshot.getValue() != null) {
                            completed = snapshot.getValue(Integer.class);
                        }
                        userRef.child("completed").setValue(completed + 1);
                    });
                } else if (status.equals("Currently Reading")) {
                    userRef.child("reading").get().addOnSuccessListener(snapshot -> {
                        int reading = 0;
                        if (snapshot.getValue() != null) {
                            reading = snapshot.getValue(Integer.class);
                        }
                        userRef.child("reading").setValue(reading + 1);
                    });
                }
            } else {
                Toast.makeText(this, "Failed to add book", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
    private void incrementReaction(String reactionType) {
        reactionsRef.child(reactionType).get().addOnCompleteListener(task -> {
            int current = 0;
            if (task.isSuccessful() && task.getResult().exists()) {
                current = task.getResult().getValue(Integer.class);
            }
            reactionsRef.child(reactionType).setValue(current + 1);
        });
    }
     */
    private void incrementReaction(String reactionType) {
        String uid = AuthManager.getUid();
        if (uid == null) return;

        DatabaseReference reactionRef = reactionsRef.child(reactionType).child(uid);
        reactionRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                    reactionRef.setValue(true).addOnCompleteListener(t -> {
                        if (t.isSuccessful()) {
                            //Toast.makeText(c, "Reaction added!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    //Toast.makeText(c, "You already reacted!", Toast.LENGTH_SHORT).show();
                }
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
            public void onDataChange(DataSnapshot snapshot) {
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
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void addBookToUserList(String listId) {
        String uid = AuthManager.getUid();
        if (uid == null) return;

        String bookId = getIntent().getStringExtra("bookId");
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
}
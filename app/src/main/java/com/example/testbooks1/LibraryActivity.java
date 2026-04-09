package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Adapter.UserBooksAdapter;
import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.CommunityBook;
import com.example.testbooks1.Model.UserBook;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;

public class LibraryActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;
    private Context c;

    // UI Elements
    private View currentlyReadingCard, currentlyReadingHeader, weeklyProgressCard;
    private MaterialButton btnAll, btnCurrentlyReadingTab;
    private ImageView ivCurrentBookCover;
    private TextView tvCurrentBookTitle, tvCurrentBookAuthor, tvCurrentBookDescription;
    private MaterialButton btnContinueReading;
    
    private RecyclerView rvWantToRead;
    private UserBooksAdapter wantToReadAdapter;
    private ArrayList<CommunityBook> wantToReadList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_library);
        c = this;
        
        initViews();
        initialize();
        loadLibraryData();
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        currentlyReadingCard = findViewById(R.id.currentlyReadingCard);
        currentlyReadingHeader = findViewById(R.id.currentlyReadingHeader);

        ivCurrentBookCover = findViewById(R.id.ivCurrentBookCover);
        tvCurrentBookTitle = findViewById(R.id.tvCurrentBookTitle);
        tvCurrentBookAuthor = findViewById(R.id.tvCurrentBookAuthor);
        tvCurrentBookDescription = findViewById(R.id.tvCurrentBookDescription);
        btnContinueReading = findViewById(R.id.btnContinueReading);

        btnAll = findViewById(R.id.btn_all);
        btnCurrentlyReadingTab = findViewById(R.id.btn_currently_reading_tab);
        rvWantToRead = findViewById(R.id.rvWantToRead);

        btnAll.setOnClickListener(v -> updateTabUI(true));
        btnCurrentlyReadingTab.setOnClickListener(v -> updateTabUI(false));

        wantToReadList = new ArrayList<>();
        // UserBooksAdapter expects (Context, ArrayList<CommunityBook>, HashSet<String>, OnBookSelectListener)
        // Passing an empty HashSet and null listener to use the adapter's default detail-view behavior.
        wantToReadAdapter = new UserBooksAdapter(c, wantToReadList, new HashSet<>(), null);
        rvWantToRead.setLayoutManager(new GridLayoutManager(this, 2));
        rvWantToRead.setAdapter(wantToReadAdapter);
    }

    private void updateTabUI(boolean isAll) {
        View wantToReadHeader = findViewById(R.id.wantToReadHeader);
        if (isAll) {
            btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.navyBlue)));
            btnAll.setTextColor(getColor(R.color.white));
            btnCurrentlyReadingTab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.white)));
            btnCurrentlyReadingTab.setTextColor(getColor(R.color.darkGray));

            // Show everything
            currentlyReadingHeader.setVisibility(currentlyReadingCard.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
            rvWantToRead.setVisibility(View.VISIBLE);
            wantToReadHeader.setVisibility(View.VISIBLE);
        } else {
            btnCurrentlyReadingTab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.navyBlue)));
            btnCurrentlyReadingTab.setTextColor(getColor(R.color.white));
            btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.white)));
            btnAll.setTextColor(getColor(R.color.darkGray));

            // Show only currently reading
            rvWantToRead.setVisibility(View.GONE);
            wantToReadHeader.setVisibility(View.GONE);
        }
    }

    private void loadLibraryData() {
        String uid = AuthManager.getUid();
        if (uid == null) return;

        DatabaseReference userBooksRef = FirebaseDatabase.getInstance().getReference("user_books").child(uid);

        userBooksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                wantToReadList.clear();
                boolean currentReadingFound = false;

                for (DataSnapshot bookSnapshot : snapshot.getChildren()) {
                    String status = bookSnapshot.child("status").getValue(String.class);
                    
                    // We map the data to CommunityBook as UserBooksAdapter uses it
                    String title = bookSnapshot.child("title").getValue(String.class);
                    String author = bookSnapshot.child("author").getValue(String.class);
                    String imageUrl = bookSnapshot.child("imageUrl").getValue(String.class);
                    String description = bookSnapshot.child("description").getValue(String.class);
                    String category = bookSnapshot.child("category").getValue(String.class);
                    String bookId = bookSnapshot.getKey();

                    CommunityBook book = new CommunityBook(bookId, title, author, imageUrl, category, description, "");

                    if ("Currently Reading".equals(status) && !currentReadingFound) {
                        displayCurrentlyReading(book);
                        currentReadingFound = true;
                    } else if ("Want to Read".equals(status) || "Read".equals(status)) {
                        wantToReadList.add(book);
                    }
                }

                if (!currentReadingFound) {
                    currentlyReadingCard.setVisibility(View.GONE);
                    currentlyReadingHeader.setVisibility(View.GONE);
                } else {
                    currentlyReadingCard.setVisibility(View.VISIBLE);
                    currentlyReadingHeader.setVisibility(View.VISIBLE);
                }

                wantToReadAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayCurrentlyReading(CommunityBook book) {
        tvCurrentBookTitle.setText(book.title);
        tvCurrentBookAuthor.setText("by " + book.author);
        tvCurrentBookDescription.setText(book.description);

        if (c != null && !isFinishing()) {
            Glide.with(c)
                    .load(book.imageUrl)
                    .placeholder(R.drawable.rounded_image_bg)
                    .into(ivCurrentBookCover);
        }

        btnContinueReading.setOnClickListener(v -> {
            Intent intent = new Intent(c, BookDetailActivity.class);
            intent.putExtra("bookId", book.bookId);
            intent.putExtra("title", book.title);
            intent.putExtra("author", book.author);
            intent.putExtra("image", book.imageUrl);
            intent.putExtra("description", book.description);
            intent.putExtra("category", book.category);
            c.startActivity(intent);
        });
    }

    public void initialize(){
        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_library);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(c, MainActivity.class));
                return true;
            } else if (id == R.id.nav_library) {
                //startActivity(new Intent(c, LibraryActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(c, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }
}
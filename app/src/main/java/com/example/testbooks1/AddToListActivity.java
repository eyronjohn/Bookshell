package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Adapter.SelectedBooksAdapter;
import com.example.testbooks1.Adapter.UserBooksAdapter;
import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.CommunityBook;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AddToListActivity extends AppCompatActivity {
    EditText etTitle, etDescription;
    RecyclerView rvSelectedBooks, rvUserBooks;
    ArrayList<CommunityBook> selectedBooks, userBooks;
    SelectedBooksAdapter adapter;
    UserBooksAdapter userBooksAdapter;
    HashSet<String> selectedBookIds;
    CommunityBook firstBookFromIntent;
    MaterialButton btnShare;
    DatabaseReference communityRef;
    ImageView btnBack;
    private ImageView ivCoverImage;
    private Uri coverImageUri;
    private String coverImageBase64;
    BottomNavigationView bottomNav;
    private static final int PICK_IMAGE_REQUEST = 101;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_to_list);
        c = this;
        initialize();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initialize() {
        etTitle = findViewById(R.id.title);
        etDescription = findViewById(R.id.description);
        rvUserBooks = findViewById(R.id.rvUserBooks);
        btnShare = findViewById(R.id.addToCommunity);
        btnBack = findViewById(R.id.btnBack);
        ivCoverImage = findViewById(R.id.ivCoverImage);
        ivCoverImage.setOnClickListener(v -> selectCoverImage());

        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                //startActivity(new Intent(c, HomeActivity.class));
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(c, SearchActivity.class));
                return true;
            } else if (id == R.id.nav_community) {
                startActivity(new Intent(c, CommunityActivity.class));
                return true;
            } else if (id == R.id.nav_library) {
                startActivity(new Intent(c, LibraryActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(c, ProfileActivity.class));
                return true;
            }
            return false;
        });
        selectedBooks = new ArrayList<>();
        userBooks = new ArrayList<>();
        selectedBookIds = new HashSet<>();

//        adapter = new SelectedBooksAdapter(this, selectedBooks);
//        rvSelectedBooks.setLayoutManager(
//                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        );
//        rvSelectedBooks.setAdapter(adapter);
//        rvSelectedBooks.setNestedScrollingEnabled(false);

        /*
        userBooksAdapter = new UserBooksAdapter(
                this,
                userBooks,
                selectedBookIds,
                (book, isSelected) -> {
                    if (isSelected) {
                        selectedBooks.add(book);
                    } else {
                        removeBookFromSelected(book.bookId);
                    }
                    userBooksAdapter.notifyDataSetChanged();
                }
        );

         */
        userBooksAdapter = new UserBooksAdapter(
                this,
                userBooks,
                selectedBookIds,
                (book, isSelected) -> {
                    if (isSelected) {
                        if (!selectedBooks.contains(book)) {
                            if (firstBookFromIntent != null) {
                                selectedBooks.add(1, book);
                            } else {
                                selectedBooks.add(book);
                            }
                        }
                    } else {
                        removeBookFromSelected(book.bookId);
                    }
                    userBooksAdapter.notifyDataSetChanged();
                }
        );

        rvUserBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        //rvUserBooks.setLayoutManager(new GridLayoutManager(this, 2));
        rvUserBooks.setAdapter(userBooksAdapter);
        rvUserBooks.setNestedScrollingEnabled(true);

        //user = FirebaseAuth.getInstance().getCurrentUser();
        communityRef = FirebaseDatabase.getInstance().getReference("communityLists");

        String firstBookId = getIntent().getStringExtra("bookId");
        if (firstBookId != null) {
            firstBookFromIntent = new CommunityBook(
                    firstBookId,
                    getIntent().getStringExtra("title"),
                    getIntent().getStringExtra("author"),
                    getIntent().getStringExtra("image"),
                    getIntent().getStringExtra("category"),
                    getIntent().getStringExtra("description"),
                    getIntent().getStringExtra("snippet")
            );
            selectedBookIds.add(firstBookId);
            selectedBooks.add(firstBookFromIntent);
        }

        loadUserBooks();
        btnShare.setOnClickListener(v -> saveCommunityList());
        btnBack.setOnClickListener(v -> { finish(); });
        //onBackPressed();
    }

    /*
    private void loadUserBooks() {
        if (user == null) return;
        DatabaseReference userBooksRef = FirebaseDatabase.getInstance()
                .getReference("user_books")
                .child(user.getUid());
        userBooksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userBooks.clear();
                if (firstBookFromIntent != null) {
                    userBooks.add(firstBookFromIntent);
                }
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String bookId = ds.getKey();

                    if (firstBookFromIntent != null && firstBookFromIntent.bookId.equals(bookId)) continue;
                    CommunityBook book = new CommunityBook();
                    book.bookId = bookId;
                    book.title = ds.child("bookTitle").getValue(String.class);
                    book.author = ds.child("author").getValue(String.class);
                    book.image = ds.child("imageUrl").getValue(String.class);
                    book.category = ds.child("category").getValue(String.class);
                    book.description = ds.child("description").getValue(String.class);
                    book.normalize();
                    if (book.title != null) {
                        userBooks.add(book);
                    }
                }
                userBooksAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
     */
    private void loadUserBooks() {
        String uid = AuthManager.getUid();
        if (uid == null) return;
        DatabaseReference userBooksRef = FirebaseDatabase.getInstance()
                .getReference("user_books")
                .child(uid);
        userBooksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userBooks.clear();
                if (firstBookFromIntent != null) {
                    userBooks.add(firstBookFromIntent);
                }
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String bookId = ds.getKey();
                    if (firstBookFromIntent != null && firstBookFromIntent.bookId.equals(bookId)) continue;

                    CommunityBook book = new CommunityBook();
                    book.bookId = bookId;
                    book.title = ds.child("title").getValue(String.class);
                    book.author = ds.child("author").getValue(String.class);
                    book.imageUrl = ds.child("imageUrl").getValue(String.class);
                    book.category = ds.child("category").getValue(String.class);
                    book.description = ds.child("description").getValue(String.class);
                    if (book.title != null) {
                        userBooks.add(book);
                    }
                }
                userBooksAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveCommunityList() {
        String uid = AuthManager.getUid();
        if (uid == null) return;

        if (selectedBooks.size() < 2) {
            Toast.makeText(this, "Please select at least 2 books.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title.", Toast.LENGTH_SHORT).show();
            return;
        } if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description.", Toast.LENGTH_SHORT).show();
            return;
        }

        String listId = communityRef.child(uid).push().getKey();
        DatabaseReference listRef = communityRef.child(uid).child(listId);
        listRef.child("title").setValue(title);
        listRef.child("description").setValue(description);
        listRef.child("timestamp").setValue(System.currentTimeMillis());
        listRef.child("reactionCount").setValue(0);
        listRef.child("reactions").setValue(new HashMap<>());

        if (coverImageBase64 != null && !coverImageBase64.isEmpty()) {
            listRef.child("coverImage").setValue(coverImageBase64);
        }

        for (CommunityBook book : selectedBooks) {
            // finalTitle = book.title != null ? book.title : book.bookTitle;
            //String finalImage = book.image != null ? book.image : book.imageUrl;
            HashMap<String, Object> map = new HashMap<>();
            map.put("title", book.title);
            map.put("author", book.author);
            map.put("imageUrl", book.imageUrl);
            map.put("category", book.category);
            map.put("description", book.description);
            listRef.child("books").child(book.bookId).setValue(map);
        }
        Toast.makeText(c, "List shared successfully.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void removeBookFromSelected(String bookId) {
        for (int i = 0; i < selectedBooks.size(); i++) {
            if (selectedBooks.get(i).bookId.equals(bookId)) {
                selectedBooks.remove(i);
                return;
            }
        }
    }

    private void selectCoverImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            coverImageUri = data.getData();
            ivCoverImage.setImageURI(coverImageUri);
            findViewById(R.id.tvCoverHint).setVisibility(View.GONE);

            convertToBase64(coverImageUri);
        }
    }
    private void convertToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            Bitmap bitmap = Bitmap.createScaledBitmap(original, 600, 400, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);

            byte[] imageBytes = baos.toByteArray();
            if (imageBytes.length > 200000) {
                Toast.makeText(this, "Image too large, please choose a smaller one", Toast.LENGTH_SHORT).show();
                return;
            }
            coverImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }
}
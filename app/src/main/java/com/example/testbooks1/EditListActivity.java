package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.EditText;
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

import com.example.testbooks1.Adapter.UserBooksAdapter;
import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.CommunityBook;
import com.google.android.material.button.MaterialButton;
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

public class EditListActivity extends AppCompatActivity {

    EditText etTitle, etDescription;
    ImageView ivCoverImage, btnBack;
    TextView tvCoverHint;
    RecyclerView rvUserBooks;
    MaterialButton btnSave;
    Uri coverImageUri;
    String coverImageBase64;
    ArrayList<CommunityBook> selectedBooks = new ArrayList<>();
    ArrayList<CommunityBook> combinedBooks = new ArrayList<>();
    HashSet<String> selectedBookIds = new HashSet<>();
    UserBooksAdapter userBooksAdapter;
    DatabaseReference communityRef;
    //FirebaseUser user;
    String listId;
    Context c;
    static final int PICK_IMAGE_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_list);
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
        ivCoverImage = findViewById(R.id.ivCoverImage);
        tvCoverHint = findViewById(R.id.tvCoverHint);
        rvUserBooks = findViewById(R.id.rvUserBooks);
        btnSave = findViewById(R.id.addToCommunity);
        btnBack = findViewById(R.id.btnBack);

        userBooksAdapter = new UserBooksAdapter(this, combinedBooks, selectedBookIds,
                (book, isSelected) -> {
                    if (isSelected) addBookToSelected(book);
                    else removeBookFromSelected(book.bookId);
                    userBooksAdapter.notifyDataSetChanged();
                });

        rvUserBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvUserBooks.setAdapter(userBooksAdapter);

        communityRef = FirebaseDatabase.getInstance().getReference("communityLists");
        listId = getIntent().getStringExtra("listId");

        loadListData();
        //loadUserBooks();

        ivCoverImage.setOnClickListener(v -> selectCoverImage());
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void loadListData() {
        String uid = AuthManager.getUid();
        if (uid == null || listId == null) return;

        communityRef.child(uid).child(listId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        combinedBooks.clear();
                        selectedBooks.clear();
                        selectedBookIds.clear();

                        etTitle.setText(snapshot.child("title").getValue(String.class));
                        etDescription.setText(snapshot.child("description").getValue(String.class));

                        String cover = snapshot.child("coverImage").getValue(String.class);
                        if (cover != null) {
                            tvCoverHint.setVisibility(TextView.GONE);
                            byte[] decoded = Base64.decode(cover, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            ivCoverImage.setImageBitmap(bitmap);
                            coverImageBase64 = cover;
                        }

                        // community books
                        for (DataSnapshot bookSnap : snapshot.child("books").getChildren()) {
                            CommunityBook book = bookSnap.getValue(CommunityBook.class);
                            if (book != null) {
                                book.bookId = bookSnap.getKey();
                                addBookToSelected(book);
                                addBookToCombined(book);
                            }
                        }
                        //userBooksAdapter.setSelectedBookIds(selectedBookIds);
                        userBooksAdapter.notifyDataSetChanged();
                        loadUserBooks();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadUserBooks() {
        String uid = AuthManager.getUid();
        if (uid == null) return;
        DatabaseReference userBooksRef = FirebaseDatabase.getInstance()
                .getReference("user_books")
                .child(uid);

        userBooksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String bookId = ds.getKey();
                    if (selectedBookIds.contains(bookId)) continue;

                    CommunityBook book = new CommunityBook();
                    book.bookId = bookId;
                    book.title = ds.child("title").getValue(String.class);
                    book.author = ds.child("author").getValue(String.class);
                    book.imageUrl = ds.child("imageUrl").getValue(String.class);
                    book.category = ds.child("category").getValue(String.class);
                    book.description = ds.child("description").getValue(String.class);
                    addBookToCombined(book);
                }
                userBooksAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addBookToSelected(CommunityBook book) {
        if (!selectedBookIds.contains(book.bookId)) {
            selectedBooks.add(book);
            selectedBookIds.add(book.bookId);
        }
        addBookToCombined(book);
    }

    private void addBookToCombined(CommunityBook book) {
        boolean exists = false;
        for (CommunityBook b : combinedBooks) {
            if (b.bookId.equals(book.bookId)) {
                exists = true;
                break;
            }
        }
        if (!exists) combinedBooks.add(book);
    }

    private void removeBookFromSelected(String bookId) {
        for (int i = 0; i < selectedBooks.size(); i++) {
            if (selectedBooks.get(i).bookId.equals(bookId)) {
                selectedBooks.remove(i);
                selectedBookIds.remove(bookId);
                break;
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
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            coverImageUri = data.getData();
            ivCoverImage.setImageURI(coverImageUri);
            convertToBase64(coverImageUri);
        }
    }

    private void convertToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            bitmap = Bitmap.createScaledBitmap(bitmap, 600, 400, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            coverImageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(c, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveChanges() {
        String uid = AuthManager.getUid();
        if (uid == null || listId == null) return;

        if (selectedBookIds.size() < 2) {
            Toast.makeText(c, "Please select at least 2 books.", Toast.LENGTH_SHORT).show();
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

        DatabaseReference listRef = communityRef.child(uid).child(listId);
        listRef.child("title").setValue(title);
        listRef.child("description").setValue(description);
        if (coverImageBase64 != null) listRef.child("coverImage").setValue(coverImageBase64);

        listRef.child("books").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                HashSet<String> existingIds = new HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    existingIds.add(ds.getKey());
                }

                //remove unselected books
                for (String existingId : existingIds) {
                    if (!selectedBookIds.contains(existingId)) {
                        listRef.child("books").child(existingId).removeValue();
                    }
                }

                //add new selected books
                for (CommunityBook book : combinedBooks) {
                    if (!selectedBookIds.contains(book.bookId)) continue;
                    if (!existingIds.contains(book.bookId)) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("title", book.title);
                        map.put("author", book.author);
                        map.put("imageUrl", book.imageUrl);
                        map.put("category", book.category);
                        map.put("description", book.description);
                        listRef.child("books").child(book.bookId).setValue(map);
                    }
                }
                Toast.makeText(c, "List updated successfully.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
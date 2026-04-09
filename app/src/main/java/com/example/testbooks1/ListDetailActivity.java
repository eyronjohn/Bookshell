package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testbooks1.Adapter.BookAdapter;
import com.example.testbooks1.Adapter.BookCollectionAdapter;
import com.example.testbooks1.Adapter.CommentAdapter;
import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.Book;
import com.example.testbooks1.Model.Comment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class ListDetailActivity extends AppCompatActivity {

    RecyclerView recyclerView, recyclerComments;
    ArrayList<Book> bookList;
    BookCollectionAdapter adapter;
    TextView tvListTitle, tvListDescription;
    String userId;
    ArrayList<Comment> commentList;
    CommentAdapter commentAdapter;
    EditText etComment;
    ImageView btnSubmitComment, btnBack;
    DatabaseReference listRef;
    BottomNavigationView bottomNav;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_detail);
        c = this;
        initialize();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void initialize(){
        tvListTitle = findViewById(R.id.tvListTitle);
        tvListDescription = findViewById(R.id.tvListDescription);
        tvListTitle.setText(getIntent().getStringExtra("listTitle"));
        tvListDescription.setText(getIntent().getStringExtra("listDescription"));
        etComment = findViewById(R.id.etComment);
        btnSubmitComment = findViewById(R.id.btnSubmitComment);
        btnBack = findViewById(R.id.btnBack);

        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_community);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(c, MainActivity.class));
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(c, SearchActivity.class));
                return true;
            } else if (id == R.id.nav_community) {
                //startActivity(new Intent(c, CommunityActivity.class));
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

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            Toast.makeText(this, "Missing userId!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerBooks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        bookList = new ArrayList<>();
        adapter = new BookCollectionAdapter(this, bookList);
        recyclerView.setAdapter(adapter);

        loadBooks();

        recyclerComments = findViewById(R.id.recyclerComments);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentList);
        recyclerComments.setAdapter(commentAdapter);

        String listId = getIntent().getStringExtra("listId");
        listRef = FirebaseDatabase.getInstance().getReference()
                .child("communityLists")
                .child(userId)
                .child(listId);

        loadComments();

        String uid = AuthManager.getUid();
        btnSubmitComment.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (!text.isEmpty() && uid != null) {
                String commentId = listRef.child("comments").push().getKey();

                // fetch full name
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                        .child("users")
                        .child(uid);

                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String fullName = snapshot.child("firstName").getValue(String.class) + " " +
                                snapshot.child("lastName").getValue(String.class);

                        Comment comment = new Comment(
                                commentId,
                                uid,
                                fullName,
                                text,
                                System.currentTimeMillis()
                        );
                        listRef.child("comments").child(commentId).setValue(comment);
                        etComment.setText("");
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ListDetailActivity.this, "Failed to get user name", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        btnBack.setOnClickListener(v -> { finish(); });
    }

    private void loadBooks() {
        String listId = getIntent().getStringExtra("listId");

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child("communityLists")
                .child(userId)
                .child(listId)
                .child("books")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        bookList.clear();
                        for (DataSnapshot bookSnap : snapshot.getChildren()) {

                            Book book = bookSnap.getValue(Book.class);
                            if (book != null) {
                                if (bookSnap.child("image").exists()) {
                                    book.setImageUrl(bookSnap.child("image").getValue(String.class));
                                } else if (bookSnap.child("imageUrl").exists()) {
                                    book.setImageUrl(bookSnap.child("imageUrl").getValue(String.class));
                                }
                                book.setId(bookSnap.getKey());
                                bookList.add(book);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(c, "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadComments() {
        listRef.child("comments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Comment c = snap.getValue(Comment.class);
                    if (c != null) commentList.add(c);
                }
                commentAdapter.notifyDataSetChanged();
                recyclerComments.scrollToPosition(commentList.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
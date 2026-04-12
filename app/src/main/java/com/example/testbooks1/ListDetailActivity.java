package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.annotation.SuppressLint;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testbooks1.Adapter.BookCollectionAdapter;
import com.example.testbooks1.Adapter.CommentAdapter;
import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.Book;
import com.example.testbooks1.Model.Comment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

@SuppressLint("NotifyDataSetChanged")
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
        attachKeyboardScrollPadding(findViewById(R.id.scrollView), findViewById(R.id.list_detail_root_content));
    }

    public void initialize(){
        tvListTitle = findViewById(R.id.tvListTitle);
        tvListDescription = findViewById(R.id.tvListDescription);
        tvListTitle.setText(getIntent().getStringExtra("listTitle"));
        tvListDescription.setText(getIntent().getStringExtra("listDescription"));
        etComment = findViewById(R.id.etComment);
        View listScroll = findViewById(R.id.scrollView);
        etComment.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ensureFieldVisibleScroll(listScroll, v);
            }
        });
        btnSubmitComment = findViewById(R.id.btnSubmitComment);
        btnBack = findViewById(R.id.btnBack);

        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_community);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                MainActivity.openHome(c);
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
        if (listId == null) {
            Toast.makeText(this, "Missing listId!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
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
                if (commentId == null) {
                    return;
                }

                // fetch full name
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                        .child("users")
                        .child(uid);

                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String lastName = snapshot.child("lastName").getValue(String.class);
                        String fullName = ((firstName != null ? firstName : "") + " "
                                + (lastName != null ? lastName : "")).trim();
                        if (fullName.isEmpty()) {
                            fullName = "Anonymous";
                        }

                        Comment comment = new Comment(
                                commentId,
                                uid,
                                fullName,
                                text,
                                System.currentTimeMillis()
                        );
                        listRef.child("comments").child(commentId).setValue(comment).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                etComment.setText("");
                            } else {
                                Toast.makeText(ListDetailActivity.this, R.string.toast_comment_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ListDetailActivity.this, "Failed to get user name", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        if (commentAdapter != null) {
            commentAdapter.detachUserProfileListeners();
        }
        super.onDestroy();
    }

    private void loadBooks() {
        String listId = getIntent().getStringExtra("listId");
        if (listId == null || listId.isEmpty()) {
            return;
        }

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child("communityLists")
                .child(userId)
                .child(listId)
                .child("books")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
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
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(c, "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadComments() {
        listRef.child("comments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Comment c = snap.getValue(Comment.class);
                    if (c != null) commentList.add(c);
                }
                commentAdapter.notifyDataSetChanged();
                recyclerComments.scrollToPosition(commentList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
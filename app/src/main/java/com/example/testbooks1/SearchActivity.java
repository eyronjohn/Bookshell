package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.annotation.SuppressLint;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testbooks1.Adapter.BookHorizontalAdapter;
import com.example.testbooks1.Model.Book;
import com.example.testbooks1.Model.BookCount;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("NotifyDataSetChanged")
public class SearchActivity extends AppCompatActivity {

    EditText etSearch;
    TextView tvNoRecent, tvNoMostRead;
    RecyclerView rvBooks, rvRecent;
    BookHorizontalAdapter adapter, recentAdapter;
    List<Book> recentList, bookList;
    BottomNavigationView bottomNav;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);
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
    }

    public void initialize(){
        etSearch = findViewById(R.id.etSearch);
        tvNoRecent = findViewById(R.id.tvNoRecent);
        tvNoMostRead = findViewById(R.id.tvNoMostRead);
        rvBooks = findViewById(R.id.rvBooks);
        bookList = new ArrayList<>();
        rvRecent = findViewById(R.id.rvRecent);
        recentList = new ArrayList<>();

        //rvBooks.setLayoutManager(new GridLayoutManager(this, 2));
        LinearLayoutManager layoutManager = new LinearLayoutManager(c, LinearLayoutManager.HORIZONTAL, false);
        rvBooks.setLayoutManager(layoutManager);
        adapter = new BookHorizontalAdapter(c, (ArrayList<Book>) bookList);
        rvBooks.setAdapter(adapter);

        LinearLayoutManager layoutManager2 = new LinearLayoutManager(c, LinearLayoutManager.HORIZONTAL, false);
        rvRecent.setLayoutManager(layoutManager2);
        recentAdapter = new BookHorizontalAdapter(c, (ArrayList<Book>) recentList);
        rvRecent.setAdapter(recentAdapter);

        loadRecentBooks();
        loadMostReadBooks();

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                openSearchPage(query);
            }
            return true;
        });

        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_search);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                MainActivity.openHome(c);
                return true;
            } else if (id == R.id.nav_search) {
                //startActivity(new Intent(c, SearchActivity.class));
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
    }

    private void loadMostReadBooks() {
        DatabaseReference userBooksRef = FirebaseDatabase.getInstance().getReference("user_books");

        userBooksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, BookCount> bookMap = new HashMap<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot bookSnapshot : userSnapshot.getChildren()) {
                        String status = bookSnapshot.child("status").getValue(String.class);
                        if (status != null && (status.equals("Read"))) {
                            String bookId = bookSnapshot.getKey();
                            String title = bookSnapshot.child("title").getValue(String.class);
                            String imageUrl = bookSnapshot.child("imageUrl").getValue(String.class);
                            String author = bookSnapshot.child("author").getValue(String.class);
                            String description = bookSnapshot.child("description").getValue(String.class);
                            String publisher = bookSnapshot.child("publisher").getValue(String.class);
                            String category = bookSnapshot.child("category").getValue(String.class);
                            String readerLink = bookSnapshot.child("readerLink").getValue(String.class);

                            if (bookId != null && title != null) {
                                BookCount existing = bookMap.get(bookId);
                                if (existing != null) {
                                    existing.count++;
                                } else {
                                    bookMap.put(bookId, new BookCount(
                                            bookId, title, imageUrl, author,
                                            description, publisher, category, readerLink, 1));
                                }
                            }
                        }
                    }
                }

                // Sort by read count descending
                List<BookCount> sortedList = new ArrayList<>(bookMap.values());
                sortedList.sort((a, b) -> b.count - a.count);
                bookList.clear();
                for (BookCount bc : sortedList) {
                    bookList.add(new Book(
                            bc.bookId, bc.title, bc.imageUrl, bc.author,
                            bc.description, bc.publisher, bc.category, bc.readerLink));
                }
                adapter.notifyDataSetChanged();

                if (bookList.isEmpty()) {
                    tvNoMostRead.setVisibility(View.VISIBLE);
                    rvBooks.setVisibility(View.GONE);
                } else {
                    tvNoMostRead.setVisibility(View.GONE);
                    rvBooks.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(c, "Failed to load most read books", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecentBooks() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("recently_viewed")
                .child(user.getUid());

        ref.orderByChild("timestamp").limitToLast(10)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Book> bestById = new HashMap<>();
                        Map<String, Long> bestTs = new HashMap<>();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Book book = ds.getValue(Book.class);
                            if (book == null) {
                                continue;
                            }
                            String id = book.getId();
                            if (id == null || id.isEmpty()) {
                                id = ds.getKey();
                            }
                            if (id == null || id.isEmpty()) {
                                continue;
                            }
                            Long ts = ds.child("timestamp").getValue(Long.class);
                            if (ts == null) {
                                ts = 0L;
                            }
                            Long prev = bestTs.get(id);
                            if (prev == null || ts >= prev) {
                                if (book.getId() == null || book.getId().isEmpty()) {
                                    book.setId(id);
                                }
                                bestById.put(id, book);
                                bestTs.put(id, ts);
                            }
                        }

                        List<String> ids = new ArrayList<>(bestById.keySet());
                        ids.sort((a, b) -> {
                            Long xb = bestTs.get(b);
                            Long xa = bestTs.get(a);
                            long lb = xb != null ? xb : 0L;
                            long la = xa != null ? xa : 0L;
                            return Long.compare(lb, la);
                        });

                        recentList.clear();
                        for (String id : ids) {
                            recentList.add(bestById.get(id));
                            if (recentList.size() >= 10) {
                                break;
                            }
                        }
                        recentAdapter.notifyDataSetChanged();

                        if (recentList.isEmpty()) {
                            tvNoRecent.setVisibility(View.VISIBLE);
                            rvRecent.setVisibility(View.GONE);
                        } else {
                            tvNoRecent.setVisibility(View.GONE);
                            rvRecent.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void openSearchPage(String query) {
        Intent intent = new Intent(this, SearchPageActivity.class);
        intent.putExtra("query", query);
        startActivity(intent);
    }
}
package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.annotation.SuppressLint;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testbooks1.Adapter.CommunityAdapter;
import com.example.testbooks1.Model.CommunityBook;
import com.example.testbooks1.Model.CommunityItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("NotifyDataSetChanged")
public class CommunityActivity extends AppCompatActivity {

    ProgressBar progress;
    RecyclerView recyclerView;
    ArrayList<CommunityItem> communityList;
    TextView tvNoCommunity;
    CommunityAdapter adapter;
    BottomNavigationView bottomNav;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.item_community_list);
        c = this;
        initialize();
        loadCommunityData();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void initialize() {
        progress = findViewById(R.id.progressBooks);
        tvNoCommunity = findViewById(R.id.tvNoCommunity);
        recyclerView = findViewById(R.id.communityRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(c));
        communityList = new ArrayList<>();
        adapter = new CommunityAdapter(c, communityList);
        recyclerView.setAdapter(adapter);
        findViewById(R.id.fabCreateList).setOnClickListener(
                v -> startActivity(new Intent(c, CreateListActivity.class)));
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
    }

    @Override
    protected void onDestroy() {
        if (adapter != null) {
            adapter.detachUserProfileListeners();
        }
        super.onDestroy();
    }

    private void loadCommunityData() {
        progress.setVisibility(View.VISIBLE);
        tvNoCommunity.setVisibility(View.GONE);

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child("communityLists").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                communityList.clear();
                adapter.notifyDataSetChanged();

                ArrayList<DataSnapshot> allListSnaps = new ArrayList<>();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    for (DataSnapshot listSnap : userSnap.getChildren()) {
                        allListSnaps.add(listSnap);
                    }
                }

                final int totalLists = allListSnaps.size();

                if (totalLists == 0) {
                    progress.setVisibility(View.GONE);
                    tvNoCommunity.setVisibility(View.VISIBLE);
                    return;
                }

                final List<CommunityItem> loadedBuffer = new ArrayList<>();
                final int[] loadedCount = {0};

                for (DataSnapshot listSnap : allListSnaps) {
                    String listId = listSnap.getKey();
                    String listTitle = listSnap.child("title").getValue(String.class);
                    String listDescription = listSnap.child("description").getValue(String.class);
                    String coverImage = listSnap.child("coverImage").getValue(String.class);
                    final long listTimestampMs = parseListTimestamp(listSnap);

                    ArrayList<CommunityBook> books = new ArrayList<>();
                    for (DataSnapshot bookSnap : listSnap.child("books").getChildren()) {
                        CommunityBook book = bookSnap.getValue(CommunityBook.class);
                        if (book != null) books.add(book);
                    }

                    String firstBookImage = !books.isEmpty() ? books.get(0).imageUrl : null;
                    String userId = listSnap.getRef().getParent().getKey();
                    final String fUserId = userId;
                    if (userId == null) {
                        synchronized (loadedBuffer) {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalLists) {
                                sortAndShowCommunityFeed(loadedBuffer);
                            }
                        }
                        continue;
                    }

                    db.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userData) {
                            String firstName = userData.child("firstName").getValue(String.class);
                            String lastName = userData.child("lastName").getValue(String.class);
                            String profileImageUrl = userData.child("profileImageUrl").getValue(String.class);

                            CommunityItem item = new CommunityItem();
                            item.userId = fUserId;
                            item.listId = listId;
                            String fn = firstName != null ? firstName : "";
                            String ln = lastName != null ? lastName : "";
                            String fullName = (fn + " " + ln).trim();
                            item.fullName = fullName.isEmpty() ? "Anonymous" : fullName;
                            item.profileImageUrl = profileImageUrl;
                            item.listTitle = listTitle;
                            item.listDescription = listDescription;
                            item.coverImage = coverImage;
                            item.firstBookImage = firstBookImage;
                            item.books = books;
                            item.timestampMs = listTimestampMs;

                            if (listSnap.hasChild("comments")) {
                                item.commentCount = (int) listSnap.child("comments").getChildrenCount();
                            } else {
                                item.commentCount = 0;
                            }
                            synchronized (loadedBuffer) {
                                loadedBuffer.add(item);
                                loadedCount[0]++;
                                if (loadedCount[0] == totalLists) {
                                    sortAndShowCommunityFeed(loadedBuffer);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            CommunityItem fallback = new CommunityItem();
                            fallback.userId = fUserId;
                            fallback.listId = listId;
                            fallback.fullName = "Anonymous";
                            fallback.profileImageUrl = null;
                            fallback.listTitle = listTitle;
                            fallback.listDescription = listDescription;
                            fallback.coverImage = coverImage;
                            fallback.firstBookImage = firstBookImage;
                            fallback.books = books;
                            fallback.timestampMs = listTimestampMs;
                            if (listSnap.hasChild("comments")) {
                                fallback.commentCount = (int) listSnap.child("comments").getChildrenCount();
                            }
                            synchronized (loadedBuffer) {
                                loadedBuffer.add(fallback);
                                loadedCount[0]++;
                                if (loadedCount[0] == totalLists) {
                                    sortAndShowCommunityFeed(loadedBuffer);
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progress.setVisibility(View.GONE);
            }
        });
    }

    private void sortAndShowCommunityFeed(List<CommunityItem> buffer) {
        buffer.sort((a, b) -> {
            int cmp = Long.compare(b.timestampMs, a.timestampMs);
            if (cmp != 0) {
                return cmp;
            }
            String idA = a.listId != null ? a.listId : "";
            String idB = b.listId != null ? b.listId : "";
            return idA.compareTo(idB);
        });
        communityList.clear();
        communityList.addAll(buffer);
        adapter.notifyDataSetChanged();
        progress.setVisibility(View.GONE);
    }

    private static long parseListTimestamp(DataSnapshot listSnap) {
        DataSnapshot ts = listSnap.child("timestamp");
        if (!ts.exists() || ts.getValue() == null) {
            return 0L;
        }
        Object val = ts.getValue();
        if (val instanceof Long) {
            return (Long) val;
        }
        if (val instanceof Integer) {
            return ((Integer) val).longValue();
        }
        if (val instanceof Double) {
            return ((Double) val).longValue();
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return 0L;
    }
}
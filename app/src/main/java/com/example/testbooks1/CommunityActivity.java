package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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

public class CommunityActivity extends AppCompatActivity {

    ProgressBar progress;
    RecyclerView recyclerView;
    ArrayList<CommunityItem> communityList;
    TextView tvNoCommunity;
    CommunityAdapter adapter;
    ImageView btnCreateList;
    BottomNavigationView bottomNav;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.item_community_list);
        c = this;
        initialize();
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
        btnCreateList = findViewById(R.id.btnCreateList);
        loadCommunityData();

        btnCreateList.setOnClickListener(v -> startActivity(new Intent(c, CreateListActivity.class)));
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

    private void loadCommunityData() {
        progress.setVisibility(View.VISIBLE);
        tvNoCommunity.setVisibility(View.GONE);

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child("communityLists").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<DataSnapshot> allListSnaps = new ArrayList<>();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    for (DataSnapshot listSnap : userSnap.getChildren()) {
                        allListSnaps.add(listSnap);
                    }
                }

                final int totalLists = allListSnaps.size();
                final int[] loadedCount = {0};

                if (totalLists == 0) {
                    progress.setVisibility(View.GONE);
                    tvNoCommunity.setVisibility(View.VISIBLE);
                    return;
                }

                for (DataSnapshot listSnap : allListSnaps) {
                    String listId = listSnap.getKey();
                    String listTitle = listSnap.child("title").getValue(String.class);
                    String listDescription = listSnap.child("description").getValue(String.class);
                    String coverImage = listSnap.child("coverImage").getValue(String.class);

                    ArrayList<CommunityBook> books = new ArrayList<>();
                    for (DataSnapshot bookSnap : listSnap.child("books").getChildren()) {
                        CommunityBook book = bookSnap.getValue(CommunityBook.class);
                        if (book != null) books.add(book);
                    }

                    String firstBookImage = books.size() > 0 ? books.get(0).imageUrl : null;
                    String userId = listSnap.getRef().getParent().getKey();
                    final String fUserId = userId;

                    db.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot userData) {
                            String firstName = userData.child("firstName").getValue(String.class);
                            String lastName = userData.child("lastName").getValue(String.class);

                            CommunityItem item = new CommunityItem();
                            item.userId = fUserId;
                            item.listId = listId;
                            item.fullName = firstName + " " + lastName;
                            item.listTitle = listTitle;
                            item.listDescription = listDescription;
                            item.coverImage = coverImage;
                            item.firstBookImage = firstBookImage;
                            item.books = books;

                            if (listSnap.hasChild("comments")) {
                                item.commentCount = (int) listSnap.child("comments").getChildrenCount();
                            } else {
                                item.commentCount = 0;
                            }
                            communityList.add(item);
                            adapter.notifyDataSetChanged();

                            loadedCount[0]++;
                            if (loadedCount[0] == totalLists) {
                                progress.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalLists) {
                                progress.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progress.setVisibility(View.GONE);
            }
        });
    }
}
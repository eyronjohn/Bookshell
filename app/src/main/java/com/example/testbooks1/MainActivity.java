package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.testbooks1.Adapter.BookAdapter;
import com.example.testbooks1.Adapter.BookRecommendedAdapter;
import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.Book;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText etSearch;
    TextView tvCurrentlyReading;
    ShapeableImageView bookImage;
    List<Book> bookList;
    ProgressBar progress;
    RecyclerView rvBooks, rvRecommended;
    BookAdapter adapter;
    BookRecommendedAdapter recommendedAdapter;
    ArrayList<Book> recommendedList;
    LinearLayout recommendedSection;
    BottomNavigationView bottomNav;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        c = this;
        initialize();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void initialize(){
        etSearch = findViewById(R.id.etSearch);
        rvBooks = findViewById(R.id.rvBooks);
        bookList = new ArrayList<>();
        tvCurrentlyReading = findViewById(R.id.tvCurrentlyReading);
        bookImage = findViewById(R.id.bookImage);

        rvBooks.setLayoutManager(new GridLayoutManager(c, 2));
        adapter = new BookAdapter(c, (ArrayList<Book>) bookList);
        rvBooks.setAdapter(adapter);

        rvRecommended = findViewById(R.id.rvRecommended);
        recommendedList = new ArrayList<>();

        rvRecommended.setLayoutManager(new LinearLayoutManager(c, LinearLayoutManager.VERTICAL, false)
        );

        recommendedAdapter = new BookRecommendedAdapter(c, recommendedList);
        rvRecommended.setAdapter(recommendedAdapter);

        progress = findViewById(R.id.progressBooks);
        progress.setVisibility(View.VISIBLE);
        recommendedSection = findViewById(R.id.recommendedSection);
        recommendedSection.setVisibility(View.GONE);

        loadPersonalizedBooks();
        //callBooks("bestseller");

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                openSearchPage(query);
            }
            return true;
        });

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

        //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = AuthManager.getUid();
        if (uid != null) {
            //String uid = user.getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("user_books")
                    .child(uid);

            ref.orderByChild("status")
                    .equalTo("Currently Reading")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Book latestBook = null;
                            long latestTimestamp = 0;

                            for (DataSnapshot ds : snapshot.getChildren()) {
                                Long timestamp = ds.child("timestamp").getValue(Long.class);
                                if (timestamp != null && timestamp > latestTimestamp) {
                                    latestTimestamp = timestamp;
                                    String title = ds.child("title").getValue(String.class);
                                    String imageUrl = ds.child("imageUrl").getValue(String.class);
                                    latestBook = new Book(title, imageUrl);
                                }
                            }

                            if (latestBook != null) {
                                String imageUrl = latestBook.getImageUrl();
                                if (imageUrl.startsWith("http://")) {
                                    imageUrl = imageUrl.replace("http://", "https://");
                                }
                                tvCurrentlyReading.setText(latestBook.getTitle());
                                Glide.with(c)
                                        .load(imageUrl)
                                        .into(bookImage);
                                findViewById(R.id.currentReadingSection).setVisibility(View.VISIBLE);
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                        }
                    });
        }
    }
    /*
    public void callBooks(String queryInput) {
        String query = queryInput.trim().replace(" ", "+");
        String url = "https://www.googleapis.com/books/v1/volumes?q=" + query + "&maxResults=4" + "&key=AIzaSyAycxqRNFLfOCxktkf3cDcWChAc0Cfvk4Y";

        RequestQueue r = Volley.newRequestQueue(c);

        JsonObjectRequest json = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        progress.setVisibility(View.GONE);
                        bookList.clear();
                        if (!response.has("items")) return;

                        JSONArray items = response.getJSONArray("items");

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject bookObj = items.getJSONObject(i);
                            JSONObject volumeInfo = bookObj.getJSONObject("volumeInfo");

                            String id = bookObj.getString("id");
                            String title = volumeInfo.getString("title");
                            String author = "Unknown";
                            if (volumeInfo.has("authors")) {
                                JSONArray authorsArray = volumeInfo.getJSONArray("authors");
                                author = authorsArray.getString(0);
                            }
                            String description = volumeInfo.optString("description", "No description available");
                            String publisher = volumeInfo.optString("publisher", "Unknown");

                            String category = "Unknown";
                            if (volumeInfo.has("categories")) {
                                JSONArray categoriesArray = volumeInfo.getJSONArray("categories");
                                if (categoriesArray.length() > 0) {
                                    category = categoriesArray.getString(0);
                                }
                            }

                            String imageUrl = "";
                            if (volumeInfo.has("imageLinks")) {
                                imageUrl = volumeInfo
                                        .getJSONObject("imageLinks")
                                        .optString("thumbnail", "");
                                if (imageUrl.startsWith("http://")) {
                                    imageUrl = imageUrl.replace("http://", "https://");
                                }
                            }

                            bookList.add(new Book(id, title, imageUrl, author, description, publisher, category));
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(c, error.toString(), Toast.LENGTH_SHORT).show()
        );
        r.add(json);
    }

     */
    public void callBooks(String queryInput) {
        String query = queryInput.trim().replace(" ", "+");
        String url = "https://www.googleapis.com/books/v1/volumes?q=" + query
                + "&maxResults=6"
                + "&printType=books"
                + "&filter=ebooks"
                + "&key=AIzaSyAycxqRNFLfOCxktkf3cDcWChAc0Cfvk4Y";

        RequestQueue r = Volley.newRequestQueue(c);

        JsonObjectRequest json = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        progress.setVisibility(View.GONE);
                        bookList.clear();
                        if (!response.has("items")) return;

                        JSONArray items = response.getJSONArray("items");

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject bookObj = items.getJSONObject(i);
                            JSONObject volumeInfo = bookObj.getJSONObject("volumeInfo");
                            JSONObject accessInfo = bookObj.getJSONObject("accessInfo");

                            String viewability = accessInfo.optString("viewability", "NONE");
                            boolean embeddable = accessInfo.optBoolean("embeddable", false);

                            if (!(viewability.equals("ALL_PAGES") || viewability.equals("PARTIAL")) || !embeddable) {
                                continue;
                            }

                            String readerLink = accessInfo.optString("webReaderLink", "");

                            String id = bookObj.getString("id");
                            String title = volumeInfo.getString("title");

                            String author = "Unknown";
                            if (volumeInfo.has("authors")) {
                                JSONArray authorsArray = volumeInfo.getJSONArray("authors");
                                author = authorsArray.getString(0);
                            }

                            String description = volumeInfo.optString("description", "No description available");
                            String publisher = volumeInfo.optString("publisher", "Unknown");

                            String category = "Unknown";
                            if (volumeInfo.has("categories")) {
                                JSONArray categoriesArray = volumeInfo.getJSONArray("categories");
                                if (categoriesArray.length() > 0) {
                                    category = categoriesArray.getString(0);
                                }
                            }

                            String imageUrl = "";
                            if (volumeInfo.has("imageLinks")) {
                                imageUrl = volumeInfo
                                        .getJSONObject("imageLinks")
                                        .optString("thumbnail", "");
                                if (imageUrl.startsWith("http://")) {
                                    imageUrl = imageUrl.replace("http://", "https://");
                                }
                            }
                            bookList.add(new Book(id, title, imageUrl, author, description, publisher, category, readerLink));
                        }
                        adapter.notifyDataSetChanged();
                        recommendedSection.setVisibility(View.VISIBLE);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(c, error.toString(), Toast.LENGTH_SHORT).show()
        );
        r.add(json);
    }
    public void callRecommendedBooks(String queryInput) {
        String query = queryInput.trim().replace(" ", "+");

        String url = "https://www.googleapis.com/books/v1/volumes?q=" + query
                + "&maxResults=2"
                + "&printType=books"
                + "&filter=ebooks"
                + "&key=AIzaSyAycxqRNFLfOCxktkf3cDcWChAc0Cfvk4Y";

        RequestQueue r = Volley.newRequestQueue(c);

        JsonObjectRequest json = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        recommendedList.clear();

                        if (!response.has("items")) return;

                        JSONArray items = response.getJSONArray("items");

                        int count = 0;

                        for (int i = 0; i < items.length(); i++) {
                            if (count == 2) break;

                            JSONObject bookObj = items.getJSONObject(i);
                            JSONObject volumeInfo = bookObj.getJSONObject("volumeInfo");
                            JSONObject accessInfo = bookObj.getJSONObject("accessInfo");

                            String viewability = accessInfo.optString("viewability", "NONE");
                            boolean embeddable = accessInfo.optBoolean("embeddable", false);

                            if (!(viewability.equals("ALL_PAGES") || viewability.equals("PARTIAL")) || !embeddable) {
                                continue;
                            }

                            String readerLink = accessInfo.optString("webReaderLink", "");

                            String id = bookObj.getString("id");
                            String title = volumeInfo.getString("title");

                            String author = "Unknown";
                            if (volumeInfo.has("authors")) {
                                author = volumeInfo.getJSONArray("authors").getString(0);
                            }

                            String description = volumeInfo.optString("description", "");
                            String publisher = volumeInfo.optString("publisher", "");

                            String category = "Unknown";
                            if (volumeInfo.has("categories")) {
                                category = volumeInfo.getJSONArray("categories").getString(0);
                            }

                            String imageUrl = "";
                            if (volumeInfo.has("imageLinks")) {
                                imageUrl = volumeInfo.getJSONObject("imageLinks")
                                        .optString("thumbnail", "");
                                if (imageUrl.startsWith("http://")) {
                                    imageUrl = imageUrl.replace("http://", "https://");
                                }
                            }

                            recommendedList.add(new Book(id, title, imageUrl, author, description, publisher, category, readerLink));
                            count++;
                        }

                        recommendedAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(c, error.toString(), Toast.LENGTH_SHORT).show()
        );

        r.add(json);
    }
    private void loadPersonalizedBooks() {
        String uid = AuthManager.getUid();
        if (uid == null) {
            callBooks("bestseller");
            return;
        }
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("user_books")
                .child(uid);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                HashMap<String, Integer> categoryCount = new HashMap<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String category = ds.child("category").getValue(String.class);
                    if (category != null && !category.equals("Unknown")) {
                        categoryCount.put(category,
                                categoryCount.getOrDefault(category, 0) + 1);
                    }
                }
                String topCategory = null;
                int max = 0;

                for (String cat : categoryCount.keySet()) {
                    if (categoryCount.get(cat) > max) {
                        max = categoryCount.get(cat);
                        topCategory = cat;
                    }
                }

                if (topCategory != null) {
                    callBooks(topCategory + " popular books");
                    callRecommendedBooks(topCategory);
                } else {
                    callBooks("bestseller");
                    callRecommendedBooks("popular books");
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                callBooks("bestseller");
            }
        });
    }

    private void openSearchPage(String query) {
        Intent intent = new Intent(this, SearchPageActivity.class);
        intent.putExtra("query", query);
        startActivity(intent);
    }
}
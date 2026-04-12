package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
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

@SuppressLint("NotifyDataSetChanged")
public class MainActivity extends AppCompatActivity {

    private static final String VOLLEY_TAG_BOOKS_TODAY = "books_today";
    private static final String VOLLEY_TAG_BOOKS_RECOMMENDED = "books_recommended";

    /** Use when navigating to Home from other screens so we reuse one MainActivity instead of stacking copies. */
    public static void openHome(Context context) {
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(i);
    }

    EditText etSearch;
    TextView tvCurrentlyReading;
    ShapeableImageView bookImage;
    List<Book> bookList;
    ProgressBar progress;
    RecyclerView rvBooks, rvRecommended;
    LinearLayout currentReadingSection;
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

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentlyReadingBanner();
    }

    private void refreshCurrentlyReadingBanner() {
        if (tvCurrentlyReading == null || currentReadingSection == null || bookImage == null) {
            return;
        }
        String uid = AuthManager.getUid();
        if (uid == null) {
            currentReadingSection.setVisibility(View.GONE);
            return;
        }
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("user_books")
                .child(uid);

        ref.orderByChild("status")
                .equalTo("Currently Reading")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
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
                            if (imageUrl != null && imageUrl.startsWith("http://")) {
                                imageUrl = imageUrl.replace("http://", "https://");
                            }
                            tvCurrentlyReading.setText(latestBook.getTitle());
                            Glide.with(MainActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.sample_book)
                                    .error(R.drawable.sample_book)
                                    .into(bookImage);
                            currentReadingSection.setVisibility(View.VISIBLE);
                        } else {
                            tvCurrentlyReading.setText("");
                            currentReadingSection.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    public void initialize(){
        etSearch = findViewById(R.id.etSearch);
        rvBooks = findViewById(R.id.rvBooks);
        bookList = new ArrayList<>();
        tvCurrentlyReading = findViewById(R.id.tvCurrentlyReading);
        currentReadingSection = findViewById(R.id.currentReadingSection);
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
    }
    public void callBooks(String queryInput) {
        callBooksInternal(queryInput.trim(), false);
    }

    private void callBooksInternal(String queryInput, boolean alreadyFallback) {
        String query = queryInput.replace(" ", "+");
        String url = GoogleBooksJson.urlWithBooksApiKey(
                "https://www.googleapis.com/books/v1/volumes?q=" + query
                        + "&maxResults=20"
                        + "&printType=books"
                        + "&filter=ebooks");

        RequestQueue r = Volley.newRequestQueue(c);
        r.cancelAll(VOLLEY_TAG_BOOKS_TODAY);
        JsonObjectRequest json = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (!response.has("items")) {
                            if (!alreadyFallback) {
                                callBooksInternal("bestseller", true);
                                return;
                            }
                            progress.setVisibility(View.GONE);
                            bookList.clear();
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        JSONArray items = response.getJSONArray("items");
                        ArrayList<Book> parsed = new ArrayList<>();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject bookObj = items.getJSONObject(i);
                            JSONObject volumeInfo = bookObj.optJSONObject("volumeInfo");
                            if (volumeInfo == null) {
                                continue;
                            }
                            JSONObject accessInfo = bookObj.optJSONObject("accessInfo");
                            if (accessInfo == null) {
                                continue;
                            }

                            String viewability = accessInfo.optString("viewability", "NONE");
                            boolean embeddable = accessInfo.optBoolean("embeddable", false);

                            if (!(viewability.equals("ALL_PAGES") || viewability.equals("PARTIAL")) || !embeddable) {
                                continue;
                            }

                            String readerLink = accessInfo.optString("webReaderLink", "");

                            String id = bookObj.optString("id", "");
                            if (id.isEmpty()) {
                                continue;
                            }
                            String title = volumeInfo.optString("title", "Untitled");

                            String author = "Unknown";
                            if (volumeInfo.has("authors")) {
                                JSONArray authorsArray = volumeInfo.getJSONArray("authors");
                                if (authorsArray.length() > 0) {
                                    author = authorsArray.getString(0);
                                }
                            }

                            String description = volumeInfo.optString("description", "No description available.");
                            String publisher = volumeInfo.optString("publisher", "Unknown");

                            String category = GoogleBooksJson.pickDisplayCategory(volumeInfo);

                            String imageUrl = "";
                            if (volumeInfo.has("imageLinks")) {
                                imageUrl = volumeInfo
                                        .getJSONObject("imageLinks")
                                        .optString("thumbnail", "https://img.freepik.com/free-vector/red-text-book-closed-icon_18591-82397.jpg?semt=ais_hybrid&w=740&q=80");
                                if (imageUrl.startsWith("http://")) {
                                    imageUrl = imageUrl.replace("http://", "https://");
                                }
                            }
                            parsed.add(new Book(id, title, imageUrl, author, description, publisher, category, readerLink));
                            if (parsed.size() >= 6) {
                                break;
                            }
                        }

                        if (parsed.isEmpty() && !alreadyFallback) {
                            callBooksInternal("bestseller", true);
                            return;
                        }

                        progress.setVisibility(View.GONE);
                        bookList.clear();
                        bookList.addAll(parsed);
                        adapter.notifyDataSetChanged();
                        recommendedSection.setVisibility(View.VISIBLE);
                    } catch (JSONException e) {
                        Log.e("MainActivity", "books_today JSON", e);
                        if (!alreadyFallback) {
                            callBooksInternal("bestseller", true);
                            return;
                        }
                        if (progress != null) {
                            progress.setVisibility(View.GONE);
                        }
                        bookList.clear();
                        adapter.notifyDataSetChanged();
                    }
                },
                error -> {
                    if (!alreadyFallback) {
                        callBooksInternal("bestseller", true);
                        return;
                    }
                    if (progress != null) {
                        progress.setVisibility(View.GONE);
                    }
                    bookList.clear();
                    adapter.notifyDataSetChanged();
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);

                        Log.e("VOLLEY_ERROR", "Status Code: " + statusCode);
                        Log.e("VOLLEY_ERROR", "Response: " + responseData);
                    } else {
                        Log.e("VOLLEY_ERROR", "Error: " + error);
                    }
                }
        );
        json.setTag(VOLLEY_TAG_BOOKS_TODAY);
        r.add(json);
    }
    public void callRecommendedBooks(String queryInput) {
        String query = queryInput.trim().replace(" ", "+");

        String url = GoogleBooksJson.urlWithBooksApiKey(
                "https://www.googleapis.com/books/v1/volumes?q=" + query
                        + "&maxResults=8"
                        + "&printType=books"
                        + "&filter=ebooks");

        RequestQueue r = Volley.newRequestQueue(c);
        r.cancelAll(VOLLEY_TAG_BOOKS_RECOMMENDED);
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
                            JSONObject volumeInfo = bookObj.optJSONObject("volumeInfo");
                            if (volumeInfo == null) {
                                continue;
                            }
                            JSONObject accessInfo = bookObj.optJSONObject("accessInfo");
                            if (accessInfo == null) {
                                continue;
                            }

                            String viewability = accessInfo.optString("viewability", "NONE");
                            boolean embeddable = accessInfo.optBoolean("embeddable", false);

                            if (!(viewability.equals("ALL_PAGES") || viewability.equals("PARTIAL")) || !embeddable) {
                                continue;
                            }

                            String readerLink = accessInfo.optString("webReaderLink", "");

                            String id = bookObj.optString("id", "");
                            if (id.isEmpty()) {
                                continue;
                            }
                            String title = volumeInfo.optString("title", "Untitled");

                            String author = "Unknown";
                            if (volumeInfo.has("authors")) {
                                JSONArray arr = volumeInfo.optJSONArray("authors");
                                if (arr != null && arr.length() > 0) {
                                    author = arr.getString(0);
                                }
                            }

                            String description = volumeInfo.optString("description", "No description available.");
                            String publisher = volumeInfo.optString("publisher", "");

                            String category = GoogleBooksJson.pickDisplayCategory(volumeInfo);

                            String imageUrl = "";
                            if (volumeInfo.has("imageLinks")) {
                                imageUrl = volumeInfo.getJSONObject("imageLinks")
                                        .optString("thumbnail", "https://img.freepik.com/free-vector/red-text-book-closed-icon_18591-82397.jpg?semt=ais_hybrid&w=740&q=80");
                                if (imageUrl.startsWith("http://")) {
                                    imageUrl = imageUrl.replace("http://", "https://");
                                }
                            }

                            recommendedList.add(new Book(id, title, imageUrl, author, description, publisher, category, readerLink));
                            count++;
                        }

                        recommendedAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        Log.e("MainActivity", "books_recommended JSON", e);
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);

                        Log.e("VOLLEY_ERROR", "Status Code: " + statusCode);
                        Log.e("VOLLEY_ERROR", "Response: " + responseData);
                    } else {
                        Log.e("VOLLEY_ERROR", "Error: " + error);
                    }
                }
        );
        json.setTag(VOLLEY_TAG_BOOKS_RECOMMENDED);
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
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, Integer> categoryCount = new HashMap<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String category = ds.child("category").getValue(String.class);
                    if (category != null && !category.equals("Unknown")) {
                        categoryCount.merge(category, 1, Integer::sum);
                    }
                }
                String topCategory = null;
                int max = 0;

                for (String cat : categoryCount.keySet()) {
                    Integer v = categoryCount.get(cat);
                    int count = v != null ? v : 0;
                    if (count > max) {
                        max = count;
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
            public void onCancelled(@NonNull DatabaseError error) {
                callBooks("bestseller");
                callRecommendedBooks("popular books");
            }
        });
    }

    private void openSearchPage(String query) {
        Intent intent = new Intent(this, SearchPageActivity.class);
        intent.putExtra("query", query);
        startActivity(intent);
    }
}
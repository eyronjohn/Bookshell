package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.annotation.SuppressLint;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.testbooks1.Adapter.BookAdapter;
import com.example.testbooks1.Model.Book;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("NotifyDataSetChanged")
public class SearchPageActivity extends AppCompatActivity {

    EditText etSearch;
    TextView tvNoResults;
    ProgressBar progress;
    RecyclerView rvBooks;
    BookAdapter adapter;
    List<Book> bookList;
    BottomNavigationView bottomNav;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_page);
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
        tvNoResults = findViewById(R.id.tvNoResults);
        rvBooks = findViewById(R.id.rvBooks);
        bookList = new ArrayList<>();
        rvBooks.setLayoutManager(new GridLayoutManager(c, 2));
        adapter = new BookAdapter(c, (ArrayList<Book>) bookList);
        rvBooks.setAdapter(adapter);
        progress = findViewById(R.id.progressBooks);
        progress.setVisibility(View.VISIBLE);

        String query = getIntent().getStringExtra("query");
        if (query != null && !query.isEmpty()) {
            etSearch.setText(query);
            callBooks(query);
        }

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String searchQuery = etSearch.getText().toString().trim();
            if (searchQuery.isEmpty()) {
                Toast.makeText(c, R.string.toast_enter_search_query, Toast.LENGTH_SHORT).show();
            } else {
                progress.setVisibility(View.VISIBLE);
                callBooks(searchQuery);
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
        String trimmed = queryInput != null ? queryInput.trim() : "";
        if (trimmed.isEmpty()) {
            progress.setVisibility(View.GONE);
            tvNoResults.setText(R.string.search_no_results);
            tvNoResults.setVisibility(View.VISIBLE);
            rvBooks.setVisibility(View.GONE);
            return;
        }

        bookList.clear();
        adapter.notifyDataSetChanged();

        String query = trimmed.replace(" ", "+");
        String url = GoogleBooksJson.urlWithBooksApiKey(
                "https://www.googleapis.com/books/v1/volumes?q=" + query
                        + "&maxResults=20"
                        + "&printType=books"
                        + "&filter=ebooks");

        RequestQueue r = Volley.newRequestQueue(c);
        JsonObjectRequest json = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        progress.setVisibility(View.GONE);

                        if (!response.has("items")) {
                            tvNoResults.setText(R.string.search_no_results);
                            tvNoResults.setVisibility(View.VISIBLE);
                            rvBooks.setVisibility(View.GONE);
                            return;
                        }

                        JSONArray items = response.getJSONArray("items");
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject bookObj = items.getJSONObject(i);
                            JSONObject volumeInfo = bookObj.optJSONObject("volumeInfo");
                            if (volumeInfo == null) {
                                continue;
                            }

                            JSONObject accessInfo = bookObj.optJSONObject("accessInfo");
                            String readerLink = accessInfo != null ? accessInfo.optString("webReaderLink", "") : "";

                            String id = bookObj.optString("id", "");
                            if (id.isEmpty()) {
                                continue;
                            }
                            String title = volumeInfo.optString("title", "Untitled");

                            String author = "Unknown";
                            if (volumeInfo.has("authors")) {
                                JSONArray authorsArray = volumeInfo.optJSONArray("authors");
                                if (authorsArray != null && authorsArray.length() > 0) {
                                    author = authorsArray.getString(0);
                                }
                            }

                            String description = volumeInfo.optString("description", "No description available.");
                            String publisher = volumeInfo.optString("publisher", "Unknown");

                            String category = GoogleBooksJson.pickDisplayCategory(volumeInfo);

                            String imageUrl = "";
                            if (volumeInfo.has("imageLinks")) {
                                JSONObject links = volumeInfo.optJSONObject("imageLinks");
                                if (links != null) {
                                    imageUrl = links.optString("thumbnail",
                                            "https://img.freepik.com/free-vector/red-text-book-closed-icon_18591-82397.jpg?semt=ais_hybrid&w=740&q=80");
                                }
                                if (imageUrl.startsWith("http://")) {
                                    imageUrl = imageUrl.replace("http://", "https://");
                                }
                            }
                            bookList.add(new Book(id, title, imageUrl, author, description, publisher, category, readerLink));
                        }

                        adapter.notifyDataSetChanged();
                        if (bookList.isEmpty()) {
                            tvNoResults.setText(R.string.search_no_results);
                            tvNoResults.setVisibility(View.VISIBLE);
                            rvBooks.setVisibility(View.GONE);
                        } else {
                            tvNoResults.setVisibility(View.GONE);
                            rvBooks.setVisibility(View.VISIBLE);
                        }
                    } catch (JSONException e) {
                        Log.e("SearchPage", "parse error", e);
                        progress.setVisibility(View.GONE);
                        tvNoResults.setText(R.string.search_no_results);
                        tvNoResults.setVisibility(View.VISIBLE);
                        rvBooks.setVisibility(View.GONE);
                    }
                },
                //error -> Toast.makeText(c, error.toString(), Toast.LENGTH_SHORT).show()
                error -> {
                    progress.setVisibility(View.GONE);
                    tvNoResults.setText(R.string.search_no_results);
                    tvNoResults.setVisibility(View.VISIBLE);
                    rvBooks.setVisibility(View.GONE);

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
        r.add(json);
    }
}
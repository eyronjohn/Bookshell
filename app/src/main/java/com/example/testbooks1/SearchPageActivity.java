package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

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

public class SearchPageActivity extends AppCompatActivity {

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
        rvBooks = findViewById(R.id.rvBooks);
        bookList = new ArrayList<>();
        rvBooks.setLayoutManager(new GridLayoutManager(c, 2));
        adapter = new BookAdapter(c, (ArrayList<Book>) bookList);
        rvBooks.setAdapter(adapter);
        progress = findViewById(R.id.progressBooks);
        progress.setVisibility(View.VISIBLE);

        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_search);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(c, MainActivity.class));
                return true;
            } else if (id == R.id.nav_search) {
                //startActivity(new Intent(c, SearchActivity.class));
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

        String query = getIntent().getStringExtra("query");
        if (query != null && !query.isEmpty()) {
            callBooks(query);
        }
    }

    public void callBooks(String queryInput) {
        String query = queryInput.trim().replace(" ", "+");
        String url = "https://www.googleapis.com/books/v1/volumes?q=" + query
                + "&maxResults=10"
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
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(c, error.toString(), Toast.LENGTH_SHORT).show()
        );
        r.add(json);
    }
}
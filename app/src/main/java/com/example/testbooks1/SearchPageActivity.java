package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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
            if(searchQuery.isEmpty()){
                Toast.makeText(c, "Please enter a search query.", Toast.LENGTH_SHORT).show();

            } if (!searchQuery.isEmpty()) {
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
                startActivity(new Intent(c, MainActivity.class));
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(c, SearchActivity.class));
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

    private boolean isValidQuery(String query) {
        query = query.trim();
        if (query.isEmpty()) return false;
        if (query.length() < 2) return false;
        int alphaCount = 0;
        for (char c : query.toCharArray()) {
            if (Character.isLetter(c)) alphaCount++;
        }
        double alphaRatio = (double) alphaCount / query.length();
        return alphaRatio >= 0.5;
    }

    public void callBooks(String queryInput) {
        if (!isValidQuery(queryInput)) {
            progress.setVisibility(View.GONE);
            tvNoResults.setText("No results found.");
            tvNoResults.setVisibility(View.VISIBLE);
            rvBooks.setVisibility(View.GONE);
            return;
        }

        bookList.clear();
        adapter.notifyDataSetChanged();

        //String query = queryInput.trim().replace(" ", "+");
        String query = Uri.encode(queryInput.trim());
        String url = "https://www.googleapis.com/books/v1/volumes?q=" + query
                + "&maxResults=10"
                + "&orderBy=newest"
                + "&printType=books"
                + "&filter=ebooks";
                //+ "&key=AIzaSyAycxqRNFLfOCxktkf3cDcWChAc0Cfvk4Y";

        RequestQueue r = Volley.newRequestQueue(c);
        JsonObjectRequest json = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        progress.setVisibility(View.GONE);

                        if (!response.has("items")) {
                            tvNoResults.setText("No results found.");
                            tvNoResults.setVisibility(View.VISIBLE);
                            rvBooks.setVisibility(View.GONE);
                            return;
                        }

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

                            String description = volumeInfo.optString("description", "No description available.");
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
                                        .optString("thumbnail", "https://img.freepik.com/free-vector/red-text-book-closed-icon_18591-82397.jpg?semt=ais_hybrid&w=740&q=80");
                                if (imageUrl.startsWith("http://")) {
                                    imageUrl = imageUrl.replace("http://", "https://");
                                }
                            }
                            bookList.add(new Book(id, title, imageUrl, author, description, publisher, category, readerLink));
                        }

                        adapter.notifyDataSetChanged();
                        if (bookList.isEmpty()) {
                            tvNoResults.setVisibility(View.VISIBLE);
                            rvBooks.setVisibility(View.GONE);
                        } else {
                            tvNoResults.setVisibility(View.GONE);
                            rvBooks.setVisibility(View.VISIBLE);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                //error -> Toast.makeText(c, error.toString(), Toast.LENGTH_SHORT).show()
                error -> {
                    progress.setVisibility(View.GONE);
                    tvNoResults.setText("No results found.");
                    tvNoResults.setVisibility(View.VISIBLE);
                    rvBooks.setVisibility(View.GONE);

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);

                        Log.e("VOLLEY_ERROR", "Status Code: " + statusCode);
                        Log.e("VOLLEY_ERROR", "Response: " + responseData);
                    } else {
                        Log.e("VOLLEY_ERROR", "Error: " + error.toString());
                    }
                }
        );
        r.add(json);
    }
}
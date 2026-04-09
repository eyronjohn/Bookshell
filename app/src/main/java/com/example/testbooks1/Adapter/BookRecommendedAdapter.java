package com.example.testbooks1.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.testbooks1.BookDetailActivity;
import com.example.testbooks1.Model.Book;
import com.example.testbooks1.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class BookRecommendedAdapter extends RecyclerView.Adapter<BookRecommendedAdapter.ViewHolder> {

    ArrayList<Book> list;
    Context context;

    public BookRecommendedAdapter(Context context, ArrayList<Book> list) {
        this.context = context;
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivBook;
        TextView tvTitle;
        TextView tvAuthor;

        public ViewHolder(View itemView) {
            super(itemView);
            ivBook = itemView.findViewById(R.id.ivBook);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book_recommended, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Book book = list.get(position);
        holder.tvTitle.setText(book.getTitle());
        holder.tvAuthor.setText(book.getAuthor());

        Glide.with(context)
                .load(book.getImageUrl())
                .centerCrop()
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(32)))
                .into(holder.ivBook);

        holder.itemView.setOnClickListener(v -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();

            if (user != null) {
                String userId = user.getUid();

                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("recently_viewed")
                        .child(userId)
                        .child(book.getId());

                Book recentBook = new Book(
                        book.getId(),
                        book.getTitle(),
                        book.getImageUrl(),
                        book.getAuthor(),
                        book.getDescription(),
                        book.getPublisher(),
                        book.getCategory(),
                        book.getReaderLink()
                );
                recentBook.timestamp = System.currentTimeMillis();

                ref.setValue(recentBook);
            }
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("bookId", book.getId());
            intent.putExtra("title", book.getTitle());
            intent.putExtra("image", book.getImageUrl());
            intent.putExtra("author", book.getAuthor());
            intent.putExtra("description", book.getDescription());
            intent.putExtra("publisher", book.getPublisher());
            intent.putExtra("category", book.getCategory());
            intent.putExtra("readerLink", book.getReaderLink());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
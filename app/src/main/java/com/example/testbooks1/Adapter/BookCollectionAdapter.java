package com.example.testbooks1.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Model.Book;
import com.example.testbooks1.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;

public class BookCollectionAdapter extends RecyclerView.Adapter<BookCollectionAdapter.ViewHolder> {

    ArrayList<Book> list;
    Context context;

    public BookCollectionAdapter(Context context, ArrayList<Book> list) {
        this.context = context;
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivBook;
        TextView tvTitle;
        TextView tvAuthor;
        TextView tvCategory;

        public ViewHolder(View itemView) {
            super(itemView);
            ivBook = itemView.findViewById(R.id.ivThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_collection_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Book book = list.get(position);
        holder.tvTitle.setText(book.getTitle());
        holder.tvAuthor.setText(book.getAuthor());

        String imageUrl = book.getImageUrl();
        if (imageUrl != null) {
            if (imageUrl.contains("&edge=curl")) {
                imageUrl = imageUrl.replace("&edge=curl", "");
            }
            imageUrl = imageUrl.replace(" ", "%20");
        } else {
            imageUrl = "";
        }

        Log.d("BookImageURL", "Position " + position + ": " + imageUrl);

        Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .into(holder.ivBook);

//        holder.itemView.setOnClickListener(v -> {
//            FirebaseAuth auth = FirebaseAuth.getInstance();
//            FirebaseUser user = auth.getCurrentUser();
//
//            if (user != null) {
//                String userId = user.getUid();
//
//                DatabaseReference ref = FirebaseDatabase.getInstance()
//                        .getReference("recently_viewed")
//                        .child(userId)
//                        .child(book.getId());
//
//                Book recentBook = new Book(
//                        book.getId(),
//                        book.getTitle(),
//                        book.getImageUrl(),
//                        book.getAuthor(),
//                        book.getDescription(),
//                        book.getPublisher(),
//                        book.getCategory()
//                );
//
//                recentBook.timestamp = System.currentTimeMillis();
//                ref.setValue(recentBook);
//            }
//
//            Intent intent = new Intent(context, BookDetailActivity.class);
//            intent.putExtra("bookId", book.getId());
//            intent.putExtra("title", book.getTitle());
//            intent.putExtra("image", book.getImageUrl());
//            intent.putExtra("author", book.getAuthor());
//            intent.putExtra("description", book.getDescription());
//            intent.putExtra("publisher", book.getPublisher());
//            intent.putExtra("category", book.getCategory());
//            context.startActivity(intent);
//        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
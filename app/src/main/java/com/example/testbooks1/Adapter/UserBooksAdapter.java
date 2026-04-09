package com.example.testbooks1.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Model.CommunityBook;
import com.example.testbooks1.R;

import java.util.ArrayList;
import java.util.HashSet;

public class UserBooksAdapter extends RecyclerView.Adapter<UserBooksAdapter.ViewHolder> {

    Context context;
    ArrayList<CommunityBook> books;
    HashSet<String> selectedBookIds;
    OnBookSelectListener listener;

    public UserBooksAdapter(Context context,
                            ArrayList<CommunityBook> books,
                            HashSet<String> selectedBookIds,
                            OnBookSelectListener listener) {
        this.context = context;
        this.books = books;
        this.selectedBookIds = selectedBookIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityBook book = books.get(position);

        holder.tvTitle.setText(book.title);
        holder.tvAuthor.setText(book.author);
        Glide.with(context).load(book.imageUrl).into(holder.ivBook);

        boolean isSelected = selectedBookIds != null && selectedBookIds.contains(book.bookId);
        holder.checkIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            boolean currentlySelected = selectedBookIds.contains(book.bookId);

            if (currentlySelected) {
                selectedBookIds.remove(book.bookId);
                if (listener != null) listener.onBookSelected(book, false);
            } else {
                selectedBookIds.add(book.bookId);
                if (listener != null) listener.onBookSelected(book, true);
            }
            notifyItemChanged(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBook, checkIcon;
        TextView tvTitle, tvAuthor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBook = itemView.findViewById(R.id.ivBook);
            checkIcon = itemView.findViewById(R.id.checkIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
        }
    }

    public interface OnBookSelectListener {
        void onBookSelected(CommunityBook book, boolean isSelected);
    }
}
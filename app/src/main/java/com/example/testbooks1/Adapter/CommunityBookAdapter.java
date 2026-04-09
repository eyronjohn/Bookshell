package com.example.testbooks1.Adapter;
import com.example.testbooks1.ListDetailActivity;
import com.example.testbooks1.Model.CommunityBook;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Model.CommunityBook;
import com.example.testbooks1.Model.CommunityItem;
import com.example.testbooks1.R;

import java.util.ArrayList;

public class CommunityBookAdapter extends RecyclerView.Adapter<CommunityBookAdapter.ViewHolder> {
    Context context;
    ArrayList<CommunityBook> books;
    public String userId, listTitle; //

    public CommunityBookAdapter(Context context, ArrayList<CommunityBook> books) {
        this.context = context;
        this.books = books;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBook;
        TextView txtUserFullName, txtListTitle, txtListDescription;

        public ViewHolder(View itemView) {
            super(itemView);
            imgBook = itemView.findViewById(R.id.imgBook);
            txtUserFullName = itemView.findViewById(R.id.tvFullName);
            txtListTitle = itemView.findViewById(R.id.tvListTitle);
            txtListDescription = itemView.findViewById(R.id.tvListDescription);
        }
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CommunityBook book = books.get(position);

        holder.txtListTitle.setText(book.title);
        holder.txtListDescription.setText(book.description);
        holder.txtUserFullName.setText(book.author);

        Glide.with(context)
                .load(book.imageUrl)
                .into(holder.imgBook);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ListDetailActivity.class);
            intent.putExtra("userId", userId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }
}
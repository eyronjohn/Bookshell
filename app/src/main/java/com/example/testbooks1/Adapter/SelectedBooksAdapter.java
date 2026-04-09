package com.example.testbooks1.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.testbooks1.Model.CommunityBook;
import com.example.testbooks1.R;
import java.util.ArrayList;

public class SelectedBooksAdapter extends RecyclerView.Adapter<SelectedBooksAdapter.ViewHolder> {

    Context context;
    ArrayList<CommunityBook> list;

    public SelectedBooksAdapter(Context context, ArrayList<CommunityBook> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //View view = LayoutInflater.from(context).inflate(R.layout.item_selected_book, parent, false);
        View view = LayoutInflater.from(context).inflate(R.layout.item_horizontal_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CommunityBook book = list.get(position);

        holder.tvTitle.setText(book.title);
        holder.tvAuthor.setText(book.author);

        //Glide.with(context).load(book.image).into(holder.ivBook);
        Glide.with(context)
                .load(book.imageUrl)
                .into(holder.ivBook);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBook;
        TextView tvTitle, tvAuthor;

        public ViewHolder(View itemView) {
            super(itemView);
            ivBook = itemView.findViewById(R.id.ivBook);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
        }
    }
}
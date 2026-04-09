package com.example.testbooks1.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.testbooks1.R;
import com.example.testbooks1.Model.Review;

import java.util.ArrayList;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    Context context;
    ArrayList<Review> list;

    public ReviewAdapter(Context context, ArrayList<Review> list) {
        this.context = context;
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvComment, tvTime;
        RatingBar ratingBar;

        public ViewHolder(View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvUser);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvTime = itemView.findViewById(R.id.tvTime);
            ratingBar = itemView.findViewById(R.id.ratingBarItem);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Review r = list.get(position);

        holder.tvUser.setText(r.userName);
        holder.tvComment.setText(r.comment);
        holder.ratingBar.setRating(r.rating);

        // simple time display
        holder.tvTime.setText(formatTime(r.timestamp));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String formatTime(long time) {
        long diff = System.currentTimeMillis() - time;

        long minutes = diff / (1000 * 60);
        long hours = minutes / 60;
        long days = hours / 24;

        if (minutes < 60) return minutes + " min ago";
        else if (hours < 24) return hours + " hrs ago";
        else return days + " days ago";
    }
}
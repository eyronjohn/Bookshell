package com.example.testbooks1.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.ImageView;
import android.widget.TextView;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.R;
import com.example.testbooks1.Model.Review;
import com.example.testbooks1.ViewProfileActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("NotifyDataSetChanged")
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    Context context;
    ArrayList<Review> list;
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
    private final Map<String, UserUi> userCache = new HashMap<>();
    private final Map<String, ValueEventListener> userListeners = new HashMap<>();

    public ReviewAdapter(Context context, ArrayList<Review> list) {
        this.context = context;
        this.list = list;
    }

    private static class UserUi {
        String fullName;
        String imageUrl;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvComment, tvTime;
        RatingBar ratingBar;
        ImageView ivProfileImage;

        public ViewHolder(View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvUser);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvTime = itemView.findViewById(R.id.tvTime);
            ratingBar = itemView.findViewById(R.id.ratingBarItem);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
        }
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Review r = list.get(position);

        holder.tvUser.setText(r.userName != null ? r.userName : "Anonymous");
        holder.tvComment.setText(r.comment);
        holder.ratingBar.setRating(r.rating);
        holder.ivProfileImage.setImageResource(R.drawable.default_pfp);

        // simple time display
        holder.tvTime.setText(formatTime(r.timestamp));
        bindLiveUserUi(r.userId, holder);

        View.OnClickListener openProfile = v -> {
            if (r.userId != null && !r.userId.trim().isEmpty()) {
                Intent intent = new Intent(context, ViewProfileActivity.class);
                intent.putExtra("userId", r.userId);
                context.startActivity(intent);
            }
        };
        holder.ivProfileImage.setOnClickListener(openProfile);
        holder.tvUser.setOnClickListener(openProfile);
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

    private void bindLiveUserUi(String userId, ViewHolder holder) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        UserUi cached = userCache.get(userId);
        if (cached != null) {
            holder.tvUser.setText(cached.fullName);
            if (cached.imageUrl != null && !cached.imageUrl.isEmpty()) {
                Glide.with(context).load(cached.imageUrl)
                        .placeholder(R.drawable.default_pfp)
                        .error(R.drawable.default_pfp)
                        .into(holder.ivProfileImage);
            }
        }

        if (userListeners.containsKey(userId)) {
            return;
        }

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String first = snapshot.child("firstName").getValue(String.class);
                String last = snapshot.child("lastName").getValue(String.class);
                String image = snapshot.child("profileImageUrl").getValue(String.class);
                String full = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                if (full.isEmpty()) {
                    full = "Anonymous";
                }
                UserUi ui = new UserUi();
                ui.fullName = full;
                ui.imageUrl = image;
                userCache.put(userId, ui);
                notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        usersRef.child(userId).addValueEventListener(listener);
        userListeners.put(userId, listener);
    }

    public void detachUserProfileListeners() {
        for (Map.Entry<String, ValueEventListener> e : new HashMap<>(userListeners).entrySet()) {
            usersRef.child(e.getKey()).removeEventListener(e.getValue());
        }
        userListeners.clear();
    }
}
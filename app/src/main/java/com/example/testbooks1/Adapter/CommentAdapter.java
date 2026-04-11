package com.example.testbooks1.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Model.Comment;
import com.example.testbooks1.R;
import com.example.testbooks1.ViewProfileActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("NotifyDataSetChanged")
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    Context context;
    ArrayList<Comment> comments;
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
    private final Map<String, UserUi> userCache = new HashMap<>();
    private final Map<String, ValueEventListener> userListeners = new HashMap<>();

    private static class UserUi {
        String fullName;
        String imageUrl;
    }

    public CommentAdapter(Context context, ArrayList<Comment> comments) {
        this.context = context;
        this.comments = comments;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvComment, tvTime;
        ImageView ivProfileImage;

        public ViewHolder(View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvFullName);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
        }
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Comment c = comments.get(position);
        holder.tvUsername.setText(c.fullName != null ? c.fullName : "Anonymous");
        holder.tvComment.setText(c.text);
        holder.tvTime.setText(android.text.format.DateUtils.getRelativeTimeSpanString(c.timestamp));
        holder.ivProfileImage.setImageResource(R.drawable.default_pfp);
        bindLiveUserUi(c.userId, holder);

        holder.itemView.setOnClickListener(v -> {
            if (c.userId != null) {
                Intent intent = new Intent(context, ViewProfileActivity.class);
                intent.putExtra("userId", c.userId);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    private void bindLiveUserUi(String userId, ViewHolder holder) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        UserUi cached = userCache.get(userId);
        if (cached != null) {
            holder.tvUsername.setText(cached.fullName);
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
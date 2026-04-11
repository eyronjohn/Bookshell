package com.example.testbooks1.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.EditListActivity;
import com.example.testbooks1.Model.CommunityItem;
import com.example.testbooks1.R;
import com.example.testbooks1.ListDetailActivity;
import com.example.testbooks1.ViewProfileActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressLint({"NotifyDataSetChanged", "RecyclerView"})
public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {
    DatabaseReference db = FirebaseDatabase.getInstance().getReference();

    Context context;
    ArrayList<CommunityItem> items;
    private final Map<String, UserUi> userCache = new HashMap<>();
    private final Map<String, ValueEventListener> userListeners = new HashMap<>();

    private static class UserUi {
        String fullName;
        String imageUrl;
    }

    public CommunityAdapter(Context context, ArrayList<CommunityItem> items) {
        this.context = context;
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgBook;
        TextView tvFullName, tvListTitle, tvListDescription;
        ImageView btnOption, btnHeart, ivProfileImage;
        TextView tvHeartCount, tvCommentCount;

        public ViewHolder(View itemView) {
            super(itemView);
            imgBook = itemView.findViewById(R.id.imgBook);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvListTitle = itemView.findViewById(R.id.tvListTitle);
            tvListDescription = itemView.findViewById(R.id.tvListDescription);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            btnOption = itemView.findViewById(R.id.btnOption);
            btnHeart = itemView.findViewById(R.id.btnHeart);
            tvHeartCount = itemView.findViewById(R.id.tvHeartCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
        }
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_community, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityItem item = items.get(position);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = firebaseUser != null ? firebaseUser.getUid() : null;

        DatabaseReference listRef = db.child("communityLists")
                .child(item.userId)
                .child(item.listId);

        DatabaseReference reactRef = currentUserId != null
                ? listRef.child("reactions").child(currentUserId)
                : null;

        holder.tvFullName.setText(item.fullName != null ? item.fullName : "Anonymous");
        holder.ivProfileImage.setImageResource(R.drawable.default_pfp);
        holder.tvListTitle.setText(item.listTitle);
        holder.tvListDescription.setText(item.listDescription);
        bindLiveUserUi(item, holder);

        View.OnClickListener openProfile = v -> {
            Intent intent = new Intent(context, ViewProfileActivity.class);
            intent.putExtra("userId", item.userId);
            context.startActivity(intent);
        };
        holder.ivProfileImage.setOnClickListener(openProfile);
        holder.tvFullName.setOnClickListener(openProfile);

        if (item.coverImage != null && !item.coverImage.isEmpty()) {
            try {
                byte[] decodedBytes = android.util.Base64.decode(item.coverImage, android.util.Base64.DEFAULT);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.imgBook.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.w("CommunityAdapter", "cover decode", e);
                Glide.with(context).load(item.firstBookImage).into(holder.imgBook);
            }
        } else {
            Glide.with(context).load(item.firstBookImage).into(holder.imgBook);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ListDetailActivity.class);
            intent.putExtra("userId", item.userId);
            intent.putExtra("listId", item.listId);
            intent.putExtra("listTitle", item.listTitle);
            intent.putExtra("listDescription", item.listDescription);
            intent.putParcelableArrayListExtra("books", item.books);
            context.startActivity(intent);
        });

        listRef.child("reactionCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer count = snapshot.getValue(Integer.class);
                holder.tvHeartCount.setText(String.valueOf(count != null ? count : 0));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        holder.tvCommentCount.setText(String.valueOf(item.commentCount));

        if (reactRef != null) {
            reactRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        holder.btnHeart.setImageResource(R.drawable.ic_heart_filled);
                    } else {
                        holder.btnHeart.setImageResource(R.drawable.ic_community_heart);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else {
            holder.btnHeart.setImageResource(R.drawable.ic_community_heart);
        }

        holder.btnHeart.setOnClickListener(v -> {
            if (reactRef == null) {
                Toast.makeText(context, R.string.toast_sign_in_to_react, Toast.LENGTH_SHORT).show();
                return;
            }
            reactRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        reactRef.removeValue();
                        listRef.child("reactionCount").setValue(ServerValue.increment(-1));
                    } else {
                        reactRef.setValue(true);
                        listRef.child("reactionCount").setValue(ServerValue.increment(1));
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        });

        if (currentUserId != null && currentUserId.equals(item.userId)) {
            holder.btnOption.setVisibility(View.VISIBLE);
            holder.btnOption.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, v);
                popup.getMenuInflater().inflate(R.menu.menu_edit_delete, popup.getMenu());
                popup.setOnMenuItemClickListener(menuItem -> {
                    int id = menuItem.getItemId();
                    if (id == R.id.action_edit) {
                        Intent intent = new Intent(context, EditListActivity.class);
                        intent.putExtra("listId", item.listId);
                        context.startActivity(intent);
                        return true;
                    } else if (id == R.id.action_delete) {
                        new AlertDialog.Builder(context)
                                .setTitle("Confirm Delete")
                                .setMessage("Are you sure you want to delete this list?")
                                .setPositiveButton("Confirm", (dialog, which) -> {
                                    listRef.removeValue().addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            items.remove(position);
                                            notifyItemRemoved(position);
                                            Toast.makeText(context, "List deleted successfully.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(context, "Failed to delete list.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        } else {
            holder.btnOption.setVisibility(View.GONE);
        }
    }
    @Override
    public int getItemCount() {
        return items.size();
    }

    private void bindLiveUserUi(CommunityItem item, ViewHolder holder) {
        String userId = item.userId;
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        UserUi cached = userCache.get(userId);
        if (cached != null) {
            holder.tvFullName.setText(cached.fullName);
            if (cached.imageUrl != null && !cached.imageUrl.isEmpty()) {
                Glide.with(context).load(cached.imageUrl)
                        .placeholder(R.drawable.default_pfp)
                        .error(R.drawable.default_pfp)
                        .into(holder.ivProfileImage);
            }
        } else if (item.profileImageUrl != null && !item.profileImageUrl.isEmpty()) {
            Glide.with(context).load(item.profileImageUrl)
                    .placeholder(R.drawable.default_pfp)
                    .error(R.drawable.default_pfp)
                    .into(holder.ivProfileImage);
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
        db.child("users").child(userId).addValueEventListener(listener);
        userListeners.put(userId, listener);
    }

    public void detachUserProfileListeners() {
        for (Map.Entry<String, ValueEventListener> e : new HashMap<>(userListeners).entrySet()) {
            db.child("users").child(e.getKey()).removeEventListener(e.getValue());
        }
        userListeners.clear();
    }
}
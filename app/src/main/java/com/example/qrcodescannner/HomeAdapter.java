package com.example.qrcodescannner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.HomeViewHolder> {
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onUploadClick();
        void onLogoutClick();
        void onProfileClick();
    }

    public HomeAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public HomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home, parent, false);
        return new HomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeViewHolder holder, int position) {
        // Set up click listeners
        holder.uploadButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUploadClick();
            }
        });

        holder.logoutButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLogoutClick();
            }
        });

        holder.profileButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProfileClick();
            }
        });
    }

    @Override
    public int getItemCount() {
        return 1; // We only need one item
    }

    static class HomeViewHolder extends RecyclerView.ViewHolder {
        Button uploadButton;
        Button logoutButton;
        Button profileButton;
        TextView heading;
        ImageView imageView;

        HomeViewHolder(@NonNull View itemView) {
            super(itemView);
            uploadButton = itemView.findViewById(R.id.uploadButton);
            logoutButton = itemView.findViewById(R.id.logoutButton);
            profileButton = itemView.findViewById(R.id.profile);
            heading = itemView.findViewById(R.id.Heading);
            imageView = itemView.findViewById(R.id.imageView3);
        }
    }
} 
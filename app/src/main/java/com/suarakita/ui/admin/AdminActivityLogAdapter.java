package com.suarakita.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.suarakita.R;
import com.suarakita.model.ActivityLog;

import java.util.ArrayList;
import java.util.List;

public class AdminActivityLogAdapter extends RecyclerView.Adapter<AdminActivityLogAdapter.ViewHolder> {

    private final List<ActivityLog> items = new ArrayList<>();

    public void submitList(List<ActivityLog> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityLog log = items.get(position);
        holder.textDescription.setText(log.getDescription());
        holder.textMeta.setText(holder.itemView.getContext()
                .getString(R.string.admin_activity_log_meta, log.getAdminName(), log.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textDescription;
        final TextView textMeta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDescription = itemView.findViewById(R.id.textDescription);
            textMeta = itemView.findViewById(R.id.textMeta);
        }
    }
}

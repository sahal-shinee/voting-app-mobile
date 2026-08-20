package com.suarakita.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.suarakita.R;
import com.suarakita.model.AdminCandidate;

import java.util.ArrayList;
import java.util.List;

public class AdminCandidateAdapter extends RecyclerView.Adapter<AdminCandidateAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(AdminCandidate candidate);

        void onDelete(AdminCandidate candidate);
    }

    private final List<AdminCandidate> candidates = new ArrayList<>();
    private final Listener listener;

    public AdminCandidateAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<AdminCandidate> newCandidates) {
        candidates.clear();
        if (newCandidates != null) {
            candidates.addAll(newCandidates);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_candidate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminCandidate candidate = candidates.get(position);

        holder.textName.setText(candidate.getName());
        holder.textInactive.setVisibility(candidate.isActive() ? View.GONE : View.VISIBLE);
        holder.buttonDelete.setVisibility(candidate.isActive() ? View.VISIBLE : View.GONE);

        Glide.with(holder.imagePhoto.getContext())
                .load(candidate.getPhotoUrl())
                .placeholder(R.drawable.ic_placeholder_photo)
                .error(R.drawable.ic_placeholder_photo)
                .centerCrop()
                .into(holder.imagePhoto);

        holder.buttonEdit.setOnClickListener(v -> listener.onEdit(candidate));
        holder.buttonDelete.setOnClickListener(v -> listener.onDelete(candidate));
    }

    @Override
    public int getItemCount() {
        return candidates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imagePhoto;
        final TextView textName;
        final TextView textInactive;
        final TextView buttonEdit;
        final TextView buttonDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePhoto = itemView.findViewById(R.id.imagePhoto);
            textName = itemView.findViewById(R.id.textName);
            textInactive = itemView.findViewById(R.id.textInactive);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}

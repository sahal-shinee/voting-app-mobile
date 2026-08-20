package com.suarakita.ui.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.card.MaterialCardView;
import com.suarakita.R;
import com.suarakita.model.Candidate;

import java.util.ArrayList;
import java.util.List;

public class CandidateAdapter extends RecyclerView.Adapter<CandidateAdapter.ViewHolder> {

    public interface OnCandidateSelectedListener {
        void onCandidateSelected(Candidate candidate);
    }

    private final List<Candidate> candidates = new ArrayList<>();
    private final OnCandidateSelectedListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private int lastAnimatedPosition = -1;

    public CandidateAdapter(OnCandidateSelectedListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Candidate> newCandidates) {
        candidates.clear();
        if (newCandidates != null) {
            candidates.addAll(newCandidates);
        }
        selectedPosition = RecyclerView.NO_POSITION;
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    public Candidate getSelectedCandidate() {
        if (selectedPosition == RecyclerView.NO_POSITION) {
            return null;
        }
        return candidates.get(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_candidate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Candidate candidate = candidates.get(position);

        holder.textName.setText(candidate.getName());
        holder.textDescription.setText(candidate.getDescription());
        holder.textNumber.setText(String.valueOf(position + 1));

        Glide.with(holder.imagePhoto.getContext())
                .load(candidate.getPhotoUrl())
                .transform(new CircleCrop())
                .placeholder(R.drawable.ic_placeholder_photo)
                .error(R.drawable.ic_placeholder_photo)
                .into(holder.imagePhoto);

        boolean selected = position == selectedPosition;
        holder.radioSelected.setChecked(selected);
        holder.card.setStrokeWidth(selected ? dp(holder, 2) : 0);
        holder.card.setCardBackgroundColor(holder.itemView.getResources()
                .getColor(selected ? R.color.color_primary_subtle : R.color.color_surface, holder.itemView.getContext().getTheme()));

        holder.card.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            if (previous != RecyclerView.NO_POSITION) {
                notifyItemChanged(previous);
            }
            notifyItemChanged(selectedPosition);
            listener.onCandidateSelected(candidate);
        });

        if (position > lastAnimatedPosition) {
            holder.itemView.setAnimation(AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_fade_slide_in));
            lastAnimatedPosition = position;
        }
    }

    private int dp(ViewHolder holder, int value) {
        return (int) (value * holder.itemView.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return candidates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView imagePhoto;
        final TextView textName;
        final TextView textDescription;
        final TextView textNumber;
        final RadioButton radioSelected;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            imagePhoto = itemView.findViewById(R.id.imagePhoto);
            textName = itemView.findViewById(R.id.textName);
            textDescription = itemView.findViewById(R.id.textDescription);
            textNumber = itemView.findViewById(R.id.textNumber);
            radioSelected = itemView.findViewById(R.id.radioSelected);
        }
    }
}

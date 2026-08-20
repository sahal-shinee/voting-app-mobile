package com.suarakita.ui.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.suarakita.R;
import com.suarakita.model.CandidateResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {

    private final List<CandidateResult> results = new ArrayList<>();
    private int yourCandidateId = -1;
    private int lastAnimatedPosition = -1;

    public void submitList(List<CandidateResult> newResults, int yourCandidateId) {
        results.clear();
        if (newResults != null) {
            results.addAll(newResults);
        }
        this.yourCandidateId = yourCandidateId;
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CandidateResult result = results.get(position);

        String name = result.getName();
        if (!result.isActive()) {
            name += " (" + holder.itemView.getContext().getString(R.string.results_inactive_candidate) + ")";
        }
        holder.textName.setText(name);

        holder.textPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", result.getPercentage()));
        holder.textVoteCount.setText(holder.itemView.getContext()
                .getString(R.string.results_vote_count, result.getVoteCount()));
        holder.progressPercentage.setProgress((int) Math.round(result.getPercentage()));
        holder.textYourPick.setVisibility(result.getCandidateId() == yourCandidateId ? View.VISIBLE : View.GONE);
        // Backend selalu mengurutkan kandidat dari suara terbanyak (ORDER BY vote_count DESC),
        // jadi vote_count di posisi 0 = jumlah suara tertinggi. Dibandingkan (bukan position == 0)
        // supaya kalau ada yang seri di posisi puncak, badge "Memimpin" muncul di semuanya.
        boolean isLeading = result.getVoteCount() > 0 && result.getVoteCount() == results.get(0).getVoteCount();
        holder.textLeading.setVisibility(isLeading ? View.VISIBLE : View.GONE);

        Glide.with(holder.imagePhoto.getContext())
                .load(result.getPhotoUrl())
                .placeholder(R.drawable.ic_placeholder_photo)
                .error(R.drawable.ic_placeholder_photo)
                .centerCrop()
                .into(holder.imagePhoto);

        if (position > lastAnimatedPosition) {
            holder.itemView.setAnimation(AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_fade_slide_in));
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imagePhoto;
        final TextView textName;
        final TextView textPercentage;
        final TextView textVoteCount;
        final TextView textYourPick;
        final TextView textLeading;
        final LinearProgressIndicator progressPercentage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePhoto = itemView.findViewById(R.id.imagePhoto);
            textName = itemView.findViewById(R.id.textName);
            textPercentage = itemView.findViewById(R.id.textPercentage);
            textVoteCount = itemView.findViewById(R.id.textVoteCount);
            textYourPick = itemView.findViewById(R.id.textYourPick);
            textLeading = itemView.findViewById(R.id.textLeading);
            progressPercentage = itemView.findViewById(R.id.progressPercentage);
        }
    }
}

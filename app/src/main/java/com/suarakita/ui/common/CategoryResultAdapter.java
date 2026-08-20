package com.suarakita.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.suarakita.R;
import com.suarakita.model.CandidateResult;
import com.suarakita.model.Category;
import com.suarakita.model.VotingResults;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Dipakai untuk preview kategori di tab Hasil -- baik sisi siswa (GET /categories/{id}/results,
// bisa locked/403) maupun admin (GET /admin/categories/{id}/results, tidak pernah locked).
public class CategoryResultAdapter extends RecyclerView.Adapter<CategoryResultAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final List<CategoryResultPreview> items = new ArrayList<>();
    private final OnCategoryClickListener listener;

    public CategoryResultAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CategoryResultPreview> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void updateResults(int position, VotingResults results) {
        if (position < 0 || position >= items.size()) {
            return;
        }
        CategoryResultPreview item = items.get(position);
        item.results = results;
        item.state = CategoryResultPreview.State.LOADED;
        notifyItemChanged(position);
    }

    public void markLocked(int position) {
        setState(position, CategoryResultPreview.State.LOCKED);
    }

    public void markError(int position) {
        setState(position, CategoryResultPreview.State.ERROR);
    }

    private void setState(int position, CategoryResultPreview.State state) {
        if (position < 0 || position >= items.size()) {
            return;
        }
        items.get(position).state = state;
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryResultPreview item = items.get(position);
        holder.textName.setText(item.category.getName());
        holder.card.setOnClickListener(v -> listener.onCategoryClick(item.category));

        switch (item.state) {
            case LOCKED:
                holder.progressBar.setVisibility(View.GONE);
                holder.donutChart.setVisibility(View.GONE);
                holder.textLeading.setVisibility(View.GONE);
                holder.textSubtitle.setText(R.string.results_locked_short);
                break;

            case ERROR:
                holder.progressBar.setVisibility(View.GONE);
                holder.donutChart.setVisibility(View.GONE);
                holder.textLeading.setVisibility(View.GONE);
                holder.textSubtitle.setText(R.string.results_error);
                break;

            case LOADED:
                holder.progressBar.setVisibility(View.GONE);
                holder.donutChart.setVisibility(View.VISIBLE);
                bindResults(holder, item.results);
                break;

            case LOADING:
            default:
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.donutChart.setVisibility(View.GONE);
                holder.textLeading.setVisibility(View.GONE);
                holder.textSubtitle.setText(null);
                break;
        }
    }

    private void bindResults(ViewHolder holder, VotingResults results) {
        List<CandidateResult> candidates = results.getCandidates();
        holder.textSubtitle.setText(holder.itemView.getContext()
                .getString(R.string.results_total_votes, results.getTotalVotes()));

        if (candidates == null || candidates.isEmpty()) {
            holder.donutChart.setSegments(Collections.emptyList());
            holder.donutChart.setCenterText("0%");
            holder.textLeading.setVisibility(View.GONE);
            return;
        }

        CandidateResult leading = candidates.get(0);
        for (CandidateResult candidate : candidates) {
            if (candidate.getVoteCount() > leading.getVoteCount()) {
                leading = candidate;
            }
        }

        int color = DonutChartView.colorForIndex(holder.itemView.getContext(), 0);
        List<DonutChartView.Segment> segments = new ArrayList<>();
        segments.add(new DonutChartView.Segment((float) leading.getPercentage(), color));
        holder.donutChart.setSegments(segments);
        holder.donutChart.setCenterText(Math.round(leading.getPercentage()) + "%");

        holder.textLeading.setVisibility(View.VISIBLE);
        holder.textLeading.setText(holder.itemView.getContext()
                .getString(R.string.results_leading_candidate, leading.getName()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView textName;
        final TextView textSubtitle;
        final TextView textLeading;
        final DonutChartView donutChart;
        final ProgressBar progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            textName = itemView.findViewById(R.id.textName);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textLeading = itemView.findViewById(R.id.textLeading);
            donutChart = itemView.findViewById(R.id.donutChart);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}

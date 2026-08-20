package com.suarakita.ui.student;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.suarakita.R;
import com.suarakita.model.Category;
import com.suarakita.ui.common.DonutChartView;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private int lastAnimatedPosition = -1;

    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Category> newCategories) {
        categories.clear();
        if (newCategories != null) {
            categories.addAll(newCategories);
        }
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);

        holder.textName.setText(category.getName());

        String name = category.getName();
        holder.textAvatar.setText(name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        holder.textAvatar.getBackground().mutate().setTint(DonutChartView.colorForIndex(holder.itemView.getContext(), position));

        if (category.getDescription() == null || category.getDescription().isEmpty()) {
            holder.textDescription.setVisibility(View.GONE);
        } else {
            holder.textDescription.setVisibility(View.VISIBLE);
            holder.textDescription.setText(category.getDescription());
        }

        boolean open = category.isVotingOpen();
        holder.textStatus.setText(open ? R.string.status_live : R.string.status_closed);
        int statusColor = ContextCompat.getColor(holder.itemView.getContext(),
                open ? R.color.color_live : R.color.color_text_secondary);
        holder.textStatus.getBackground().mutate().setTint(statusColor);

        holder.textVoted.setVisibility(category.isHasVoted() ? View.VISIBLE : View.GONE);

        holder.card.setOnClickListener(v -> listener.onCategoryClick(category));

        if (position > lastAnimatedPosition) {
            holder.itemView.setAnimation(AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_fade_slide_in));
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView textName;
        final TextView textDescription;
        final TextView textStatus;
        final TextView textVoted;
        final TextView textAvatar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            textName = itemView.findViewById(R.id.textName);
            textDescription = itemView.findViewById(R.id.textDescription);
            textStatus = itemView.findViewById(R.id.textStatus);
            textVoted = itemView.findViewById(R.id.textVoted);
            textAvatar = itemView.findViewById(R.id.textAvatar);
        }
    }
}

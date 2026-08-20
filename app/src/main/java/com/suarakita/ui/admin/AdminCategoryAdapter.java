package com.suarakita.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.suarakita.R;
import com.suarakita.model.Category;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminCategoryAdapter extends RecyclerView.Adapter<AdminCategoryAdapter.ViewHolder> {

    private static final SimpleDateFormat DB_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd MMM, HH:mm", new Locale("in", "ID"));

    public interface Listener {
        void onCategoryClick(Category category);
    }

    private final List<Category> categories = new ArrayList<>();
    private final Listener listener;

    public AdminCategoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Category> newCategories) {
        categories.clear();
        if (newCategories != null) {
            categories.addAll(newCategories);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        Context context = holder.itemView.getContext();

        holder.textName.setText(category.getName());

        boolean votingOpen = category.isVotingOpen();
        holder.chipVotingStatus.setText(votingOpen ? R.string.admin_chip_voting_open : R.string.admin_chip_voting_closed);
        holder.chipVotingStatus.getBackground().mutate()
                .setTint(ContextCompat.getColor(context, votingOpen ? R.color.color_live : R.color.color_text_secondary));

        boolean resultsLive = category.isShowLiveResults();
        holder.chipResultsStatus.setText(resultsLive ? R.string.admin_chip_results_live : R.string.admin_chip_results_hidden);
        holder.chipResultsStatus.getBackground().mutate()
                .setTint(ContextCompat.getColor(context, resultsLive ? R.color.color_primary : R.color.color_text_secondary));

        String start = formatForDisplay(category.getVotingStartAt());
        String end = formatForDisplay(category.getVotingEndAt());
        if (start != null && end != null) {
            holder.rowSchedule.setVisibility(View.VISIBLE);
            holder.textSchedule.setText(context.getString(R.string.admin_category_schedule_chip, start, end));
        } else {
            holder.rowSchedule.setVisibility(View.GONE);
        }

        holder.card.setOnClickListener(v -> listener.onCategoryClick(category));
    }

    private String formatForDisplay(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        try {
            Date date = DB_FORMAT.parse(dbValue);
            return date != null ? DISPLAY_FORMAT.format(date) : null;
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView textName;
        final TextView chipVotingStatus;
        final TextView chipResultsStatus;
        final LinearLayout rowSchedule;
        final TextView textSchedule;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            textName = itemView.findViewById(R.id.textName);
            chipVotingStatus = itemView.findViewById(R.id.chipVotingStatus);
            chipResultsStatus = itemView.findViewById(R.id.chipResultsStatus);
            rowSchedule = itemView.findViewById(R.id.rowSchedule);
            textSchedule = itemView.findViewById(R.id.textSchedule);
        }
    }
}

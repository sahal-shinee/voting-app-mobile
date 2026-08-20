package com.suarakita.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.suarakita.R;
import com.suarakita.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryTrashAdapter extends RecyclerView.Adapter<CategoryTrashAdapter.ViewHolder> {

    public interface Listener {
        void onRestore(Category category);

        void onPermanentDelete(Category category);
    }

    private final List<Category> items = new ArrayList<>();
    private final Listener listener;

    public CategoryTrashAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Category> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_trash, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = items.get(position);
        holder.textName.setText(category.getName());
        holder.textDeletedAt.setText(holder.itemView.getContext()
                .getString(R.string.admin_trash_deleted_at, category.getDeletedAt()));

        holder.buttonRestore.setOnClickListener(v -> listener.onRestore(category));
        holder.buttonPermanentDelete.setOnClickListener(v -> listener.onPermanentDelete(category));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textName;
        final TextView textDeletedAt;
        final MaterialButton buttonRestore;
        final MaterialButton buttonPermanentDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textDeletedAt = itemView.findViewById(R.id.textDeletedAt);
            buttonRestore = itemView.findViewById(R.id.buttonRestore);
            buttonPermanentDelete = itemView.findViewById(R.id.buttonPermanentDelete);
        }
    }
}

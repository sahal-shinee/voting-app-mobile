package com.suarakita.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.suarakita.R;
import com.suarakita.model.Student;

import java.util.ArrayList;
import java.util.List;

public class NonVoterAdapter extends RecyclerView.Adapter<NonVoterAdapter.ViewHolder> {

    private final List<Student> items = new ArrayList<>();

    public void submitList(List<Student> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_non_voter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = items.get(position);
        holder.textName.setText(student.getName());
        holder.textNis.setText(student.getNis());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textName;
        final TextView textNis;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textNis = itemView.findViewById(R.id.textNis);
        }
    }
}

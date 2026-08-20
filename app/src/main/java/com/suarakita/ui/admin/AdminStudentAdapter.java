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

public class AdminStudentAdapter extends RecyclerView.Adapter<AdminStudentAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(Student student);

        void onResetPassword(Student student);

        void onDelete(Student student);
    }

    private final List<Student> students = new ArrayList<>();
    private final Listener listener;

    public AdminStudentAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Student> newStudents) {
        students.clear();
        if (newStudents != null) {
            students.addAll(newStudents);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = students.get(position);

        holder.textName.setText(student.getName());
        holder.textNis.setText(student.getNis());
        holder.textMustChange.setVisibility(student.isMustChangePassword() ? View.VISIBLE : View.GONE);

        holder.buttonEdit.setOnClickListener(v -> listener.onEdit(student));
        holder.buttonReset.setOnClickListener(v -> listener.onResetPassword(student));
        holder.buttonDelete.setOnClickListener(v -> listener.onDelete(student));
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textName;
        final TextView textNis;
        final TextView textMustChange;
        final TextView buttonEdit;
        final TextView buttonReset;
        final TextView buttonDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textNis = itemView.findViewById(R.id.textNis);
            textMustChange = itemView.findViewById(R.id.textMustChange);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonReset = itemView.findViewById(R.id.buttonReset);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}

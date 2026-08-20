package com.suarakita.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.Category;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCategoryTrashActivity extends AppCompatActivity {

    private RecyclerView recyclerTrash;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private TextView textError;
    private CategoryTrashAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_category_trash);

        findViewById(R.id.buttonBackArrow).setOnClickListener(v -> finish());

        recyclerTrash = findViewById(R.id.recyclerTrash);
        progressBar = findViewById(R.id.progressBar);
        textEmpty = findViewById(R.id.textEmpty);
        textError = findViewById(R.id.textError);

        adapter = new CategoryTrashAdapter(new CategoryTrashAdapter.Listener() {
            @Override
            public void onRestore(Category category) {
                confirmRestore(category);
            }

            @Override
            public void onPermanentDelete(Category category) {
                confirmPermanentDelete(category);
            }
        });
        recyclerTrash.setLayoutManager(new LinearLayoutManager(this));
        recyclerTrash.setAdapter(adapter);

        loadTrash();
    }

    private void loadTrash() {
        showLoading();

        RetrofitClient.getApiService().adminGetCategoriesTrash().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Category> trash = response.body().getData();
                    adapter.submitList(trash);
                    if (trash.isEmpty()) {
                        showEmpty();
                    } else {
                        showContent();
                    }
                } else {
                    showError(ApiError.message(response, getString(R.string.admin_loading_error)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                showError(getString(R.string.error_no_connection));
            }
        });
    }

    private void confirmRestore(Category category) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_confirm_restore_title)
                .setMessage(R.string.admin_confirm_restore_message)
                .setPositiveButton(R.string.admin_confirm_yes, (dialog, which) -> restore(category))
                .setNegativeButton(R.string.admin_confirm_cancel, null)
                .show();
    }

    private void restore(Category category) {
        RetrofitClient.getApiService().adminRestoreCategory(category.getId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminCategoryTrashActivity.this, R.string.admin_restored, Toast.LENGTH_SHORT).show();
                            loadTrash();
                        } else {
                            Toast.makeText(AdminCategoryTrashActivity.this,
                                    ApiError.message(response, getString(R.string.admin_loading_error)),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(AdminCategoryTrashActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmPermanentDelete(Category category) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_confirm_permanent_delete_title)
                .setMessage(R.string.admin_confirm_permanent_delete_message)
                .setPositiveButton(R.string.admin_confirm_yes, (dialog, which) -> permanentDelete(category))
                .setNegativeButton(R.string.admin_confirm_cancel, null)
                .show();
    }

    private void permanentDelete(Category category) {
        RetrofitClient.getApiService().adminPermanentDeleteCategory(category.getId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            loadTrash();
                        } else {
                            // 409 (sudah ada suara) atau 422 (belum di-soft-delete) -- tampilkan
                            // pesan asli dari server, bukan dianggap error generik.
                            Toast.makeText(AdminCategoryTrashActivity.this,
                                    ApiError.message(response, getString(R.string.admin_loading_error)),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(AdminCategoryTrashActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        textEmpty.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerTrash.setVisibility(View.GONE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        textEmpty.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerTrash.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerTrash.setVisibility(View.GONE);
        textEmpty.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        textEmpty.setVisibility(View.GONE);
        recyclerTrash.setVisibility(View.GONE);
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}

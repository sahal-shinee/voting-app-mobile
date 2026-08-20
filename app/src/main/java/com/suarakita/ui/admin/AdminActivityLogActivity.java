package com.suarakita.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.model.ActivityLog;
import com.suarakita.model.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivityLogActivity extends AppCompatActivity {

    private RecyclerView recyclerLogs;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private TextView textError;
    private AdminActivityLogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_activity_log);

        findViewById(R.id.buttonBackArrow).setOnClickListener(v -> finish());

        recyclerLogs = findViewById(R.id.recyclerLogs);
        progressBar = findViewById(R.id.progressBar);
        textEmpty = findViewById(R.id.textEmpty);
        textError = findViewById(R.id.textError);

        adapter = new AdminActivityLogAdapter();
        recyclerLogs.setLayoutManager(new LinearLayoutManager(this));
        recyclerLogs.setAdapter(adapter);

        loadLogs();
    }

    private void loadLogs() {
        showLoading();

        RetrofitClient.getApiService().adminGetActivityLogs().enqueue(new Callback<ApiResponse<List<ActivityLog>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ActivityLog>>> call, Response<ApiResponse<List<ActivityLog>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<ActivityLog> logs = response.body().getData();
                    adapter.submitList(logs);
                    if (logs.isEmpty()) {
                        showEmpty();
                    } else {
                        showContent();
                    }
                } else {
                    showError(ApiError.message(response, getString(R.string.admin_loading_error)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ActivityLog>>> call, Throwable t) {
                showError(getString(R.string.error_no_connection));
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        textEmpty.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerLogs.setVisibility(View.GONE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        textEmpty.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerLogs.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerLogs.setVisibility(View.GONE);
        textEmpty.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        textEmpty.setVisibility(View.GONE);
        recyclerLogs.setVisibility(View.GONE);
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}

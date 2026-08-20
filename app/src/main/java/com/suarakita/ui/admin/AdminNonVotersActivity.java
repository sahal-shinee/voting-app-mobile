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
import com.suarakita.model.ApiResponse;
import com.suarakita.model.Student;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminNonVotersActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";

    private RecyclerView recyclerNonVoters;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private TextView textError;
    private NonVoterAdapter adapter;

    private int categoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_non_voters);

        categoryId = getIntent().getIntExtra(EXTRA_CATEGORY_ID, -1);

        findViewById(R.id.buttonBackArrow).setOnClickListener(v -> finish());

        recyclerNonVoters = findViewById(R.id.recyclerNonVoters);
        progressBar = findViewById(R.id.progressBar);
        textEmpty = findViewById(R.id.textEmpty);
        textError = findViewById(R.id.textError);

        adapter = new NonVoterAdapter();
        recyclerNonVoters.setLayoutManager(new LinearLayoutManager(this));
        recyclerNonVoters.setAdapter(adapter);

        loadNonVoters();
    }

    private void loadNonVoters() {
        showLoading();

        RetrofitClient.getApiService().adminGetNonVoters(categoryId).enqueue(new Callback<ApiResponse<List<Student>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Student>>> call, Response<ApiResponse<List<Student>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Student> nonVoters = response.body().getData();
                    adapter.submitList(nonVoters);
                    if (nonVoters.isEmpty()) {
                        showEmpty();
                    } else {
                        showContent();
                    }
                } else {
                    showError(ApiError.message(response, getString(R.string.admin_loading_error)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Student>>> call, Throwable t) {
                showError(getString(R.string.error_no_connection));
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        textEmpty.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerNonVoters.setVisibility(View.GONE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        textEmpty.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerNonVoters.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerNonVoters.setVisibility(View.GONE);
        textEmpty.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        textEmpty.setVisibility(View.GONE);
        recyclerNonVoters.setVisibility(View.GONE);
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}

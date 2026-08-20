package com.suarakita.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.data.SessionManager;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.Category;
import com.suarakita.model.VotingResults;
import com.suarakita.ui.auth.ChangePasswordActivity;
import com.suarakita.ui.auth.LoginActivity;
import com.suarakita.ui.common.BrandText;
import com.suarakita.ui.common.CategoryResultAdapter;
import com.suarakita.ui.common.CategoryResultPreview;
import com.suarakita.ui.common.DonutChartView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentDashboardActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerCategories;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private TextView textError;
    private View bannerChangePassword;
    private DonutChartView donutProgress;
    private TextView textProgressLabel;

    private CategoryAdapter categoryAdapter;
    private CategoryResultAdapter resultPreviewAdapter;
    private SessionManager session;
    private boolean isHasilTab = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        session = new SessionManager(this);

        TextView textBrand = findViewById(R.id.textBrand);
        textBrand.setText(BrandText.accent(getString(R.string.brand_suara), getString(R.string.brand_kita)));

        TextView textWelcome = findViewById(R.id.textWelcome);
        textWelcome.setText(getString(R.string.greeting_hello, session.getName()));

        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerCategories = findViewById(R.id.recyclerCategories);
        progressBar = findViewById(R.id.progressBar);
        textEmpty = findViewById(R.id.textEmpty);
        textError = findViewById(R.id.textError);

        bannerChangePassword = findViewById(R.id.bannerChangePassword);
        bannerChangePassword.findViewById(R.id.buttonChangePassword)
                .setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));

        donutProgress = findViewById(R.id.donutProgress);
        textProgressLabel = findViewById(R.id.textProgressLabel);

        categoryAdapter = new CategoryAdapter(this::onCategoryClick);
        resultPreviewAdapter = new CategoryResultAdapter(this::onCategoryClick);
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategories.setAdapter(categoryAdapter);

        swipeRefresh.setOnRefreshListener(this::loadCategories);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            isHasilTab = item.getItemId() == R.id.nav_hasil;
            recyclerCategories.setAdapter(isHasilTab ? resultPreviewAdapter : categoryAdapter);
            loadCategories();
            return true;
        });

        TextView buttonLogout = findViewById(R.id.buttonLogout);
        buttonLogout.setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bannerChangePassword.setVisibility(session.mustChangePassword() ? View.VISIBLE : View.GONE);
        loadCategories();
    }

    private void onCategoryClick(Category category) {
        Intent intent;
        if (!isHasilTab && !category.isHasVoted() && category.isVotingOpen()) {
            intent = new Intent(this, VotingActivity.class);
            intent.putExtra(VotingActivity.EXTRA_CATEGORY_ID, category.getId());
            intent.putExtra(VotingActivity.EXTRA_CATEGORY_NAME, category.getName());
        } else {
            intent = new Intent(this, ResultsActivity.class);
            intent.putExtra(ResultsActivity.EXTRA_CATEGORY_ID, category.getId());
            intent.putExtra(ResultsActivity.EXTRA_CATEGORY_NAME, category.getName());
        }
        startActivity(intent);
    }

    private void loadCategories() {
        showLoading();

        RetrofitClient.getApiService().getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Category> categories = response.body().getData();
                    updateProgress(categories);
                    if (categories.isEmpty()) {
                        showEmpty();
                        return;
                    }
                    showContent();
                    if (isHasilTab) {
                        loadHasilPreviews(categories);
                    } else {
                        categoryAdapter.submitList(categories);
                    }
                } else {
                    showError(ApiError.message(response, getString(R.string.dashboard_error)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                showError(getString(R.string.error_no_connection));
            }
        });
    }

    private void updateProgress(List<Category> categories) {
        int total = categories.size();
        int voted = 0;
        for (Category category : categories) {
            if (category.isHasVoted()) {
                voted++;
            }
        }

        float percentage = total > 0 ? (voted * 100f / total) : 0f;
        int color = ContextCompat.getColor(this, R.color.color_primary);

        List<DonutChartView.Segment> segments = new ArrayList<>();
        segments.add(new DonutChartView.Segment(percentage, color));
        donutProgress.setSegments(segments);
        donutProgress.setCenterText(voted + "/" + total);

        textProgressLabel.setText(getString(R.string.dashboard_progress_label, voted, total));
    }

    private void loadHasilPreviews(List<Category> categories) {
        List<CategoryResultPreview> previews = new ArrayList<>();
        for (Category category : categories) {
            previews.add(new CategoryResultPreview(category));
        }
        resultPreviewAdapter.submitList(previews);

        for (int i = 0; i < categories.size(); i++) {
            int position = i;
            RetrofitClient.getApiService().getResults(categories.get(i).getId())
                    .enqueue(new Callback<ApiResponse<VotingResults>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<VotingResults>> call, Response<ApiResponse<VotingResults>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                                resultPreviewAdapter.updateResults(position, response.body().getData());
                            } else if (ApiError.statusCode(response) == 403) {
                                resultPreviewAdapter.markLocked(position);
                            } else {
                                resultPreviewAdapter.markError(position);
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<VotingResults>> call, Throwable t) {
                            resultPreviewAdapter.markError(position);
                        }
                    });
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        textEmpty.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerCategories.setVisibility(View.GONE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        textEmpty.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerCategories.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerCategories.setVisibility(View.GONE);
        textEmpty.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        textEmpty.setVisibility(View.GONE);
        recyclerCategories.setVisibility(View.GONE);
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }

    private void logout() {
        RetrofitClient.getApiService().logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                goToLogin();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                goToLogin();
            }
        });
    }

    private void goToLogin() {
        session.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

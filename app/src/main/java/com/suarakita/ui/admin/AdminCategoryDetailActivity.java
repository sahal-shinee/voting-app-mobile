package com.suarakita.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.model.AdminCandidate;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.Category;
import com.suarakita.model.CategoryToggleRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCategoryDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";

    private TextView textTitle;
    private View scrollContent;
    private TextView textError;
    private ProgressBar progressBar;
    private SwitchMaterial switchVotingOpen;
    private SwitchMaterial switchShowResults;
    private RecyclerView recyclerCandidates;
    private TextView textNoCandidates;
    private AdminCandidateAdapter candidateAdapter;

    private int categoryId;
    private Category currentCategory;
    private boolean isUpdatingSwitches = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_category_detail);

        categoryId = getIntent().getIntExtra(EXTRA_CATEGORY_ID, -1);

        textTitle = findViewById(R.id.textTitle);
        textTitle.setText(getIntent().getStringExtra(EXTRA_CATEGORY_NAME));

        scrollContent = findViewById(R.id.scrollContent);
        textError = findViewById(R.id.textError);
        progressBar = findViewById(R.id.progressBar);
        switchVotingOpen = findViewById(R.id.switchVotingOpen);
        switchShowResults = findViewById(R.id.switchShowResults);
        recyclerCandidates = findViewById(R.id.recyclerCandidates);
        textNoCandidates = findViewById(R.id.textNoCandidates);

        findViewById(R.id.buttonBackArrow).setOnClickListener(v -> finish());
        findViewById(R.id.buttonEditCategory).setOnClickListener(v -> openCategoryEdit());
        findViewById(R.id.buttonDeleteCategory).setOnClickListener(v -> confirmDeleteCategory());
        findViewById(R.id.buttonViewResults).setOnClickListener(v -> openResults());
        findViewById(R.id.buttonAddCandidate).setOnClickListener(v -> openCandidateForm(null));

        candidateAdapter = new AdminCandidateAdapter(new AdminCandidateAdapter.Listener() {
            @Override
            public void onEdit(AdminCandidate candidate) {
                openCandidateForm(candidate);
            }

            @Override
            public void onDelete(AdminCandidate candidate) {
                confirmDeleteCandidate(candidate);
            }
        });
        recyclerCandidates.setLayoutManager(new LinearLayoutManager(this));
        recyclerCandidates.setAdapter(candidateAdapter);

        switchVotingOpen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingSwitches) {
                toggleCategory();
            }
        });
        switchShowResults.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingSwitches) {
                toggleCategory();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategory();
    }

    private void loadCategory() {
        showLoading();

        RetrofitClient.getApiService().adminGetCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Category match = null;
                    for (Category category : response.body().getData()) {
                        if (category.getId() == categoryId) {
                            match = category;
                            break;
                        }
                    }
                    if (match == null) {
                        // Kategori sudah tidak ada (mis. dihapus dari layar lain) -- balik ke list.
                        finish();
                        return;
                    }
                    showCategory(match);
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

    private void showCategory(Category category) {
        currentCategory = category;
        textTitle.setText(category.getName());

        isUpdatingSwitches = true;
        switchVotingOpen.setChecked(category.isVotingOpen());
        switchShowResults.setChecked(category.isShowLiveResults());
        isUpdatingSwitches = false;

        showContent();
        loadCandidates();
    }

    private void loadCandidates() {
        RetrofitClient.getApiService().adminGetCandidates(categoryId).enqueue(new Callback<ApiResponse<List<AdminCandidate>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<AdminCandidate>>> call, Response<ApiResponse<List<AdminCandidate>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AdminCandidate> candidates = response.body().getData();
                    candidateAdapter.submitList(candidates);
                    boolean hasCandidates = candidates != null && !candidates.isEmpty();
                    recyclerCandidates.setVisibility(hasCandidates ? View.VISIBLE : View.GONE);
                    textNoCandidates.setVisibility(hasCandidates ? View.GONE : View.VISIBLE);
                } else {
                    Toast.makeText(AdminCategoryDetailActivity.this,
                            ApiError.message(response, getString(R.string.admin_loading_error)),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<AdminCandidate>>> call, Throwable t) {
                Toast.makeText(AdminCategoryDetailActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleCategory() {
        if (currentCategory == null) {
            return;
        }

        boolean votingOpen = switchVotingOpen.isChecked();
        boolean showResults = switchShowResults.isChecked();

        RetrofitClient.getApiService()
                .adminToggleCategory(categoryId, new CategoryToggleRequest(votingOpen, showResults))
                .enqueue(new Callback<ApiResponse<Category>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Category>> call, Response<ApiResponse<Category>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            currentCategory = response.body().getData();
                        } else {
                            revertSwitches(ApiError.message(response, getString(R.string.admin_toggle_failed)));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Category>> call, Throwable t) {
                        revertSwitches(getString(R.string.error_no_connection));
                    }
                });
    }

    private void revertSwitches(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        isUpdatingSwitches = true;
        switchVotingOpen.setChecked(currentCategory.isVotingOpen());
        switchShowResults.setChecked(currentCategory.isShowLiveResults());
        isUpdatingSwitches = false;
    }

    private void openCategoryEdit() {
        if (currentCategory == null) {
            return;
        }
        Intent intent = new Intent(this, AdminCategoryFormActivity.class);
        intent.putExtra(AdminCategoryFormActivity.EXTRA_CATEGORY_ID, currentCategory.getId());
        intent.putExtra(AdminCategoryFormActivity.EXTRA_CATEGORY_NAME, currentCategory.getName());
        intent.putExtra(AdminCategoryFormActivity.EXTRA_CATEGORY_DESCRIPTION, currentCategory.getDescription());
        intent.putExtra(AdminCategoryFormActivity.EXTRA_CATEGORY_VOTING_START_AT, currentCategory.getVotingStartAt());
        intent.putExtra(AdminCategoryFormActivity.EXTRA_CATEGORY_VOTING_END_AT, currentCategory.getVotingEndAt());
        startActivity(intent);
    }

    private void confirmDeleteCategory() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_confirm_delete_category_title)
                .setMessage(R.string.admin_confirm_delete_category_message)
                .setPositiveButton(R.string.admin_confirm_yes, (dialog, which) -> deleteCategory())
                .setNegativeButton(R.string.admin_confirm_cancel, null)
                .show();
    }

    private void deleteCategory() {
        RetrofitClient.getApiService().adminDeleteCategory(categoryId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            finish();
                        } else {
                            Toast.makeText(AdminCategoryDetailActivity.this,
                                    ApiError.message(response, getString(R.string.admin_loading_error)),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(AdminCategoryDetailActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmDeleteCandidate(AdminCandidate candidate) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_confirm_delete_candidate_title)
                .setMessage(R.string.admin_confirm_delete_candidate_message)
                .setPositiveButton(R.string.admin_confirm_yes, (dialog, which) -> deleteCandidate(candidate))
                .setNegativeButton(R.string.admin_confirm_cancel, null)
                .show();
    }

    private void deleteCandidate(AdminCandidate candidate) {
        RetrofitClient.getApiService().adminDeleteCandidate(candidate.getId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            loadCandidates();
                        } else {
                            Toast.makeText(AdminCategoryDetailActivity.this,
                                    ApiError.message(response, getString(R.string.admin_loading_error)),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(AdminCategoryDetailActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openCandidateForm(AdminCandidate candidate) {
        Intent intent = new Intent(this, AdminCandidateFormActivity.class);
        intent.putExtra(AdminCandidateFormActivity.EXTRA_CATEGORY_ID, categoryId);
        if (candidate != null) {
            intent.putExtra(AdminCandidateFormActivity.EXTRA_CANDIDATE_ID, candidate.getId());
            intent.putExtra(AdminCandidateFormActivity.EXTRA_CANDIDATE_NAME, candidate.getName());
            intent.putExtra(AdminCandidateFormActivity.EXTRA_CANDIDATE_DESCRIPTION, candidate.getDescription());
            intent.putExtra(AdminCandidateFormActivity.EXTRA_CANDIDATE_PHOTO_URL, candidate.getPhotoUrl());
        }
        startActivity(intent);
    }

    private void openResults() {
        if (currentCategory == null) {
            return;
        }
        Intent intent = new Intent(this, AdminResultsActivity.class);
        intent.putExtra(AdminResultsActivity.EXTRA_CATEGORY_ID, currentCategory.getId());
        intent.putExtra(AdminResultsActivity.EXTRA_CATEGORY_NAME, currentCategory.getName());
        startActivity(intent);
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        textError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}

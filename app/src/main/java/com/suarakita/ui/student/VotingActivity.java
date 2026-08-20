package com.suarakita.ui.student;

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

import com.google.android.material.button.MaterialButton;
import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.Candidate;
import com.suarakita.model.VoteCreated;
import com.suarakita.model.VoteRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VotingActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";

    private RecyclerView recyclerCandidates;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private TextView textError;
    private MaterialButton buttonVote;

    private CandidateAdapter adapter;
    private int categoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voting);

        categoryId = getIntent().getIntExtra(EXTRA_CATEGORY_ID, -1);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);

        TextView textTitle = findViewById(R.id.textTitle);
        textTitle.setText(categoryName);

        findViewById(R.id.buttonBackArrow).setOnClickListener(v -> finish());

        recyclerCandidates = findViewById(R.id.recyclerCandidates);
        progressBar = findViewById(R.id.progressBar);
        textEmpty = findViewById(R.id.textEmpty);
        textError = findViewById(R.id.textError);
        buttonVote = findViewById(R.id.buttonVote);

        adapter = new CandidateAdapter(candidate -> buttonVote.setEnabled(true));
        recyclerCandidates.setLayoutManager(new LinearLayoutManager(this));
        recyclerCandidates.setAdapter(adapter);

        buttonVote.setOnClickListener(v -> confirmAndVote());

        loadCandidates();
    }

    private void loadCandidates() {
        showLoading();

        RetrofitClient.getApiService().getCandidates(categoryId).enqueue(new Callback<ApiResponse<List<Candidate>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Candidate>>> call, Response<ApiResponse<List<Candidate>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Candidate> candidates = response.body().getData();
                    adapter.submitList(candidates);
                    if (candidates.isEmpty()) {
                        showEmpty();
                    } else {
                        showContent();
                    }
                } else {
                    showError(ApiError.message(response, getString(R.string.voting_error)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Candidate>>> call, Throwable t) {
                showError(getString(R.string.error_no_connection));
            }
        });
    }

    private void confirmAndVote() {
        Candidate selected = adapter.getSelectedCandidate();
        if (selected == null) {
            Toast.makeText(this, R.string.voting_error_no_selection, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.voting_confirm_title)
                .setMessage(R.string.voting_confirm_message)
                .setPositiveButton(R.string.voting_confirm_yes, (dialog, which) -> submitVote(selected))
                .setNegativeButton(R.string.voting_confirm_cancel, null)
                .show();
    }

    private void submitVote(Candidate candidate) {
        buttonVote.setEnabled(false);

        RetrofitClient.getApiService()
                .createVote(new VoteRequest(categoryId, candidate.getId()))
                .enqueue(new Callback<ApiResponse<VoteCreated>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<VoteCreated>> call, Response<ApiResponse<VoteCreated>> response) {
                        if (response.isSuccessful()) {
                            goToConfirmation(candidate);
                        } else {
                            buttonVote.setEnabled(true);
                            Toast.makeText(VotingActivity.this,
                                    ApiError.message(response, getString(R.string.voting_error_generic)),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<VoteCreated>> call, Throwable t) {
                        buttonVote.setEnabled(true);
                        Toast.makeText(VotingActivity.this, R.string.error_no_connection, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToConfirmation(Candidate candidate) {
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_CANDIDATE_NAME, candidate.getName());
        startActivity(intent);
        finish();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        textEmpty.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerCandidates.setVisibility(View.GONE);
        buttonVote.setEnabled(false);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        textEmpty.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerCandidates.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        textError.setVisibility(View.GONE);
        recyclerCandidates.setVisibility(View.GONE);
        textEmpty.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        textEmpty.setVisibility(View.GONE);
        recyclerCandidates.setVisibility(View.GONE);
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}

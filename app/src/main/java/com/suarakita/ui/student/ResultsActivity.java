package com.suarakita.ui.student;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.CandidateResult;
import com.suarakita.model.MyVote;
import com.suarakita.model.VotingResults;
import com.suarakita.ui.common.BrandText;
import com.suarakita.ui.common.DonutChartView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultsActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";

    private RecyclerView recyclerResults;
    private ProgressBar progressBar;
    private TextView textError;
    private TextView textTotalVotes;
    private TextView textResultsLabel;
    private View cardResultHero;
    private DonutChartView donutChart;

    private ResultAdapter adapter;
    private int categoryId;
    private int yourCandidateId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        categoryId = getIntent().getIntExtra(EXTRA_CATEGORY_ID, -1);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);

        TextView textSectionLabel = findViewById(R.id.textSectionLabel);
        textSectionLabel.setText(BrandText.accent(
                getString(R.string.results_title_regular),
                getString(R.string.results_title_script)
        ));

        TextView textTitle = findViewById(R.id.textTitle);
        textTitle.setText(categoryName);

        findViewById(R.id.buttonBackArrow).setOnClickListener(v -> finish());

        recyclerResults = findViewById(R.id.recyclerResults);
        progressBar = findViewById(R.id.progressBar);
        textError = findViewById(R.id.textError);
        textTotalVotes = findViewById(R.id.textTotalVotes);
        textResultsLabel = findViewById(R.id.textResultsLabel);
        cardResultHero = findViewById(R.id.cardResultHero);
        donutChart = findViewById(R.id.donutChart);

        adapter = new ResultAdapter();
        recyclerResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerResults.setAdapter(adapter);

        loadMyVoteThenResults();
    }

    private void loadMyVoteThenResults() {
        showLoading();

        RetrofitClient.getApiService().getMyVotes().enqueue(new Callback<ApiResponse<List<MyVote>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MyVote>>> call, Response<ApiResponse<List<MyVote>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    for (MyVote vote : response.body().getData()) {
                        if (vote.getCategoryId() == categoryId) {
                            yourCandidateId = vote.getCandidateId();
                            break;
                        }
                    }
                }
                loadResults();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MyVote>>> call, Throwable t) {
                // Tidak fatal -- lanjut tanpa highlight "pilihanmu".
                loadResults();
            }
        });
    }

    private void loadResults() {
        RetrofitClient.getApiService().getResults(categoryId).enqueue(new Callback<ApiResponse<VotingResults>>() {
            @Override
            public void onResponse(Call<ApiResponse<VotingResults>> call, Response<ApiResponse<VotingResults>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    showResults(response.body().getData());
                } else if (ApiError.statusCode(response) == 403) {
                    showLocked();
                } else {
                    showError(ApiError.message(response, getString(R.string.results_error)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VotingResults>> call, Throwable t) {
                showError(getString(R.string.error_no_connection));
            }
        });
    }

    private void showResults(VotingResults results) {
        progressBar.setVisibility(View.GONE);

        List<CandidateResult> candidates = results.getCandidates();
        if (candidates == null || candidates.isEmpty()) {
            cardResultHero.setVisibility(View.GONE);
            textResultsLabel.setVisibility(View.GONE);
            recyclerResults.setVisibility(View.GONE);
            textError.setText(R.string.results_no_candidates);
            textError.setVisibility(View.VISIBLE);
            return;
        }

        textError.setVisibility(View.GONE);
        cardResultHero.setVisibility(View.VISIBLE);
        textResultsLabel.setVisibility(View.VISIBLE);
        recyclerResults.setVisibility(View.VISIBLE);

        textTotalVotes.setText(getString(R.string.results_total_votes, results.getTotalVotes()));
        adapter.submitList(candidates, yourCandidateId);

        animateDonut(candidates, results.getTotalVotes());
    }

    private void animateDonut(List<CandidateResult> candidates, int totalVotes) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(700);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            List<DonutChartView.Segment> segments = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                int color = DonutChartView.colorForIndex(this, i);
                segments.add(new DonutChartView.Segment((float) candidates.get(i).getPercentage() * fraction, color));
            }
            donutChart.setSegments(segments);
            donutChart.setCenterText(String.valueOf(Math.round(totalVotes * fraction)));
        });
        animator.start();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        textError.setVisibility(View.GONE);
        cardResultHero.setVisibility(View.GONE);
        textResultsLabel.setVisibility(View.GONE);
        recyclerResults.setVisibility(View.GONE);
    }

    private void showLocked() {
        progressBar.setVisibility(View.GONE);
        cardResultHero.setVisibility(View.GONE);
        textResultsLabel.setVisibility(View.GONE);
        recyclerResults.setVisibility(View.GONE);
        textError.setText(R.string.results_locked);
        textError.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        cardResultHero.setVisibility(View.GONE);
        textResultsLabel.setVisibility(View.GONE);
        recyclerResults.setVisibility(View.GONE);
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}

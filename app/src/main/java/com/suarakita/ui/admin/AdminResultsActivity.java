package com.suarakita.ui.admin;

import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.CandidateResult;
import com.suarakita.model.FastestVoter;
import com.suarakita.model.VotingResults;
import com.suarakita.ui.common.DonutChartView;
import com.suarakita.ui.student.ResultAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminResultsActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";

    private View scrollContent;
    private TextView textTotalVotes;
    private TextView textNoCandidates;
    private TextView textError;
    private ProgressBar progressBar;
    private LinearLayout containerFastestVoters;
    private RecyclerView recyclerResults;
    private DonutChartView donutChart;
    private TextView textQuorum;
    private LinearProgressIndicator progressQuorum;
    private ResultAdapter adapter;

    private int categoryId;
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_results);

        categoryId = getIntent().getIntExtra(EXTRA_CATEGORY_ID, -1);
        categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);

        TextView textTitle = findViewById(R.id.textTitle);
        textTitle.setText(categoryName);

        findViewById(R.id.buttonBackArrow).setOnClickListener(v -> finish());

        scrollContent = findViewById(R.id.scrollContent);
        textTotalVotes = findViewById(R.id.textTotalVotes);
        textNoCandidates = findViewById(R.id.textNoCandidates);
        textError = findViewById(R.id.textError);
        progressBar = findViewById(R.id.progressBar);
        containerFastestVoters = findViewById(R.id.containerFastestVoters);
        donutChart = findViewById(R.id.donutChart);
        textQuorum = findViewById(R.id.textQuorum);
        progressQuorum = findViewById(R.id.progressQuorum);

        recyclerResults = findViewById(R.id.recyclerResults);
        adapter = new ResultAdapter();
        recyclerResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerResults.setAdapter(adapter);

        findViewById(R.id.buttonViewNonVoters).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminNonVotersActivity.class);
            intent.putExtra(AdminNonVotersActivity.EXTRA_CATEGORY_ID, categoryId);
            startActivity(intent);
        });
        findViewById(R.id.buttonExport).setOnClickListener(v -> exportCsv());

        loadResults();
    }

    private void loadResults() {
        showLoading();

        RetrofitClient.getApiService().adminGetResults(categoryId).enqueue(new Callback<ApiResponse<VotingResults>>() {
            @Override
            public void onResponse(Call<ApiResponse<VotingResults>> call, Response<ApiResponse<VotingResults>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    showResults(response.body().getData());
                } else {
                    showError(ApiError.message(response, getString(R.string.admin_loading_error)));
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
        textError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.VISIBLE);

        Integer votedCount = results.getVotedCount();
        textTotalVotes.setText(getString(R.string.admin_results_voted_count, votedCount != null ? votedCount : 0));

        int voted = votedCount != null ? votedCount : 0;
        int total = results.getTotalStudents() != null ? results.getTotalStudents() : 0;
        int percentage = total > 0 ? Math.round(voted * 100f / total) : 0;
        textQuorum.setText(getString(R.string.admin_results_quorum_label, voted, total, percentage));
        progressQuorum.setProgress(percentage);

        List<CandidateResult> candidates = results.getCandidates();
        boolean hasCandidates = candidates != null && !candidates.isEmpty();
        recyclerResults.setVisibility(hasCandidates ? View.VISIBLE : View.GONE);
        donutChart.setVisibility(hasCandidates ? View.VISIBLE : View.GONE);
        textNoCandidates.setVisibility(hasCandidates ? View.GONE : View.VISIBLE);
        adapter.submitList(candidates, -1);

        if (hasCandidates) {
            animateDonut(candidates, results.getTotalVotes());
        }

        containerFastestVoters.removeAllViews();
        if (results.getFastestVoters() == null || results.getFastestVoters().isEmpty()) {
            TextView textEmpty = new TextView(this);
            textEmpty.setText(R.string.admin_results_no_fastest);
            textEmpty.setTextColor(getResources().getColor(R.color.color_text_secondary, getTheme()));
            containerFastestVoters.addView(textEmpty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        int rank = 1;
        for (FastestVoter voter : results.getFastestVoters()) {
            View row = inflater.inflate(R.layout.item_fastest_voter, containerFastestVoters, false);
            ((TextView) row.findViewById(R.id.textRank)).setText(String.valueOf(rank));
            ((TextView) row.findViewById(R.id.textName)).setText(voter.getName() + " (" + voter.getNis() + ")");
            ((TextView) row.findViewById(R.id.textTime)).setText(voter.getVotedAt());
            containerFastestVoters.addView(row);
            rank++;
        }
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

    private void exportCsv() {
        RetrofitClient.getApiService().adminExportCategoryCsv(categoryId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                boolean saved = response.isSuccessful() && response.body() != null && saveCsvToDownloads(response.body());
                Toast.makeText(AdminResultsActivity.this,
                        saved ? R.string.admin_export_success : R.string.admin_export_failed,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(AdminResultsActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean saveCsvToDownloads(ResponseBody body) {
        String safeName = (categoryName != null ? categoryName : "kategori").replaceAll("[^a-zA-Z0-9_-]+", "-");
        String fileName = "hasil-" + safeName + "-" + System.currentTimeMillis() + ".csv";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");

        Uri itemUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (itemUri == null) {
            return false;
        }

        try (OutputStream out = getContentResolver().openOutputStream(itemUri);
             InputStream in = body.byteStream()) {
            if (out == null) {
                return false;
            }
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        textError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}

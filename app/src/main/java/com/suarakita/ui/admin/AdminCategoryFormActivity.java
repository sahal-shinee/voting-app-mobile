package com.suarakita.ui.admin;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.Category;
import com.suarakita.model.CategoryRequest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCategoryFormActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";
    public static final String EXTRA_CATEGORY_DESCRIPTION = "category_description";
    public static final String EXTRA_CATEGORY_VOTING_START_AT = "category_voting_start_at";
    public static final String EXTRA_CATEGORY_VOTING_END_AT = "category_voting_end_at";

    private static final SimpleDateFormat DB_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("in", "ID"));

    private TextInputEditText inputName;
    private TextInputEditText inputDescription;
    private TextInputEditText inputVotingStart;
    private TextInputEditText inputVotingEnd;
    private TextView textError;
    private MaterialButton buttonSave;
    private ProgressBar progressBar;

    private int categoryId = -1;
    private String votingStartAt;
    private String votingEndAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_category_form);

        categoryId = getIntent().getIntExtra(EXTRA_CATEGORY_ID, -1);
        boolean isEdit = categoryId != -1;

        TextView textTitle = findViewById(R.id.textTitle);
        textTitle.setText(isEdit ? R.string.admin_category_form_title_edit : R.string.admin_category_form_title_add);

        findViewById(R.id.buttonBackArrow).setOnClickListener(v -> finish());

        inputName = findViewById(R.id.inputName);
        inputDescription = findViewById(R.id.inputDescription);
        inputVotingStart = findViewById(R.id.inputVotingStart);
        inputVotingEnd = findViewById(R.id.inputVotingEnd);
        textError = findViewById(R.id.textError);
        buttonSave = findViewById(R.id.buttonSave);
        progressBar = findViewById(R.id.progressBar);

        if (isEdit) {
            inputName.setText(getIntent().getStringExtra(EXTRA_CATEGORY_NAME));
            inputDescription.setText(getIntent().getStringExtra(EXTRA_CATEGORY_DESCRIPTION));
            votingStartAt = getIntent().getStringExtra(EXTRA_CATEGORY_VOTING_START_AT);
            votingEndAt = getIntent().getStringExtra(EXTRA_CATEGORY_VOTING_END_AT);
            refreshScheduleDisplay();
        }

        inputVotingStart.setOnClickListener(v -> pickDateTime(true));
        inputVotingEnd.setOnClickListener(v -> pickDateTime(false));
        findViewById(R.id.buttonClearSchedule).setOnClickListener(v -> {
            votingStartAt = null;
            votingEndAt = null;
            refreshScheduleDisplay();
        });

        buttonSave.setOnClickListener(v -> save());
    }

    private void pickDateTime(boolean isStart) {
        Calendar calendar = Calendar.getInstance();
        String existing = isStart ? votingStartAt : votingEndAt;
        if (existing != null) {
            try {
                calendar.setTime(DB_FORMAT.parse(existing));
            } catch (Exception ignored) {
                // Kalau parsing gagal, biarkan kalender mulai dari waktu sekarang.
            }
        }

        new DatePickerDialog(this, (datePicker, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);

            new TimePickerDialog(this, (timePicker, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                String dbValue = DB_FORMAT.format(calendar.getTime());
                if (isStart) {
                    votingStartAt = dbValue;
                } else {
                    votingEndAt = dbValue;
                }
                refreshScheduleDisplay();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void refreshScheduleDisplay() {
        inputVotingStart.setText(formatForDisplay(votingStartAt));
        inputVotingEnd.setText(formatForDisplay(votingEndAt));
    }

    private String formatForDisplay(String dbValue) {
        if (dbValue == null) {
            return "";
        }
        try {
            return DISPLAY_FORMAT.format(DB_FORMAT.parse(dbValue));
        } catch (Exception e) {
            return "";
        }
    }

    private void save() {
        String name = inputName.getText() == null ? "" : inputName.getText().toString().trim();
        String description = inputDescription.getText() == null ? "" : inputDescription.getText().toString().trim();

        textError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(name)) {
            showError(getString(R.string.admin_category_error_name_empty));
            return;
        }

        if (votingStartAt != null && votingEndAt != null && votingEndAt.compareTo(votingStartAt) <= 0) {
            showError(getString(R.string.admin_category_error_schedule_order));
            return;
        }

        setLoading(true);
        CategoryRequest request = new CategoryRequest(name, description, votingStartAt, votingEndAt);

        Call<ApiResponse<Category>> call = categoryId == -1
                ? RetrofitClient.getApiService().adminCreateCategory(request)
                : RetrofitClient.getApiService().adminUpdateCategory(categoryId, request);

        call.enqueue(new Callback<ApiResponse<Category>>() {
            @Override
            public void onResponse(Call<ApiResponse<Category>> call, Response<ApiResponse<Category>> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(AdminCategoryFormActivity.this, R.string.admin_category_saved, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    showError(ApiError.message(response, getString(R.string.admin_loading_error)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Category>> call, Throwable t) {
                setLoading(false);
                showError(getString(R.string.error_no_connection));
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonSave.setEnabled(!loading);
    }

    private void showError(String message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}

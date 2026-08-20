package com.suarakita.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.data.SessionManager;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.ChangePasswordRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Opsional, bukan paksaan -- diakses lewat banner pengingat di dashboard, bukan
// gate yang memblokir navigasi. Server tetap menolak voting (403) selama
// must_change_password=1, jadi keamanan inti tidak hilang walau layar ini
// sekarang bisa diabaikan/di-back.
public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText inputOldPassword;
    private TextInputEditText inputNewPassword;
    private TextInputEditText inputConfirmPassword;
    private MaterialButton buttonSubmit;
    private ProgressBar progressBar;
    private TextView textError;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        sessionManager = new SessionManager(this);

        ImageView buttonBackArrow = findViewById(R.id.buttonBackArrow);
        buttonBackArrow.setOnClickListener(v -> finish());

        inputOldPassword = findViewById(R.id.inputOldPassword);
        inputNewPassword = findViewById(R.id.inputNewPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        progressBar = findViewById(R.id.progressBar);
        textError = findViewById(R.id.textError);

        buttonSubmit.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String oldPassword = textOf(inputOldPassword);
        String newPassword = textOf(inputNewPassword);
        String confirmPassword = textOf(inputConfirmPassword);

        textError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            showError(getString(R.string.change_password_error_empty));
            return;
        }

        if (newPassword.length() < 6) {
            showError(getString(R.string.change_password_error_short));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError(getString(R.string.change_password_error_mismatch));
            return;
        }

        setLoading(true);

        RetrofitClient.getApiService()
                .changePassword(new ChangePasswordRequest(oldPassword, newPassword))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        setLoading(false);

                        if (response.isSuccessful()) {
                            onChangeSuccess();
                        } else {
                            showError(ApiError.message(response, getString(R.string.change_password_error_generic)));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        setLoading(false);
                        showError(getString(R.string.error_no_connection));
                    }
                });
    }

    private void onChangeSuccess() {
        sessionManager.updateMustChangePassword(false);
        finish();
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonSubmit.setEnabled(!loading);
    }

    private void showError(String message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}

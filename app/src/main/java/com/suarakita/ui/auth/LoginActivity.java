package com.suarakita.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.data.SessionManager;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.LoginRequest;
import com.suarakita.model.LoginResult;
import com.suarakita.ui.admin.AdminDashboardActivity;
import com.suarakita.ui.common.BrandText;
import com.suarakita.ui.student.StudentDashboardActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText inputIdentifier;
    private TextInputEditText inputPassword;
    private MaterialButton buttonLogin;
    private TextView textError;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        TextView textTitle = findViewById(R.id.textTitle);
        textTitle.setText(BrandText.accent(getString(R.string.login_title_regular), getString(R.string.login_title_script)));

        inputIdentifier = findViewById(R.id.inputIdentifier);
        inputPassword = findViewById(R.id.inputPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textError = findViewById(R.id.textError);

        buttonLogin.setOnClickListener(v -> attemptLogin());

        animateEntrance();
    }

    private void animateEntrance() {
        View contentRoot = findViewById(R.id.contentRoot);
        contentRoot.setAlpha(0f);
        contentRoot.setTranslationY(48f);
        contentRoot.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(80)
                .setDuration(420)
                .start();
    }

    private void attemptLogin() {
        String identifier = inputIdentifier.getText() == null ? "" : inputIdentifier.getText().toString().trim();
        String password = inputPassword.getText() == null ? "" : inputPassword.getText().toString();

        textError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(identifier) || TextUtils.isEmpty(password)) {
            showError(getString(R.string.login_error_empty));
            return;
        }

        setLoading(true);

        RetrofitClient.getApiService()
                .login(new LoginRequest(identifier, password))
                .enqueue(new Callback<ApiResponse<LoginResult>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<LoginResult>> call, Response<ApiResponse<LoginResult>> response) {
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            onLoginSuccess(response.body().getData());
                        } else {
                            showError(ApiError.message(response, getString(R.string.login_error_generic)));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<LoginResult>> call, Throwable t) {
                        setLoading(false);
                        showError(getString(R.string.error_no_connection));
                    }
                });
    }

    private void onLoginSuccess(LoginResult result) {
        sessionManager.saveSession(
                result.getToken(),
                result.getUser().getId(),
                result.getUser().getName(),
                result.getUser().getRole(),
                result.getUser().isMustChangePassword()
        );

        Intent intent = result.getUser().isAdmin()
                ? new Intent(this, AdminDashboardActivity.class)
                : new Intent(this, StudentDashboardActivity.class);

        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        buttonLogin.setEnabled(!loading);
        buttonLogin.setText(loading ? R.string.login_loading : R.string.login_button);
    }

    private void showError(String message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}

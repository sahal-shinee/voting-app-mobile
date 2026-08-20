package com.suarakita.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.suarakita.R;
import com.suarakita.data.SessionManager;
import com.suarakita.ui.admin.AdminDashboardActivity;
import com.suarakita.ui.common.BrandText;
import com.suarakita.ui.student.StudentDashboardActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long DELAY_MS = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView textBrand = findViewById(R.id.textBrand);
        textBrand.setText(BrandText.accent(getString(R.string.brand_suara), getString(R.string.brand_kita)));

        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, DELAY_MS);
    }

    private void navigateNext() {
        SessionManager session = new SessionManager(this);
        Intent intent;

        if (!session.isLoggedIn()) {
            intent = new Intent(this, LoginActivity.class);
        } else if (session.isAdmin()) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(this, StudentDashboardActivity.class);
        }

        startActivity(intent);
        finish();
    }
}

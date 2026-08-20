package com.suarakita.ui.student;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.suarakita.R;
import com.suarakita.ui.common.BrandText;

public class ConfirmationActivity extends AppCompatActivity {

    public static final String EXTRA_CANDIDATE_NAME = "candidate_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        TextView textTitle = findViewById(R.id.textTitle);
        textTitle.setText(BrandText.accent(
                getString(R.string.confirmation_title_regular),
                getString(R.string.confirmation_title_script)
        ));

        String candidateName = getIntent().getStringExtra(EXTRA_CANDIDATE_NAME);

        TextView textCandidateName = findViewById(R.id.textCandidateName);
        textCandidateName.setText(candidateName);

        MaterialButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());
    }
}

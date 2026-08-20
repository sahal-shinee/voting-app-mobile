package com.suarakita.ui.admin;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.model.AdminCandidate;
import com.suarakita.model.ApiResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCandidateFormActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_CANDIDATE_ID = "candidate_id";
    public static final String EXTRA_CANDIDATE_NAME = "candidate_name";
    public static final String EXTRA_CANDIDATE_DESCRIPTION = "candidate_description";
    public static final String EXTRA_CANDIDATE_PHOTO_URL = "candidate_photo_url";

    private ImageView imagePhoto;
    private TextInputEditText inputName;
    private TextInputEditText inputDescription;
    private TextView textError;
    private MaterialButton buttonSave;
    private ProgressBar progressBar;

    private int categoryId;
    private int candidateId = -1;
    private Uri pickedPhotoUri;

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    pickedPhotoUri = uri;
                    Glide.with(this).load(uri).centerCrop().into(imagePhoto);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_candidate_form);

        categoryId = getIntent().getIntExtra(EXTRA_CATEGORY_ID, -1);
        candidateId = getIntent().getIntExtra(EXTRA_CANDIDATE_ID, -1);
        boolean isEdit = candidateId != -1;

        TextView textTitle = findViewById(R.id.textTitle);
        textTitle.setText(isEdit ? R.string.admin_candidate_form_title_edit : R.string.admin_candidate_form_title_add);

        findViewById(R.id.buttonBackArrow).setOnClickListener(v -> finish());

        imagePhoto = findViewById(R.id.imagePhoto);
        inputName = findViewById(R.id.inputName);
        inputDescription = findViewById(R.id.inputDescription);
        textError = findViewById(R.id.textError);
        buttonSave = findViewById(R.id.buttonSave);
        progressBar = findViewById(R.id.progressBar);

        if (isEdit) {
            inputName.setText(getIntent().getStringExtra(EXTRA_CANDIDATE_NAME));
            inputDescription.setText(getIntent().getStringExtra(EXTRA_CANDIDATE_DESCRIPTION));
            Glide.with(this)
                    .load(getIntent().getStringExtra(EXTRA_CANDIDATE_PHOTO_URL))
                    .placeholder(R.drawable.ic_placeholder_photo)
                    .error(R.drawable.ic_placeholder_photo)
                    .centerCrop()
                    .into(imagePhoto);
        }

        findViewById(R.id.buttonPickPhoto).setOnClickListener(v -> photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));

        buttonSave.setOnClickListener(v -> save());
    }

    private void save() {
        String name = inputName.getText() == null ? "" : inputName.getText().toString().trim();
        String description = inputDescription.getText() == null ? "" : inputDescription.getText().toString().trim();

        textError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(name)) {
            showError(getString(R.string.admin_candidate_error_name_empty));
            return;
        }

        setLoading(true);

        List<MultipartBody.Part> parts = new ArrayList<>();
        parts.add(MultipartBody.Part.createFormData("category_id", String.valueOf(categoryId)));
        parts.add(MultipartBody.Part.createFormData("name", name));
        parts.add(MultipartBody.Part.createFormData("description", description));

        if (pickedPhotoUri != null) {
            try {
                File photoFile = copyUriToCacheFile(pickedPhotoUri);
                RequestBody photoBody = RequestBody.create(photoFile, MediaType.parse("image/*"));
                parts.add(MultipartBody.Part.createFormData("photo", photoFile.getName(), photoBody));
            } catch (IOException e) {
                setLoading(false);
                showError(getString(R.string.admin_loading_error));
                return;
            }
        }

        Call<ApiResponse<AdminCandidate>> call = candidateId == -1
                ? RetrofitClient.getApiService().adminCreateCandidate(parts)
                : RetrofitClient.getApiService().adminUpdateCandidate(candidateId, parts);

        call.enqueue(new Callback<ApiResponse<AdminCandidate>>() {
            @Override
            public void onResponse(Call<ApiResponse<AdminCandidate>> call, Response<ApiResponse<AdminCandidate>> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(AdminCandidateFormActivity.this, R.string.admin_candidate_saved, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    showError(ApiError.message(response, getString(R.string.admin_loading_error)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AdminCandidate>> call, Throwable t) {
                setLoading(false);
                showError(getString(R.string.error_no_connection));
            }
        });
    }

    private File copyUriToCacheFile(Uri uri) throws IOException {
        String mimeType = getContentResolver().getType(uri);
        String extension = mimeType != null && mimeType.contains("png") ? ".png" : ".jpg";
        File tempFile = File.createTempFile("candidate_photo", extension, getCacheDir());

        try (InputStream input = getContentResolver().openInputStream(uri);
             OutputStream output = new FileOutputStream(tempFile)) {
            if (input == null) {
                throw new IOException("Tidak bisa membuka file foto");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }

        return tempFile;
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

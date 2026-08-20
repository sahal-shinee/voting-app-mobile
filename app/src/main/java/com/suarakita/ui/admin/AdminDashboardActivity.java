package com.suarakita.ui.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.suarakita.R;
import com.suarakita.api.ApiError;
import com.suarakita.api.RetrofitClient;
import com.suarakita.data.SessionManager;
import com.suarakita.model.AdminCreateRequest;
import com.suarakita.model.AdminCreateResult;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.Category;
import com.suarakita.model.ImportResult;
import com.suarakita.model.Student;
import com.suarakita.model.StudentCreateRequest;
import com.suarakita.model.StudentCreateResult;
import com.suarakita.model.VotingResults;
import com.suarakita.ui.auth.ChangePasswordActivity;
import com.suarakita.ui.auth.LoginActivity;
import com.suarakita.ui.common.CategoryResultAdapter;
import com.suarakita.ui.common.CategoryResultPreview;

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

public class AdminDashboardActivity extends AppCompatActivity {

    private enum Tab { KATEGORI, HASIL, SISWA }

    private SessionManager session;
    private View bannerChangePassword;
    private Tab currentTab = Tab.KATEGORI;

    // ===== Tab: Kategori =====
    private View containerKategori;
    private RecyclerView recyclerCategories;
    private ProgressBar progressBarKategori;
    private TextView textEmptyKategori;
    private TextView textErrorKategori;
    private AdminCategoryAdapter categoryAdapter;

    // ===== Tab: Hasil =====
    private View containerHasil;
    private RecyclerView recyclerHasil;
    private ProgressBar progressBarHasil;
    private TextView textEmptyHasil;
    private TextView textErrorHasil;
    private CategoryResultAdapter hasilAdapter;

    // ===== Tab: Siswa =====
    private View containerSiswa;
    private RecyclerView recyclerStudents;
    private ProgressBar progressBarSiswa;
    private TextView textEmptySiswa;
    private TextView textErrorSiswa;
    private TextInputEditText inputSearchStudent;
    private AdminStudentAdapter studentAdapter;
    private List<Student> allStudents = new ArrayList<>();

    private final ActivityResultLauncher<String> csvPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importCsv(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        session = new SessionManager(this);

        TextView textWelcome = findViewById(R.id.textWelcome);
        textWelcome.setText(getString(R.string.greeting_hello, session.getName()));

        bannerChangePassword = findViewById(R.id.bannerChangePassword);
        bannerChangePassword.findViewById(R.id.buttonChangePassword)
                .setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));

        findViewById(R.id.buttonLogout).setOnClickListener(v -> logout());
        findViewById(R.id.buttonActivityLog).setOnClickListener(v ->
                startActivity(new Intent(this, AdminActivityLogActivity.class)));

        setupKategoriTab();
        setupHasilTab();
        setupSiswaTab();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_admin_kategori) {
                switchTab(Tab.KATEGORI);
            } else if (item.getItemId() == R.id.nav_admin_hasil) {
                switchTab(Tab.HASIL);
            } else {
                switchTab(Tab.SISWA);
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bannerChangePassword.setVisibility(session.mustChangePassword() ? View.VISIBLE : View.GONE);
        refreshCurrentTab();
    }

    private void switchTab(Tab tab) {
        currentTab = tab;
        containerKategori.setVisibility(tab == Tab.KATEGORI ? View.VISIBLE : View.GONE);
        containerHasil.setVisibility(tab == Tab.HASIL ? View.VISIBLE : View.GONE);
        containerSiswa.setVisibility(tab == Tab.SISWA ? View.VISIBLE : View.GONE);
        refreshCurrentTab();
    }

    private void refreshCurrentTab() {
        switch (currentTab) {
            case KATEGORI:
                loadCategories();
                break;
            case HASIL:
                loadHasilOverview();
                break;
            case SISWA:
                loadStudents();
                break;
        }
    }

    private void logout() {
        RetrofitClient.getApiService().logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                goToLogin();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                goToLogin();
            }
        });
    }

    private void goToLogin() {
        session.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // =========================================================================
    // Tab: Kategori
    // =========================================================================

    private void setupKategoriTab() {
        containerKategori = findViewById(R.id.containerKategori);
        recyclerCategories = findViewById(R.id.recyclerCategories);
        progressBarKategori = findViewById(R.id.progressBarKategori);
        textEmptyKategori = findViewById(R.id.textEmptyKategori);
        textErrorKategori = findViewById(R.id.textErrorKategori);

        categoryAdapter = new AdminCategoryAdapter(this::openCategoryDetail);
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategories.setAdapter(categoryAdapter);

        findViewById(R.id.buttonAddCategory).setOnClickListener(v ->
                startActivity(new Intent(this, AdminCategoryFormActivity.class)));
        findViewById(R.id.buttonViewTrash).setOnClickListener(v ->
                startActivity(new Intent(this, AdminCategoryTrashActivity.class)));
    }

    private void loadCategories() {
        showKategoriLoading();

        RetrofitClient.getApiService().adminGetCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Category> categories = response.body().getData();
                    categoryAdapter.submitList(categories);
                    if (categories.isEmpty()) {
                        showKategoriEmpty();
                    } else {
                        showKategoriContent();
                    }
                } else {
                    showKategoriError(ApiError.message(response, getString(R.string.admin_loading_error)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                showKategoriError(getString(R.string.error_no_connection));
            }
        });
    }

    private void openCategoryDetail(Category category) {
        Intent intent = new Intent(this, AdminCategoryDetailActivity.class);
        intent.putExtra(AdminCategoryDetailActivity.EXTRA_CATEGORY_ID, category.getId());
        intent.putExtra(AdminCategoryDetailActivity.EXTRA_CATEGORY_NAME, category.getName());
        startActivity(intent);
    }

    private void openResultsFor(Category category) {
        Intent intent = new Intent(this, AdminResultsActivity.class);
        intent.putExtra(AdminResultsActivity.EXTRA_CATEGORY_ID, category.getId());
        intent.putExtra(AdminResultsActivity.EXTRA_CATEGORY_NAME, category.getName());
        startActivity(intent);
    }

    private void showKategoriLoading() {
        progressBarKategori.setVisibility(View.VISIBLE);
        textEmptyKategori.setVisibility(View.GONE);
        textErrorKategori.setVisibility(View.GONE);
        recyclerCategories.setVisibility(View.GONE);
    }

    private void showKategoriContent() {
        progressBarKategori.setVisibility(View.GONE);
        textEmptyKategori.setVisibility(View.GONE);
        textErrorKategori.setVisibility(View.GONE);
        recyclerCategories.setVisibility(View.VISIBLE);
    }

    private void showKategoriEmpty() {
        progressBarKategori.setVisibility(View.GONE);
        textErrorKategori.setVisibility(View.GONE);
        recyclerCategories.setVisibility(View.GONE);
        textEmptyKategori.setVisibility(View.VISIBLE);
    }

    private void showKategoriError(String message) {
        progressBarKategori.setVisibility(View.GONE);
        textEmptyKategori.setVisibility(View.GONE);
        recyclerCategories.setVisibility(View.GONE);
        textErrorKategori.setText(message);
        textErrorKategori.setVisibility(View.VISIBLE);
    }

    // =========================================================================
    // Tab: Hasil
    // =========================================================================

    private void setupHasilTab() {
        containerHasil = findViewById(R.id.containerHasil);
        recyclerHasil = findViewById(R.id.recyclerHasil);
        progressBarHasil = findViewById(R.id.progressBarHasil);
        textEmptyHasil = findViewById(R.id.textEmptyHasil);
        textErrorHasil = findViewById(R.id.textErrorHasil);

        hasilAdapter = new CategoryResultAdapter(this::openResultsFor);
        recyclerHasil.setLayoutManager(new LinearLayoutManager(this));
        recyclerHasil.setAdapter(hasilAdapter);
    }

    private void loadHasilOverview() {
        progressBarHasil.setVisibility(View.VISIBLE);
        textEmptyHasil.setVisibility(View.GONE);
        textErrorHasil.setVisibility(View.GONE);
        recyclerHasil.setVisibility(View.GONE);

        RetrofitClient.getApiService().adminGetCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Category> categoryList = response.body().getData();
                    progressBarHasil.setVisibility(View.GONE);

                    if (categoryList.isEmpty()) {
                        textEmptyHasil.setVisibility(View.VISIBLE);
                        return;
                    }

                    recyclerHasil.setVisibility(View.VISIBLE);
                    loadHasilPreviews(categoryList);
                } else {
                    progressBarHasil.setVisibility(View.GONE);
                    textErrorHasil.setText(ApiError.message(response, getString(R.string.admin_loading_error)));
                    textErrorHasil.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                progressBarHasil.setVisibility(View.GONE);
                textErrorHasil.setText(getString(R.string.error_no_connection));
                textErrorHasil.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadHasilPreviews(List<Category> categoryList) {
        List<CategoryResultPreview> previews = new ArrayList<>();
        for (Category category : categoryList) {
            previews.add(new CategoryResultPreview(category));
        }
        hasilAdapter.submitList(previews);

        for (int i = 0; i < categoryList.size(); i++) {
            int position = i;
            RetrofitClient.getApiService().adminGetResults(categoryList.get(i).getId())
                    .enqueue(new Callback<ApiResponse<VotingResults>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<VotingResults>> call, Response<ApiResponse<VotingResults>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                                hasilAdapter.updateResults(position, response.body().getData());
                            } else {
                                hasilAdapter.markError(position);
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<VotingResults>> call, Throwable t) {
                            hasilAdapter.markError(position);
                        }
                    });
        }
    }

    // =========================================================================
    // Tab: Siswa
    // =========================================================================

    private void setupSiswaTab() {
        containerSiswa = findViewById(R.id.containerSiswa);
        recyclerStudents = findViewById(R.id.recyclerStudents);
        progressBarSiswa = findViewById(R.id.progressBarSiswa);
        textEmptySiswa = findViewById(R.id.textEmptySiswa);
        textErrorSiswa = findViewById(R.id.textErrorSiswa);
        inputSearchStudent = findViewById(R.id.inputSearchStudent);

        studentAdapter = new AdminStudentAdapter(new AdminStudentAdapter.Listener() {
            @Override
            public void onEdit(Student student) {
                showEditStudentDialog(student);
            }

            @Override
            public void onResetPassword(Student student) {
                confirmResetPassword(student);
            }

            @Override
            public void onDelete(Student student) {
                confirmDeleteStudent(student);
            }
        });
        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        recyclerStudents.setAdapter(studentAdapter);

        inputSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.buttonAddStudent).setOnClickListener(v -> showAddStudentDialog());
        findViewById(R.id.buttonImportCsv).setOnClickListener(v -> csvPicker.launch("text/*"));
        findViewById(R.id.buttonAddAdmin).setOnClickListener(v -> showAddAdminDialog());
    }

    private void loadStudents() {
        showSiswaLoading();

        RetrofitClient.getApiService().adminGetStudents().enqueue(new Callback<ApiResponse<List<Student>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Student>>> call, Response<ApiResponse<List<Student>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    allStudents = response.body().getData();
                    if (allStudents.isEmpty()) {
                        showSiswaEmpty();
                    } else {
                        showSiswaContent();
                    }
                    filterStudents(inputSearchStudent.getText() == null ? "" : inputSearchStudent.getText().toString());
                } else {
                    showSiswaError(ApiError.message(response, getString(R.string.admin_loading_error)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Student>>> call, Throwable t) {
                showSiswaError(getString(R.string.error_no_connection));
            }
        });
    }

    private void filterStudents(String query) {
        String needle = query.trim().toLowerCase();
        if (needle.isEmpty()) {
            studentAdapter.submitList(allStudents);
            return;
        }

        List<Student> filtered = new ArrayList<>();
        for (Student student : allStudents) {
            boolean matchesName = student.getName() != null && student.getName().toLowerCase().contains(needle);
            boolean matchesNis = student.getNis() != null && student.getNis().toLowerCase().contains(needle);
            if (matchesName || matchesNis) {
                filtered.add(student);
            }
        }
        studentAdapter.submitList(filtered);
    }

    private void showAddStudentDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null);
        TextInputEditText inputName = view.findViewById(R.id.inputName);
        TextInputEditText inputNis = view.findViewById(R.id.inputNis);

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_students_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.admin_students_dialog_save, (dialog, which) -> {
                    String name = inputName.getText() == null ? "" : inputName.getText().toString().trim();
                    String nis = inputNis.getText() == null ? "" : inputNis.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(nis)) {
                        Toast.makeText(this, R.string.admin_students_error_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createStudent(name, nis);
                })
                .setNegativeButton(R.string.admin_confirm_cancel, null)
                .show();
    }

    private void createStudent(String name, String nis) {
        RetrofitClient.getApiService().adminCreateStudent(new StudentCreateRequest(name, nis))
                .enqueue(new Callback<ApiResponse<StudentCreateResult>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<StudentCreateResult>> call, Response<ApiResponse<StudentCreateResult>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Toast.makeText(AdminDashboardActivity.this,
                                    getString(R.string.admin_students_created_message, response.body().getData().getInitialPassword()),
                                    Toast.LENGTH_LONG).show();
                            loadStudents();
                        } else {
                            Toast.makeText(AdminDashboardActivity.this,
                                    ApiError.message(response, getString(R.string.admin_loading_error)),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<StudentCreateResult>> call, Throwable t) {
                        Toast.makeText(AdminDashboardActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditStudentDialog(Student student) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null);
        TextInputEditText inputName = view.findViewById(R.id.inputName);
        TextInputEditText inputNis = view.findViewById(R.id.inputNis);
        inputName.setText(student.getName());
        inputNis.setText(student.getNis());

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_students_edit_title)
                .setView(view)
                .setPositiveButton(R.string.admin_students_dialog_save, (dialog, which) -> {
                    String name = inputName.getText() == null ? "" : inputName.getText().toString().trim();
                    String nis = inputNis.getText() == null ? "" : inputNis.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(nis)) {
                        Toast.makeText(this, R.string.admin_students_error_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateStudent(student.getId(), name, nis);
                })
                .setNegativeButton(R.string.admin_confirm_cancel, null)
                .show();
    }

    private void updateStudent(int id, String name, String nis) {
        RetrofitClient.getApiService().adminUpdateStudent(id, new StudentCreateRequest(name, nis))
                .enqueue(new Callback<ApiResponse<Student>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Student>> call, Response<ApiResponse<Student>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminDashboardActivity.this, R.string.admin_students_updated, Toast.LENGTH_SHORT).show();
                            loadStudents();
                        } else {
                            Toast.makeText(AdminDashboardActivity.this,
                                    ApiError.message(response, getString(R.string.admin_loading_error)),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Student>> call, Throwable t) {
                        Toast.makeText(AdminDashboardActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmDeleteStudent(Student student) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_confirm_delete_student_title)
                .setMessage(R.string.admin_confirm_delete_student_message)
                .setPositiveButton(R.string.admin_confirm_yes, (dialog, which) -> deleteStudent(student))
                .setNegativeButton(R.string.admin_confirm_cancel, null)
                .show();
    }

    private void deleteStudent(Student student) {
        RetrofitClient.getApiService().adminDeleteStudent(student.getId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminDashboardActivity.this, R.string.admin_students_deleted, Toast.LENGTH_SHORT).show();
                            loadStudents();
                        } else {
                            Toast.makeText(AdminDashboardActivity.this,
                                    ApiError.message(response, getString(R.string.admin_loading_error)),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(AdminDashboardActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddAdminDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_admin, null);
        TextInputEditText inputName = view.findViewById(R.id.inputName);
        TextInputEditText inputUsername = view.findViewById(R.id.inputUsername);

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_admins_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.admin_students_dialog_save, (dialog, which) -> {
                    String name = inputName.getText() == null ? "" : inputName.getText().toString().trim();
                    String username = inputUsername.getText() == null ? "" : inputUsername.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(username)) {
                        Toast.makeText(this, R.string.admin_admins_error_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createAdmin(name, username);
                })
                .setNegativeButton(R.string.admin_confirm_cancel, null)
                .show();
    }

    private void createAdmin(String name, String username) {
        RetrofitClient.getApiService().adminCreateAdmin(new AdminCreateRequest(name, username))
                .enqueue(new Callback<ApiResponse<AdminCreateResult>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AdminCreateResult>> call, Response<ApiResponse<AdminCreateResult>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Toast.makeText(AdminDashboardActivity.this,
                                    getString(R.string.admin_admins_created_message, response.body().getData().getInitialPassword()),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AdminDashboardActivity.this,
                                    ApiError.message(response, getString(R.string.admin_loading_error)),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AdminCreateResult>> call, Throwable t) {
                        Toast.makeText(AdminDashboardActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmResetPassword(Student student) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_students_reset_confirm_title)
                .setMessage(getString(R.string.admin_students_reset_confirm_message, student.getName()))
                .setPositiveButton(R.string.admin_confirm_yes, (dialog, which) -> resetPassword(student))
                .setNegativeButton(R.string.admin_confirm_cancel, null)
                .show();
    }

    private void resetPassword(Student student) {
        RetrofitClient.getApiService().adminResetPassword(student.getId())
                .enqueue(new Callback<ApiResponse<StudentCreateResult>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<StudentCreateResult>> call, Response<ApiResponse<StudentCreateResult>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Toast.makeText(AdminDashboardActivity.this,
                                    getString(R.string.admin_students_reset_message, response.body().getData().getInitialPassword()),
                                    Toast.LENGTH_LONG).show();
                            loadStudents();
                        } else {
                            Toast.makeText(AdminDashboardActivity.this,
                                    ApiError.message(response, getString(R.string.admin_loading_error)),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<StudentCreateResult>> call, Throwable t) {
                        Toast.makeText(AdminDashboardActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void importCsv(Uri uri) {
        File csvFile;
        try {
            csvFile = copyUriToCacheFile(uri);
        } catch (IOException e) {
            Toast.makeText(this, R.string.admin_loading_error, Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(csvFile, MediaType.parse("text/csv"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", csvFile.getName(), body);

        RetrofitClient.getApiService().adminImportStudents(filePart).enqueue(new Callback<ApiResponse<ImportResult>>() {
            @Override
            public void onResponse(Call<ApiResponse<ImportResult>> call, Response<ApiResponse<ImportResult>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    ImportResult result = response.body().getData();
                    Toast.makeText(AdminDashboardActivity.this,
                            getString(R.string.admin_students_import_result, result.getCreatedCount(), result.getSkippedCount()),
                            Toast.LENGTH_LONG).show();
                    loadStudents();
                } else {
                    Toast.makeText(AdminDashboardActivity.this,
                            ApiError.message(response, getString(R.string.admin_loading_error)),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ImportResult>> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File copyUriToCacheFile(Uri uri) throws IOException {
        File tempFile = File.createTempFile("students_import", ".csv", getCacheDir());

        try (InputStream input = getContentResolver().openInputStream(uri);
             OutputStream output = new FileOutputStream(tempFile)) {
            if (input == null) {
                throw new IOException("Tidak bisa membuka file CSV");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }

        return tempFile;
    }

    private void showSiswaLoading() {
        progressBarSiswa.setVisibility(View.VISIBLE);
        textEmptySiswa.setVisibility(View.GONE);
        textErrorSiswa.setVisibility(View.GONE);
        recyclerStudents.setVisibility(View.GONE);
    }

    private void showSiswaContent() {
        progressBarSiswa.setVisibility(View.GONE);
        textEmptySiswa.setVisibility(View.GONE);
        textErrorSiswa.setVisibility(View.GONE);
        recyclerStudents.setVisibility(View.VISIBLE);
    }

    private void showSiswaEmpty() {
        progressBarSiswa.setVisibility(View.GONE);
        textErrorSiswa.setVisibility(View.GONE);
        recyclerStudents.setVisibility(View.GONE);
        textEmptySiswa.setVisibility(View.VISIBLE);
    }

    private void showSiswaError(String message) {
        progressBarSiswa.setVisibility(View.GONE);
        textEmptySiswa.setVisibility(View.GONE);
        recyclerStudents.setVisibility(View.GONE);
        textErrorSiswa.setText(message);
        textErrorSiswa.setVisibility(View.VISIBLE);
    }
}

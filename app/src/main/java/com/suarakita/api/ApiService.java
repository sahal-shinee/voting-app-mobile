package com.suarakita.api;

import com.suarakita.model.ActivityLog;
import com.suarakita.model.AdminCandidate;
import com.suarakita.model.AdminCreateRequest;
import com.suarakita.model.AdminCreateResult;
import com.suarakita.model.ApiResponse;
import com.suarakita.model.Candidate;
import com.suarakita.model.Category;
import com.suarakita.model.CategoryRequest;
import com.suarakita.model.CategoryToggleRequest;
import com.suarakita.model.ChangePasswordRequest;
import com.suarakita.model.ImportResult;
import com.suarakita.model.LoginRequest;
import com.suarakita.model.LoginResult;
import com.suarakita.model.MyVote;
import com.suarakita.model.Student;
import com.suarakita.model.StudentCreateRequest;
import com.suarakita.model.StudentCreateResult;
import com.suarakita.model.VoteCreated;
import com.suarakita.model.VoteRequest;
import com.suarakita.model.VotingResults;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface ApiService {

    @POST("auth/login")
    Call<ApiResponse<LoginResult>> login(@Body LoginRequest request);

    @POST("auth/change-password")
    Call<ApiResponse<Void>> changePassword(@Body ChangePasswordRequest request);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout();

    @GET("categories")
    Call<ApiResponse<List<Category>>> getCategories();

    @GET("categories/{id}/candidates")
    Call<ApiResponse<List<Candidate>>> getCandidates(@Path("id") int categoryId);

    @POST("votes")
    Call<ApiResponse<VoteCreated>> createVote(@Body VoteRequest request);

    @GET("categories/{id}/results")
    Call<ApiResponse<VotingResults>> getResults(@Path("id") int categoryId);

    @GET("me/votes")
    Call<ApiResponse<List<MyVote>>> getMyVotes();

    // ---- Admin: categories ----

    @GET("admin/categories")
    Call<ApiResponse<List<Category>>> adminGetCategories();

    @POST("admin/categories")
    Call<ApiResponse<Category>> adminCreateCategory(@Body CategoryRequest request);

    @PUT("admin/categories/{id}")
    Call<ApiResponse<Category>> adminUpdateCategory(@Path("id") int id, @Body CategoryRequest request);

    @DELETE("admin/categories/{id}")
    Call<ApiResponse<Void>> adminDeleteCategory(@Path("id") int id);

    @PATCH("admin/categories/{id}/toggle")
    Call<ApiResponse<Category>> adminToggleCategory(@Path("id") int id, @Body CategoryToggleRequest request);

    @GET("admin/categories/{id}/results")
    Call<ApiResponse<VotingResults>> adminGetResults(@Path("id") int id);

    @GET("admin/categories/trash")
    Call<ApiResponse<List<Category>>> adminGetCategoriesTrash();

    @POST("admin/categories/{id}/restore")
    Call<ApiResponse<Void>> adminRestoreCategory(@Path("id") int id);

    @DELETE("admin/categories/{id}/permanent")
    Call<ApiResponse<Void>> adminPermanentDeleteCategory(@Path("id") int id);

    @GET("admin/categories/{id}/non-voters")
    Call<ApiResponse<List<Student>>> adminGetNonVoters(@Path("id") int id);

    @Streaming
    @GET("admin/categories/{id}/export")
    Call<ResponseBody> adminExportCategoryCsv(@Path("id") int id);

    // ---- Admin: candidates ----

    @GET("admin/candidates")
    Call<ApiResponse<List<AdminCandidate>>> adminGetCandidates(@Query("category_id") int categoryId);

    @Multipart
    @POST("admin/candidates")
    Call<ApiResponse<AdminCandidate>> adminCreateCandidate(@Part List<MultipartBody.Part> parts);

    @Multipart
    @POST("admin/candidates/{id}")
    Call<ApiResponse<AdminCandidate>> adminUpdateCandidate(@Path("id") int id, @Part List<MultipartBody.Part> parts);

    @DELETE("admin/candidates/{id}")
    Call<ApiResponse<Void>> adminDeleteCandidate(@Path("id") int id);

    // ---- Admin: students ----

    @GET("admin/students")
    Call<ApiResponse<List<Student>>> adminGetStudents();

    @POST("admin/students")
    Call<ApiResponse<StudentCreateResult>> adminCreateStudent(@Body StudentCreateRequest request);

    @PUT("admin/students/{id}")
    Call<ApiResponse<Student>> adminUpdateStudent(@Path("id") int id, @Body StudentCreateRequest request);

    @DELETE("admin/students/{id}")
    Call<ApiResponse<Void>> adminDeleteStudent(@Path("id") int id);

    @Multipart
    @POST("admin/students/import")
    Call<ApiResponse<ImportResult>> adminImportStudents(@Part MultipartBody.Part file);

    @POST("admin/students/{id}/reset-password")
    Call<ApiResponse<StudentCreateResult>> adminResetPassword(@Path("id") int id);

    // ---- Admin: akun admin ----

    @POST("admin/admins")
    Call<ApiResponse<AdminCreateResult>> adminCreateAdmin(@Body AdminCreateRequest request);

    // ---- Admin: log aktivitas ----

    @GET("admin/activity-logs")
    Call<ApiResponse<List<ActivityLog>>> adminGetActivityLogs();
}

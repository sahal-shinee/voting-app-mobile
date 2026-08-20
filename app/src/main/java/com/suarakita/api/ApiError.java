package com.suarakita.api;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.suarakita.model.ApiResponse;

import retrofit2.Response;

// Retrofit tidak otomatis men-deserialize errorBody() (response 4xx/5xx) -- helper ini
// menariknya jadi pesan ramah dari envelope {success,data,message} yang dikirim backend.
public class ApiError {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private ApiError() {
    }

    public static String message(Response<?> response, String fallback) {
        if (response.errorBody() == null) {
            return fallback;
        }

        try {
            String body = response.errorBody().string();
            ApiResponse<?> parsed = GSON.fromJson(body, ApiResponse.class);
            if (parsed != null && parsed.getMessage() != null && !parsed.getMessage().isEmpty()) {
                return parsed.getMessage();
            }
        } catch (Exception ignored) {
            // Body bukan JSON yang valid -- pakai fallback.
        }

        return fallback;
    }

    public static int statusCode(Response<?> response) {
        return response.code();
    }
}

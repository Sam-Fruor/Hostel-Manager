package com.example.propertymanager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.Serializable;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private long startTime;
    private final int MIN_SPLASH_TIME = 2000; // 2 Seconds
    private boolean isNavigating = false; // SAFETY FLAG

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Force Light Mode
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_splash);

        startTime = System.currentTimeMillis();

        fetchDataAndProceed();
    }

    private void fetchDataAndProceed() {
        SupabaseClient.getService().getTenants(
                SupabaseClient.getKey(),
                SupabaseClient.getAuth(),
                "eq.true"
        ).enqueue(new Callback<List<Tenant>>() {
            @Override
            public void onResponse(Call<List<Tenant>> call, Response<List<Tenant>> response) {
                if (isFinishing() || isDestroyed()) return; // Stop if app closed

                if (response.isSuccessful() && response.body() != null) {
                    proceedToMain(response.body());
                } else {
                    proceedToMain(null);
                }
            }

            @Override
            public void onFailure(Call<List<Tenant>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                proceedToMain(null);
            }
        });
    }

    private void proceedToMain(List<Tenant> fetchedData) {
        // 1. SAFETY CHECK: If we are already going to Main, stop here.
        if (isNavigating) return;
        isNavigating = true;

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        long remainingTime = MIN_SPLASH_TIME - elapsedTime;

        if (remainingTime < 0) remainingTime = 0;

        // Use Looper.getMainLooper() to avoid deprecated Handler warning
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check again just to be safe
            if (isFinishing() || isDestroyed()) return;

            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            if (fetchedData != null) {
                intent.putExtra("PRELOADED_DATA", (Serializable) fetchedData);
            }
            startActivity(intent);
            finish(); // Close Splash so you can't go back to it
        }, remainingTime);
    }
}
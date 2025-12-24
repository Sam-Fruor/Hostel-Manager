package com.example.propertymanager;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    // Ensure this URL is correct (must end with /)
    public static final String BASE_URL = "https://gsbpbgyykqlfjrtafzkq.supabase.co/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdzYnBiZ3l5a3FsZmpydGFmemtxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU5OTM5NzYsImV4cCI6MjA4MTU2OTk3Nn0.O24dI8qK5_LCrTxGVzv-qrw1TuB5KchapvwC4ueEa6U";

    private static Retrofit retrofit = null;

    // CHANGED: Returns 'SupabaseService' (the separate file)
    public static SupabaseService getService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        // CHANGED: Creates 'SupabaseService.class'
        return retrofit.create(SupabaseService.class);
    }

    // Helper to get headers easily
    public static String getKey() { return API_KEY; }
    public static String getAuth() { return "Bearer " + API_KEY; }

}
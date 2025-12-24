package com.example.propertymanager;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SupabaseService {

    // --- TENANT OPERATIONS ---

    @POST("rest/v1/tenants")
    Call<Void> addTenant(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Header("Prefer") String prefer,
            @Body Tenant tenant
    );

    @GET("rest/v1/tenants?select=*")
    Call<List<Tenant>> getTenants(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("is_active") String isActive
    );

    @PATCH("rest/v1/tenants")
    Call<Void> updateTenant(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String id,
            @Body Tenant tenant
    );

    @Multipart
    @POST("storage/v1/object/tenant_docs/{filename}")
    Call<ResponseBody> uploadImage(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Path("filename") String filename,
            @Part MultipartBody.Part image
    );

    // --- ROOM MASTER OPERATIONS ---

    // 1. THIS IS THE NEW METHOD THAT WAS MISSING
    @GET("rest/v1/room_master?select=*")
    Call<List<RoomMaster>> getRoomsFiltered(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("billing_month") String month,
            @Query("billing_year") String year
    );

    @GET("rest/v1/room_master?select=*")
    Call<List<RoomMaster>> getRooms(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("select") String select
    );

    @POST("rest/v1/room_master")
    @Headers("Prefer: resolution=merge-duplicates")
    Call<Void> upsertRoom(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("on_conflict") String conflictColumns,
            @Body RoomMaster room
    );

    // --- HISTORY OPERATIONS ---

    @GET("rest/v1/tenant_history?select=*")
    Call<List<Tenant>> getHistory(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token
    );

    @POST("rest/v1/tenant_history")
    Call<Void> saveToHistory(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Body Tenant tenant
    );

    @POST("rest/v1/payment_log")
    Call<Void> logPayment(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Body PaymentLog log
    );

    // 2. Fetch payment history (Reading data)
    // FIX: Added "rest/v1/", changed to @GET, and separated queries
    @GET("rest/v1/payment_log")
    Call<List<PaymentLog>> getPaymentLogs(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("select") String select,  // Pass "*"
            @Query("order") String order     // Pass "created_at.desc"
    );
}
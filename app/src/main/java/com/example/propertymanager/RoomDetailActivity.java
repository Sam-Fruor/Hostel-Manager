package com.example.propertymanager;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomDetailActivity extends AppCompatActivity {

    public static RoomData currentRoomData = null;

    private TextView tvRoomHeader, tvOccupancy;
    private RecyclerView recyclerView;
    private TenantAdapter adapter;
    private RoomData roomData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        tvRoomHeader = findViewById(R.id.tvRoomHeader);
        tvOccupancy = findViewById(R.id.tvOccupancy);
        recyclerView = findViewById(R.id.recyclerRoomTenants);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (currentRoomData != null) {
            roomData = currentRoomData;
            currentRoomData = null;
        }

        if (roomData != null) {
            tvRoomHeader.setText(roomData.getPropertyName() + " - " + roomData.getRoomNumber());
            int count = (roomData.getTenantList() != null) ? roomData.getTenantList().size() : 0;
            tvOccupancy.setText("Occupants: " + count);

            adapter = new TenantAdapter(this, roomData.getTenantList(), new TenantAdapter.OnTenantActionListener() {
                @Override
                public void onItemClick(Tenant tenant) {
                    if ("VACANT".equals(tenant.getName())) {
                        Intent intent = new Intent(RoomDetailActivity.this, AddTenantActivity.class);
                        intent.putExtra("PREFILL_ROOM", roomData.getRoomNumber());
                        intent.putExtra("PREFILL_HOSTEL", roomData.getPropertyName());
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent = new Intent(RoomDetailActivity.this, TenantDetailActivity.class);
                        intent.putExtra("TENANT_DATA", tenant);
                        startActivity(intent);
                    }
                }

                @Override
                public void onPayClick(Tenant tenant) {
                    // ERROR FIXED: Individual tenant payment is disabled.
                    // Payment is now handled at the Room level on the Dashboard.
                    Toast.makeText(RoomDetailActivity.this, "Please collect Rent from the Dashboard.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onLeaveClick(Tenant tenant) {
                    confirmTenantLeaving(tenant);
                }

                @Override
                public void onWhatsAppClick(Tenant tenant) {
                    openWhatsApp(tenant);
                }
            });
            recyclerView.setAdapter(adapter);
        } else {
            Toast.makeText(this, "Error: No Data Found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // --- HISTORY LOGIC ---
    private void confirmTenantLeaving(Tenant tenant) {
        new AlertDialog.Builder(this)
                .setTitle("Tenant Leaving?")
                .setMessage("Mark " + tenant.getName() + " as Left?\n\nYou will be asked to select the Move-Out Date.")
                .setPositiveButton("Select Date", (dialog, which) -> {
                    showMoveOutDatePicker(tenant);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMoveOutDatePicker(Tenant tenant) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.US, "%02d-%02d-%d", dayOfMonth, month1 + 1, year1);
                    markTenantAsInactive(tenant, selectedDate);
                },
                year, month, day);

        datePickerDialog.show();
    }

    private void markTenantAsInactive(Tenant tenant, String date) {
        // 1. Update the local object
        tenant.setActive(false);
        tenant.setMoveOutDate(date);

        // 2. Send the update to the existing 'tenants' table
        SupabaseClient.getService().updateTenant(
                SupabaseClient.getKey(),
                SupabaseClient.getAuth(),
                "eq." + tenant.getId(),
                tenant
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RoomDetailActivity.this, "Tenant moved to History", Toast.LENGTH_SHORT).show();

                    // 3. Remove from the current room's list and refresh UI
                    roomData.getTenantList().remove(tenant);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(RoomDetailActivity.this, "Update Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RoomDetailActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- WHATSAPP LOGIC (FIXED) ---
    private void openWhatsApp(Tenant tenant) {
        try {
            String phone = tenant.getPhone();
            if(phone == null) return;
            phone = phone.replace("+", "").replace(" ", "");
            if(phone.length() == 10) phone = "91" + phone;

            // ERROR FIXED: Use Room's Due, not Tenant's Due
            double totalRoomDue = 0;
            if (roomData != null) {
                totalRoomDue = roomData.getCurrentDue();
            }

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US);
            String dateStr = sdf.format(new java.util.Date());

            String msg = "Hello " + tenant.getName() +
                    ", Your Rent due is ₹" +" *"+(int)totalRoomDue + "*" +
                    " for Room No.: "+"*" + roomData.getRoomNumber() + "*" +
                    " Due Date: *" + dateStr + "*";
            String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + URLEncoder.encode(msg, "UTF-8");

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }
}
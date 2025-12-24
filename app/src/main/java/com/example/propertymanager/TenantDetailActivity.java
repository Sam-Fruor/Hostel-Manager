package com.example.propertymanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class TenantDetailActivity extends AppCompatActivity {

    private TextView tvName, tvPhone, tvParentPhone, tvCompany, tvRoom, tvDate, tvExitDate, tvVehicle;
    private ImageView imgFront, imgBack, imgForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_detail);

        initViews();

        final Tenant tenant = (Tenant) getIntent().getSerializableExtra("TENANT_DATA");

        if (tenant != null) {
            populateData(tenant);

            findViewById(R.id.btnEditTenant).setOnClickListener(v -> {
                Intent intent = new Intent(TenantDetailActivity.this, EditTenantActivity.class);
                intent.putExtra("TENANT_DATA", tenant);
                startActivity(intent);
            });

        } else {
            Toast.makeText(this, "Error loading tenant data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        tvName = findViewById(R.id.tvDetailName);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvParentPhone = findViewById(R.id.tvDetailParentPhone);
        tvCompany = findViewById(R.id.tvDetailCompany);
        tvRoom = findViewById(R.id.tvDetailRoom);
        tvDate = findViewById(R.id.tvDetailDate);
        tvExitDate = findViewById(R.id.tvDetailExitDate);
        tvVehicle = findViewById(R.id.tvDetailVehicle);
        imgFront = findViewById(R.id.imgDetailAadharFront);
        imgBack = findViewById(R.id.imgDetailAadharBack);
        imgForm = findViewById(R.id.imgDetailForm);
    }

    private void populateData(Tenant tenant) {
        tvName.setText(tenant.getName());
        tvPhone.setText("Phone: " + tenant.getPhone());
        tvParentPhone.setText("Parent: " + (tenant.getParentPhone() != null ? tenant.getParentPhone() : "N/A"));
        tvCompany.setText("Company: " + (tenant.getCompanyName() != null ? tenant.getCompanyName() : "N/A"));
        tvRoom.setText(tenant.getHostelName() + " - Room " + tenant.getRoomNumber());
        tvDate.setText("Moved In: " + tenant.getMoveInDate());

        tvPhone.setOnClickListener(v -> dialNumber(tenant.getPhone()));

        // --- NEW: CLICK TO DIAL (PARENT PHONE) ---
        // I added this too because it's usually very helpful!
        if (tenant.getParentPhone() != null && !tenant.getParentPhone().isEmpty()) {
            tvParentPhone.setOnClickListener(v -> dialNumber(tenant.getParentPhone()));
        }

        if (!tenant.isActive() && tenant.getMoveOutDate() != null) {
            tvExitDate.setVisibility(View.VISIBLE);
            tvExitDate.setText("Moved Out: " + tenant.getMoveOutDate());
        } else {
            tvExitDate.setVisibility(View.GONE);
        }

        if (tenant.getVehicleNumber() != null && !tenant.getVehicleNumber().isEmpty()) {
            tvVehicle.setText("Vehicle: " + tenant.getVehicleType() + " - " + tenant.getVehicleNumber());
        } else {
            tvVehicle.setText("No Vehicle");
        }

        // --- NEW CLICK LISTENERS FOR FULL SCREEN ---
        loadAndClick(tenant.getAadharFrontUrl(), imgFront);
        loadAndClick(tenant.getAadharBackUrl(), imgBack);
        loadAndClick(tenant.getFormPhotoUrl(), imgForm);
    }

    private void dialNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open dialer", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to load image and set click listener
    private void loadAndClick(String url, ImageView target) {
        if (url != null && !url.isEmpty()) {
            // Load Thumbnail
            Glide.with(this).load(url).placeholder(android.R.drawable.ic_menu_gallery).into(target);

            // Set Click Listener to open Full Screen
            target.setOnClickListener(v -> {
                Intent intent = new Intent(TenantDetailActivity.this, FullScreenImageActivity.class);
                intent.putExtra("IMAGE_URL", url);
                startActivity(intent);
            });
        }
    }
}
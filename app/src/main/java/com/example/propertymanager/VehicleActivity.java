package com.example.propertymanager;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VehicleActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ImageView btnBack;
    private VehicleAdapter adapter;
    private List<Tenant> vehicleList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle);

        recyclerView = findViewById(R.id.recyclerViewVehicles);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // --- UPDATED ADAPTER INITIALIZATION ---
        // We now pass the Click Listener inside the constructor
        adapter = new VehicleAdapter(vehicleList, new VehicleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Tenant tenant) {
                // Open Tenant Detail Activity
                Intent intent = new Intent(VehicleActivity.this, TenantDetailActivity.class);
                intent.putExtra("TENANT_DATA", tenant); // Pass the whole tenant object
                startActivity(intent);
            }
        });

        findViewById(R.id.btnExportVehiclePdf).setOnClickListener(v -> {
            if (vehicleList != null && !vehicleList.isEmpty()) {
                generateVehiclePDF(vehicleList);
            } else {
                Toast.makeText(this, "No vehicles to export", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        loadVehicles();
    }

    private void loadVehicles() {
        progressBar.setVisibility(View.VISIBLE);

        SupabaseClient.getService().getTenants(
                SupabaseClient.getKey(),
                SupabaseClient.getAuth(),
                "eq.true"
        ).enqueue(new Callback<List<Tenant>>() {
            @Override
            public void onResponse(Call<List<Tenant>> call, Response<List<Tenant>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    vehicleList.clear();

                    for (Tenant t : response.body()) {
                        if (t.isActive() && t.getVehicleNumber() != null && !t.getVehicleNumber().trim().isEmpty()) {
                            vehicleList.add(t);
                        }
                    }

                    if (vehicleList.isEmpty()) {
                        Toast.makeText(VehicleActivity.this, "No vehicles found", Toast.LENGTH_SHORT).show();
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Tenant>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(VehicleActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateVehiclePDF(List<Tenant> vehicleList) {
        if (vehicleList == null || vehicleList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. SORTING (Hostel -> Home -> Rizz)
        java.util.Collections.sort(vehicleList, (t1, t2) -> {
            int p1 = getReportPriority(t1.getHostelName());
            int p2 = getReportPriority(t2.getHostelName());
            if (p1 != p2) return Integer.compare(p1, p2);
            return compareRoomNumbers(t1.getRoomNumber(), t2.getRoomNumber());
        });

        // 2. PDF SETUP
        PdfDocument doc = new PdfDocument();
        int pageWidth = 842;
        int pageHeight = 595;
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pi);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint linePaint = new Paint();

        // --- FONT SETUP (Times New Roman Style) ---
        // Android's "SERIF" font is visually identical to Times New Roman
        android.graphics.Typeface timesNewRoman = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL);
        android.graphics.Typeface timesBold = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD);

        // Set default font
        paint.setTypeface(timesNewRoman);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1);
        linePaint.setColor(Color.BLACK);

        // Title (Bold Serif)
        paint.setTypeface(timesBold);
        paint.setTextSize(18);
        // Draw text in CAPS
        canvas.drawText("VEHICLE RECORD REGISTRY", 300, 40, paint);

        paint.setTypeface(timesNewRoman); // Reset to normal
        paint.setTextSize(10);
        String dateStr = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        canvas.drawText("GENERATED ON: " + dateStr, 20, 60, paint);

        // --- 3. COLUMN SETTINGS ---
        int y = 90;
        int margin = 20;
        int tableWidth = 800;

        // S.NO | NAME | ROOM | PHONE | PARENT | TYPE | NUMBER
        int[] cols = {20, 60, 200, 340, 450, 560, 660};
        int[] vLines = {55, 195, 335, 445, 555, 655, 822};

        // Header Background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#EEEEEE"));
        canvas.drawRect(margin, y - 15, margin + tableWidth, y + 5, bgPaint);

        // Header Text (Bold Serif & Caps)
        paint.setTypeface(timesBold);
        paint.setTextSize(11);

        canvas.drawText("S.NO", cols[0] + 2, y, paint);
        canvas.drawText("NAME", cols[1] + 2, y, paint);
        canvas.drawText("ROOM No.", cols[2] + 2, y, paint);
        canvas.drawText("PHONE NO.", cols[3] + 2, y, paint);
        canvas.drawText("PARENT NO.", cols[4] + 2, y, paint);
        canvas.drawText("TYPE", cols[5] + 2, y, paint);
        canvas.drawText("VEHICLE NO.", cols[6] + 2, y, paint);

        // Header Grid Lines
        canvas.drawLine(margin, y - 15, margin + tableWidth, y - 15, linePaint);
        canvas.drawLine(margin, y + 5, margin + tableWidth, y + 5, linePaint);
        canvas.drawLine(margin, y - 15, margin, y + 5, linePaint);
        for (int x : vLines) canvas.drawLine(x, y - 15, x, y + 5, linePaint);

        paint.setTypeface(timesNewRoman); // Reset to Normal for Data
        y += 20;

        // --- 4. DATA LOOP ---
        int serialNo = 0;
        for (Tenant t : vehicleList) {
            serialNo++;
            int rowHeight = 25;
            int textY = y + 15;

            // Draw Data (Using Helper for UPPERCASE)
            canvas.drawText(String.valueOf(serialNo), cols[0] + 2, textY, paint);
            canvas.drawText(truncate(t.getName(), 20), cols[1] + 2, textY, paint);
            canvas.drawText(val(t.getHostelName()) + " - " + val(t.getRoomNumber()), cols[2] + 2, textY, paint);
            canvas.drawText(val(t.getPhone()), cols[3] + 2, textY, paint);
            canvas.drawText(val(t.getParentPhone()), cols[4] + 2, textY, paint);
            canvas.drawText(val(t.getVehicleType()), cols[5] + 2, textY, paint);
            canvas.drawText(val(t.getVehicleNumber()), cols[6] + 2, textY, paint);

            // Reset Paint
            paint.setColor(Color.BLACK);
            paint.setTypeface(timesNewRoman);

            // Grid Lines
            canvas.drawLine(margin, y + rowHeight, margin + tableWidth, y + rowHeight, linePaint);
            canvas.drawLine(margin, y, margin, y + rowHeight, linePaint);
            for (int x : vLines) canvas.drawLine(x, y, x, y + rowHeight, linePaint);

            y += rowHeight;

            if (y > pageHeight - 50) {
                doc.finishPage(page);
                page = doc.startPage(pi);
                canvas = page.getCanvas();
                y = 50;
            }
        }

        doc.finishPage(page);

        File f = new File(getExternalCacheDir(), "Vehicle_Records.pdf");
        try {
            doc.writeTo(new FileOutputStream(f));
            sharePdfFile(f);
        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        doc.close();
    }

    // 1. Share Intent
    private void sharePdfFile(File f) {
        if (!f.exists()) return;
        Uri uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Share Report"));
    }

    // 2. Sorting Priority: Hostel(1) > Home(2) > Rizz(3)
    private int getReportPriority(String name) {
        if (name == null) return 99;
        String lower = name.trim().toLowerCase();
        if (lower.contains("hostel")) return 1;
        if (lower.contains("home")) return 2;
        if (lower.contains("rizz")) return 3;
        return 4;
    }

    // 3. Numeric Room Sorting
    private int compareRoomNumbers(String n1, String n2) {
        try {
            String num1Str = n1.replaceAll("\\D", "");
            String num2Str = n2.replaceAll("\\D", "");
            if (num1Str.isEmpty() || num2Str.isEmpty()) return n1.compareToIgnoreCase(n2);
            return Integer.compare(Integer.parseInt(num1Str), Integer.parseInt(num2Str));
        } catch (Exception e) { return n1.compareToIgnoreCase(n2); }
    }

    // 4. String Helpers
    // Helper: Returns "-" if empty, otherwise UPPERCASE string
    private String val(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "NA";
        }
        return text.toUpperCase(); // Forces Capital Letters
    }

    // Helper: Truncates and converts to UPPERCASE
    private String truncate(String text, int length) {
        if (text == null || text.trim().isEmpty()) {
            return "-";
        }
        String upper = text.toUpperCase(); // Convert first
        return (upper.length() > length) ? upper.substring(0, length - 2) + ".." : upper;
    }
}
package com.example.propertymanager;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
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

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TenantAdapter adapter;
    private TextView tvHeader;
    private ImageView btnExportPdf;
    private List<Tenant> masterHistoryList = new ArrayList<>();
    private List<Tenant> filteredList = new ArrayList<>();
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        tvHeader = findViewById(R.id.tvHistoryHeader);
        searchView = findViewById(R.id.searchView);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        recyclerView = findViewById(R.id.recyclerHistory);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TenantAdapter(this, filteredList, new TenantAdapter.OnTenantActionListener() {
            @Override
            public void onItemClick(Tenant tenant) {
                Intent intent = new Intent(HistoryActivity.this, TenantDetailActivity.class);
                intent.putExtra("TENANT_DATA", tenant);
                startActivity(intent);
            }
            @Override public void onPayClick(Tenant tenant) {}
            @Override public void onLeaveClick(Tenant tenant) {}
            @Override public void onWhatsAppClick(Tenant tenant) {}
        });

        recyclerView.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        btnExportPdf.setOnClickListener(v -> generatePDF());

        loadHistory();
    }

    private void filter(String text) {
        filteredList.clear();
        String query = text.toLowerCase().trim();
        for (Tenant tenant : masterHistoryList) {
            if (tenant.getName().toLowerCase().contains(query)) {
                filteredList.add(tenant);
            }
        }
        adapter.notifyDataSetChanged();
        updateCounter(filteredList.size());
    }

    private void updateCounter(int count) {
        if (tvHeader != null) {
            tvHeader.setText("Past Tenants Archive (" + count + ")");
        }
    }

    private void loadHistory() {
        SupabaseClient.getService().getTenants(SupabaseClient.getKey(), SupabaseClient.getAuth(), "eq.false")
                .enqueue(new Callback<List<Tenant>>() {
                    @Override
                    public void onResponse(Call<List<Tenant>> call, Response<List<Tenant>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            masterHistoryList.clear();
                            masterHistoryList.addAll(response.body());
                            filteredList.clear();
                            filteredList.addAll(masterHistoryList);
                            adapter.notifyDataSetChanged();
                            updateCounter(masterHistoryList.size());
                        }
                    }
                    @Override public void onFailure(Call<List<Tenant>> call, Throwable t) {
                        Toast.makeText(HistoryActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void generatePDF() {
        if (filteredList == null || filteredList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- 1. SORTING LOGIC ---
        // Sorts by Property (Hostel > Home > Rizz) then by Room Number
        java.util.Collections.sort(filteredList, (t1, t2) -> {
            int p1 = getReportPriority(t1.getHostelName());
            int p2 = getReportPriority(t2.getHostelName());
            if (p1 != p2) return Integer.compare(p1, p2);
            return compareRoomNumbers(t1.getRoomNumber(), t2.getRoomNumber());
        });

        // --- 2. PDF SETUP ---
        PdfDocument doc = new PdfDocument();

        // LANDSCAPE: Width = 842, Height = 595
        int pageWidth = 842;
        int pageHeight = 595;
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pi);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint linePaint = new Paint();

        // Setup Line Paint
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1);
        linePaint.setColor(Color.BLACK);

        // Title
        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        canvas.drawText("Tenant Archive / History Report", 280, 40, paint);

        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        String dateStr = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        canvas.drawText("Generated on: " + dateStr, 20, 60, paint);

        // Column Settings
        int y = 90;
        int margin = 20;
        int tableWidth = 800;

        // Columns: S.No, Name, Prop, Phone, Parent, Occ, Vehicle, In, Out
        int[] cols = {20, 55, 170, 285, 375, 465, 560, 640, 730};
        int[] vLines = {50, 165, 280, 370, 460, 555, 635, 725, 822};

        // Header Background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#EEEEEE"));
        canvas.drawRect(margin, y - 15, margin + tableWidth, y + 5, bgPaint);

        // Header Text
        paint.setFakeBoldText(true);
        paint.setTextSize(11);

        canvas.drawText("S.No", cols[0] + 2, y, paint);
        canvas.drawText("Name", cols[1] + 2, y, paint);
        canvas.drawText("Property - Room", cols[2] + 2, y, paint);
        canvas.drawText("Phone", cols[3] + 2, y, paint);
        canvas.drawText("Parent", cols[4] + 2, y, paint);
        canvas.drawText("Occupation", cols[5] + 2, y, paint);
        canvas.drawText("Vehicle", cols[6] + 2, y, paint);
        canvas.drawText("Moved In", cols[7] + 2, y, paint);
        canvas.drawText("Moved Out", cols[8] + 2, y, paint);

        // Header Grid Lines
        canvas.drawLine(margin, y - 15, margin + tableWidth, y - 15, linePaint);
        canvas.drawLine(margin, y + 5, margin + tableWidth, y + 5, linePaint);
        canvas.drawLine(margin, y - 15, margin, y + 5, linePaint);
        for (int x : vLines) canvas.drawLine(x, y - 15, x, y + 5, linePaint);

        paint.setFakeBoldText(false);
        y += 20;

        // --- 3. DRAW DATA LOOP ---
        int serialNo = 0;
        for (Tenant t : filteredList) {
            serialNo++;
            int rowHeight = 25;
            int textY = y + 15;

            // Standard Black Text
            paint.setColor(Color.BLACK);
            canvas.drawText(String.valueOf(serialNo), cols[0] + 2, textY, paint);
            canvas.drawText(truncate(t.getName(), 16), cols[1] + 2, textY, paint);
            canvas.drawText(val(t.getHostelName()) + " - " + val(t.getRoomNumber()), cols[2] + 2, textY, paint);
            canvas.drawText(val(t.getPhone()), cols[3] + 2, textY, paint);
            canvas.drawText(val(t.getParentPhone()), cols[4] + 2, textY, paint);
            canvas.drawText(truncate(t.getCompanyName(), 14), cols[5] + 2, textY, paint);
            canvas.drawText(val(t.getVehicleNumber()), cols[6] + 2, textY, paint);

            // COLOR LOGIC: "In Date" -> GREEN
            paint.setColor(Color.parseColor("#008000")); // Dark Green
            canvas.drawText(val(t.getMoveInDate()), cols[7] + 2, textY, paint);

            // COLOR LOGIC: "Out Date" -> RED
            paint.setColor(Color.RED);
            canvas.drawText(val(t.getMoveOutDate()), cols[8] + 2, textY, paint);

            // Reset Paint for Grid Lines
            paint.setColor(Color.BLACK);

            // Draw Row Grid Lines
            canvas.drawLine(margin, y + rowHeight, margin + tableWidth, y + rowHeight, linePaint);
            canvas.drawLine(margin, y, margin, y + rowHeight, linePaint);
            for (int x : vLines) canvas.drawLine(x, y, x, y + rowHeight, linePaint);

            y += rowHeight;

            // Page Break
            if (y > pageHeight - 50) {
                doc.finishPage(page);
                page = doc.startPage(pi);
                canvas = page.getCanvas();
                y = 50;
            }
        }

        doc.finishPage(page);

        File f = new File(getExternalCacheDir(), "Tenant_History_Archive.pdf");
        try {
            doc.writeTo(new FileOutputStream(f));
            sharePdfFile(f);
        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        doc.close();
    }

    // Helper to define sort order: Hostel(1) > Home(2) > Rizz(3)
    private int getReportPriority(String name) {
        if (name == null) return 99;
        String lower = name.trim().toLowerCase();
        if (lower.contains("hostel")) return 1;
        if (lower.contains("home")) return 2;
        if (lower.contains("rizz")) return 3;
        return 4;
    }

    // Helper to sort room numbers numerically (1, 2, 10 instead of 1, 10, 2)
    private int compareRoomNumbers(String n1, String n2) {
        try {
            String num1Str = n1.replaceAll("\\D", "");
            String num2Str = n2.replaceAll("\\D", "");
            if (num1Str.isEmpty() || num2Str.isEmpty()) return n1.compareToIgnoreCase(n2);
            int num1 = Integer.parseInt(num1Str);
            int num2 = Integer.parseInt(num2Str);
            return Integer.compare(num1, num2);
        } catch (Exception e) {
            return n1.compareToIgnoreCase(n2);
        }
    }

    // Basic String Helpers
    private String val(String text) {
        if (text == null || text.trim().isEmpty()) return "-";
        return text;
    }
    private String truncate(String text, int length) {
        if (text == null || text.trim().isEmpty()) return "-";
        return (text.length() > length) ? text.substring(0, length - 2) + ".." : text;
    }

    private void sharePdfFile(File f) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
        Intent i = new Intent(Intent.ACTION_SEND); i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_STREAM, uri); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Share"));
    }
}
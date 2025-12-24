package com.example.propertymanager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PaymentHistoryAdapter adapter;
    private List<PaymentLog> historyList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Button btnStatementPdf; // New Button

    // Variables for Date Filtering
    private String selectedFromDate = "";
    private String selectedToDate = "";
    private java.util.Calendar calendar = java.util.Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        // Link Views
        recyclerView = findViewById(R.id.recyclerViewHistory);
        progressBar = findViewById(R.id.progressBarHistory);
        tvEmpty = findViewById(R.id.tvEmptyHistory);
        btnStatementPdf = findViewById(R.id.btnStatementPdf); // Link the PDF button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaymentHistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);

        // Load Data
        fetchPaymentLogs();

        // PDF Button Listener
        btnStatementPdf.setOnClickListener(v -> {
            // Open the popup to select dates
            showDateRangeDialog();
        });
    }

    // --- NEW: DATE RANGE DIALOG ---
    private void showDateRangeDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_date_range, null);
        builder.setView(view);

        TextView tvFrom = view.findViewById(R.id.tvFromDate);
        TextView tvTo = view.findViewById(R.id.tvToDate);
        Button btnGenerate = view.findViewById(R.id.btnGenerateRange);

        // --- NEW: Find the Preset Buttons ---
        Button btnThisMonth = view.findViewById(R.id.btnThisMonth);
        Button btnLastMonth = view.findViewById(R.id.btnLastMonth);
        Button btnLast6 = view.findViewById(R.id.btnLast6Months);

        android.app.AlertDialog dialog = builder.create();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);

        // --- PRESET BUTTON LOGIC ---

        // 1. THIS MONTH (1st to Today)
        btnThisMonth.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            // End Date = Today
            selectedToDate = sdf.format(c.getTime());

            // Start Date = 1st of current month
            c.set(java.util.Calendar.DAY_OF_MONTH, 1);
            selectedFromDate = sdf.format(c.getTime());

            tvFrom.setText(selectedFromDate);
            tvTo.setText(selectedToDate);
        });

        // 2. LAST MONTH (1st to Last Day of Prev Month)
        btnLastMonth.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            // Set to 1st of current month
            c.set(java.util.Calendar.DAY_OF_MONTH, 1);
            // Subtract 1 day -> gets Last Day of Previous Month
            c.add(java.util.Calendar.DATE, -1);
            selectedToDate = sdf.format(c.getTime());

            // Set to 1st of that month
            c.set(java.util.Calendar.DAY_OF_MONTH, 1);
            selectedFromDate = sdf.format(c.getTime());

            tvFrom.setText(selectedFromDate);
            tvTo.setText(selectedToDate);
        });

        // 3. LAST 6 MONTHS (Today-6M to Today)
        btnLast6.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            // End Date = Today
            selectedToDate = sdf.format(c.getTime());

            // Start Date = Today minus 6 months
            c.add(java.util.Calendar.MONTH, -6);
            selectedFromDate = sdf.format(c.getTime());

            tvFrom.setText(selectedFromDate);
            tvTo.setText(selectedToDate);
        });

        // --- EXISTING DATE PICKERS ---

        // FROM Date Picker
        tvFrom.setOnClickListener(v -> {
            new android.app.DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                selectedFromDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                tvFrom.setText(selectedFromDate);
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        // TO Date Picker
        tvTo.setOnClickListener(v -> {
            new android.app.DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                selectedToDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                tvTo.setText(selectedToDate);
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        // Generate Button Click
        btnGenerate.setOnClickListener(v -> {
            if (selectedFromDate.isEmpty() || selectedToDate.isEmpty()) {
                Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show();
                return;
            }

            filterAndGeneratePdf(selectedFromDate, selectedToDate);
            dialog.dismiss();
        });

        dialog.show();
    }

    // --- NEW: FILTER LOGIC & PRINT ---
    private void filterAndGeneratePdf(String start, String end) {
        List<PaymentLog> filteredList = new ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);

        try {
            java.util.Date startDate = sdf.parse(start);
            java.util.Date endDate = sdf.parse(end);

            // Adjust End Date to include the full day
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(endDate);
            c.add(java.util.Calendar.DATE, 1);
            endDate = c.getTime();

            for (PaymentLog log : historyList) {
                // Parse the log date (e.g. 2025-12-21T15:30:00)
                if (log.getCreatedAt() != null && log.getCreatedAt().length() >= 10) {
                    String logDateStr = log.getCreatedAt().substring(0, 10);
                    java.util.Date logDate = sdf.parse(logDateStr);

                    if (logDate != null && !logDate.before(startDate) && logDate.before(endDate)) {
                        filteredList.add(log);
                    }
                }
            }

            if (filteredList.isEmpty()) {
                Toast.makeText(this, "No records found in this range", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();
                // Call the Generator
                StatementPdfGenerator.generateStatement(this, filteredList, start, end);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Date Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchPaymentLogs() {
        progressBar.setVisibility(View.VISIBLE);

        SupabaseClient.getService().getPaymentLogs(
                SupabaseClient.getKey(),
                SupabaseClient.getAuth(),
                "*",
                "created_at.desc"
        ).enqueue(new Callback<List<PaymentLog>>() {
            @Override
            public void onResponse(Call<List<PaymentLog>> call, Response<List<PaymentLog>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    historyList.clear();
                    historyList.addAll(response.body());

                    if (historyList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(PaymentHistoryActivity.this, "Error fetching logs", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<PaymentLog>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PaymentHistoryActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
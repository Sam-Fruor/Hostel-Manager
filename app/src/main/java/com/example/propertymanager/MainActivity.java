package com.example.propertymanager;

import java.util.Collections;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements RoomAdapter.OnRoomActionListener {

    private RecyclerView recyclerView;
    private RoomAdapter roomAdapter;
    private List<RoomData> roomList = new ArrayList<>();
    private List<Tenant> masterList = new ArrayList<>();
    private Map<String, RoomMaster> roomMasterMap = new HashMap<>();
    private Button btnPdfReport;
    private TextView tvPropertiesCount, tvTenantsCount, tvTotalCollection, tvTotalPending;
    private SearchView searchView;
    private Spinner spinnerFilter, spinnerMonth, spinnerYear;

    private String selectedMonth, selectedYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- FORCE LIGHT MODE ---
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);

        tvPropertiesCount = findViewById(R.id.tvPropertiesCount);
        tvTenantsCount = findViewById(R.id.tvTenantsCount);
        tvTotalCollection = findViewById(R.id.tvTotalCollection);
        tvTotalPending = findViewById(R.id.tvPendingCollection);
        btnPdfReport = findViewById(R.id.btnPdfReport);
        setupMonthYearSelectors();
        setupButtons();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        roomAdapter = new RoomAdapter(this, roomList, this);
        recyclerView.setAdapter(roomAdapter);

        setupFilters();

        // --- CHECK FOR PRELOADED DATA FROM SPLASH SCREEN ---
        // This is the new logic to utilize data fetched during the splash screen
        List<Tenant> preloaded = (List<Tenant>) getIntent().getSerializableExtra("PRELOADED_DATA");

        if (preloaded != null && !preloaded.isEmpty()) {
            // Use the data immediately without network call
            updateDashboardWithTenants(preloaded);
        } else {
            // No data passed, load normally from network
            // Note: We remove loadDashboardData() from onResume to avoid double loading
            // on the first launch, but keep it for returning from other activities.
        }
    }

    private void setupMonthYearSelectors() {
        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerYear = findViewById(R.id.spinnerYear);

        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        String[] years = {"2024", "2025", "2026", "2027", "2028"};

        spinnerMonth.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months));
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years));

        Calendar c = Calendar.getInstance();
        spinnerMonth.setSelection(c.get(Calendar.MONTH));
        spinnerYear.setSelection(1); // Default to 2025

        selectedMonth = months[c.get(Calendar.MONTH)];
        selectedYear = "2025";

        AdapterView.OnItemSelectedListener monthYearListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = spinnerMonth.getSelectedItem().toString();
                selectedYear = spinnerYear.getSelectedItem().toString();
                loadDashboardData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerMonth.setOnItemSelectedListener(monthYearListener);
        spinnerYear.setOnItemSelectedListener(monthYearListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we already have data (e.g. from onCreate preloaded) to avoid redundant call
        if (masterList.isEmpty()) {
            loadDashboardData();
        }
    }

    private void setupFilters() {
        spinnerFilter = findViewById(R.id.spinnerFilterProperty);
        searchView = findViewById(R.id.searchView);

        // List of your property categories
        String[] filters = {"All Properties", "Hostel", "Home", "Rizz", "Vacant Rooms"};
        spinnerFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters));

        // Listener for the Spinner (Category Filter)
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Apply both search text and the category selected
                applyFilters(searchView.getQuery().toString(), filters[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Listener for the Search Bar (Text Filter)
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override public boolean onQueryTextChange(String newText) {
                String selected = spinnerFilter.getSelectedItem() != null ?
                        spinnerFilter.getSelectedItem().toString() : "All Properties";

                // This now triggers your updated search logic (Room No + Tenant Name)
                applyFilters(newText, selected);
                return true;
            }
        });
    }

    private void setupButtons() {
        findViewById(R.id.btnForceRefresh).setOnClickListener(v -> {
            // 1. Show a message so you know it's working
            Toast.makeText(MainActivity.this, "Refreshing Dashboard...", Toast.LENGTH_SHORT).show();

            // 2. Clear the list strictly to avoid duplicates during reload
            if (roomList != null) roomList.clear();
            if (roomAdapter != null) roomAdapter.notifyDataSetChanged();

            // 3. Call the function that fetches data from Supabase
            loadDashboardData();
        });
        findViewById(R.id.btnAddTenant).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddTenantActivity.class)));
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, HistoryActivity.class)));

        Button btnTenantReport = findViewById(R.id.btnTenantReport); // Make sure ID exists in XML
        if (btnTenantReport != null) {
            btnTenantReport.setOnClickListener(v -> {
                if (masterList == null || masterList.isEmpty()) {
                    Toast.makeText(this, "No active tenants found", Toast.LENGTH_SHORT).show();
                } else {
                    generateActiveTenantReport(masterList);
                }
            });
        }

        btnPdfReport.setOnClickListener(v -> {
            if (roomList == null || roomList.isEmpty()) {
                Toast.makeText(this, "No data to print", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedProp = spinnerFilter.getSelectedItem().toString();
            // This calls the multi-page generator we created
            PdfGenerator.generateReport(this, roomList, selectedMonth, selectedYear, selectedProp);
        });

        btnPdfReport.setOnLongClickListener(v -> {
            // Vibrate to give feedback (optional but nice)
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) vibrator.vibrate(50); // Vibrate for 50ms

            // Show the Export Dialog
            showExportDialog();

            return true; // Return 'true' to indicate the event is consumed
        });

        findViewById(R.id.btnPaymentHistory).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PaymentHistoryActivity.class))
        );

        findViewById(R.id.btnSyncDues).setOnClickListener(v -> showSyncConfirmation());

        Button btnVehicleRecord = findViewById(R.id.btnVehicleRecord);
        if (btnVehicleRecord != null) {
            btnVehicleRecord.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, VehicleActivity.class)));
        }
    }

    private void showExportDialog() {
        String[] options = {"Export Tenant List (Excel/CSV)", "Export Payment History (Excel/CSV)"};

        new android.app.AlertDialog.Builder(this)
                .setTitle("Backup Data to Excel")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // OPTION 1: Export Tenants
                        if (masterList != null && !masterList.isEmpty()) {
                            // PASS MONTH AND YEAR HERE
                            CsvExporter.exportTenants(this, masterList, selectedMonth, selectedYear);
                        } else {
                            Toast.makeText(this, "No tenants to export", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Export Payments
                        fetchAllPaymentsForExport();
                    }
                })
                .show();
    }

    private void fetchAllPaymentsForExport() {
        Toast.makeText(this, "Fetching all data...", Toast.LENGTH_SHORT).show();
        SupabaseClient.getService().getPaymentLogs(
                SupabaseClient.getKey(),
                SupabaseClient.getAuth(),
                "*",
                "created_at.desc"
        ).enqueue(new retrofit2.Callback<List<PaymentLog>>() {
            @Override
            public void onResponse(retrofit2.Call<List<PaymentLog>> call, retrofit2.Response<List<PaymentLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CsvExporter.exportPayments(MainActivity.this, response.body());
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<PaymentLog>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- NEW HELPER METHOD: Process Tenant List and Update UI ---
    private void updateDashboardWithTenants(List<Tenant> tenants) {
        masterList.clear();
        masterList.addAll(tenants);

        // After updating masterList, we need to fetch RoomMaster (financials)
        // and then build the final UI list.
        fetchRoomsAndBuildUI();
    }

    private void loadDashboardData() {
        SupabaseClient.getService().getTenants(SupabaseClient.getKey(), SupabaseClient.getAuth(), "eq.true")
                .enqueue(new Callback<List<Tenant>>() {
                    @Override public void onResponse(Call<List<Tenant>> call, Response<List<Tenant>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // CALL THE NEW METHOD
                            updateDashboardWithTenants(response.body());
                        }
                    }
                    @Override public void onFailure(Call<List<Tenant>> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchRoomsAndBuildUI() {
        SupabaseClient.getService().getRoomsFiltered(
                SupabaseClient.getKey(),
                SupabaseClient.getAuth(),
                "eq." + selectedMonth,
                "eq." + selectedYear
        ).enqueue(new Callback<List<RoomMaster>>() {
            @Override
            public void onResponse(Call<List<RoomMaster>> call, Response<List<RoomMaster>> response) {
                roomMasterMap.clear();

                if (response.isSuccessful() && response.body() != null) {
                    List<RoomMaster> rooms = response.body();

                    if (rooms.isEmpty()) {
                        handleRolloverRequest();
                    } else {
                        for(RoomMaster r : rooms) {
                            String key = (r.getHostelName().trim() + "-" + r.getRoomNumber().trim()).toUpperCase();
                            roomMasterMap.put(key, r);
                        }
                    }
                }

                // Always update UI
                calculateDashboardStats();
                applyFilters(searchView.getQuery().toString(), spinnerFilter.getSelectedItem().toString());
            }

            @Override public void onFailure(Call<List<RoomMaster>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                calculateDashboardStats();
                applyFilters(searchView.getQuery().toString(), spinnerFilter.getSelectedItem().toString());
            }
        });
    }

    private void applyFilters(String query, String propertyFilter) {
        roomList.clear();
        Map<String, RoomData> groups = new HashMap<>();

        // This Set will track which rooms are occupied IN THE SELECTED MONTH
        Set<String> occupiedKeysInSelectedMonth = new HashSet<>();

        // 1. Prepare Report Date (Last day of selected month)
        Date reportDate = getReportDate(selectedMonth, selectedYear);

        // 2. ADD OCCUPIED ROOMS (With Time Travel Check)
        if (!propertyFilter.equals("Vacant Rooms")) {
            for (Tenant t : masterList) {
                if (propertyFilter.equals("All Properties") || t.getHostelName().equalsIgnoreCase(propertyFilter)) {

                    // --- TIME TRAVEL CHECK ---
                    if (!isTenantValidForMonth(t, reportDate)) {
                        continue;
                    }

                    String key = (t.getHostelName().trim() + "-" + t.getRoomNumber().trim()).toUpperCase();
                    if (!groups.containsKey(key)) groups.put(key, new RoomData(t.getHostelName(), t.getRoomNumber()));
                    groups.get(key).addTenant(t);

                    occupiedKeysInSelectedMonth.add(key);
                }
            }
        }

        // 3. ADD VACANT ROOMS
        if (propertyFilter.equals("All Properties") || propertyFilter.equals("Vacant Rooms")) {
            for (Tenant v : generateVacantList(occupiedKeysInSelectedMonth)) {

                boolean matches = propertyFilter.equals("All Properties")
                        || propertyFilter.equals("Vacant Rooms")
                        || v.getHostelName().equalsIgnoreCase(propertyFilter);

                if (matches) {
                    String key = (v.getHostelName().trim() + "-" + v.getRoomNumber().trim()).toUpperCase();
                    if (!groups.containsKey(key)) {
                        groups.put(key, new RoomData(v.getHostelName(), v.getRoomNumber()));
                        groups.get(key).addTenant(v);
                    }
                }
            }
        }

        // 4. MERGE FINANCIAL DATA & APPLY SEARCH (Updated for Tenant Name Search)
        for (RoomData rd : groups.values()) {
            String key = (rd.getPropertyName().trim() + "-" + rd.getRoomNumber().trim()).toUpperCase();
            if (roomMasterMap.containsKey(key)) {
                rd.setStandardRent(roomMasterMap.get(key).getRentAmount());
                rd.setCurrentDue(roomMasterMap.get(key).getDueAmount());
                rd.setExpectedAmount(roomMasterMap.get(key).getExpectedAmount());
            } else {
                rd.setStandardRent(0); rd.setCurrentDue(0);
            }

            String dateDisplay = "-";
            if (!rd.isVacant() && rd.getTenantList() != null) {
                for (Tenant t : rd.getTenantList()) {
                    if (t.getMoveInDate() != null && t.getMoveInDate().length() >= 2) {
                        try {
                            // Extract just the day (e.g., "15" from "15-01-2024")
                            String day = t.getMoveInDate().substring(0, 2);
                            dateDisplay = day + "th";
                            break; // Use the first found tenant's date
                        } catch (Exception e) {}
                    }
                }
            }
            rd.setDueDate(dateDisplay);

            // --- SEARCH LOGIC (Checks both Room No and Tenant Names) ---
            String lowerQuery = query.toLowerCase().trim();

            // Match 1: Room Number contains query
            boolean roomMatch = rd.getRoomNumber().toLowerCase().contains(lowerQuery);

            // Match 2: Any Tenant in this room matches query
            boolean tenantMatch = false;
            if (rd.getTenantList() != null) {
                for (Tenant t : rd.getTenantList()) {
                    if (t.getName().toLowerCase().contains(lowerQuery)) {
                        tenantMatch = true;
                        break;
                    }
                }
            }

            // Final condition to add to list
            if (lowerQuery.isEmpty() || roomMatch || tenantMatch) {
                if (!propertyFilter.equals("Vacant Rooms") || rd.isVacant()) {
                    roomList.add(rd);
                }
            }
        }

        // 5. SORTING
        java.util.Collections.sort(roomList, (r1, r2) -> {
            int p1 = getPropertyPriority(r1.getPropertyName());
            int p2 = getPropertyPriority(r2.getPropertyName());
            if (p1 != p2) return Integer.compare(p1, p2);
            return compareRoomNumbers(r1.getRoomNumber(), r2.getRoomNumber());
        });

        roomAdapter.notifyDataSetChanged();
    }

    // 1. Updated onCollectRoomRent to capture Radio Button selection
    @Override
    public void onCollectRoomRent(RoomData room) {
        if (room.isVacant()) {
            Toast.makeText(this, "Cannot collect rent: Room is Vacant!", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment, null);
        builder.setView(dialogView);

        android.widget.EditText etAmount = dialogView.findViewById(R.id.etAmount);
        android.widget.RadioGroup rgMode = dialogView.findViewById(R.id.rgPaymentMode); // Find RadioGroup

        // Pre-fill amount
        etAmount.setText(String.valueOf((int)room.getCurrentDue()));
        etAmount.setSelectAllOnFocus(true);

        builder.setPositiveButton("Collect", (dialog, which) -> {
            String value = etAmount.getText().toString().trim();
            if (!value.isEmpty()) {
                try {
                    double paid = Double.parseDouble(value);

                    // --- CHECK SELECTED MODE ---
                    String mode = "Cash"; // Default
                    int selectedId = rgMode.getCheckedRadioButtonId();
                    if (selectedId == R.id.rbUPI) {
                        mode = "UPI";
                    }

                    // Pass both amount and mode to processing
                    processRoomPayment(room, paid, mode);

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Create and show safely
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_white);
        }
        dialog.show();
    }

    // 2. Updated processRoomPayment to accept 'mode'
    private void processRoomPayment(RoomData room, double amountPaid, String mode) {
        double newDue = Math.max(0, room.getCurrentDue() - amountPaid);

        double currentExpected = 0;
        String key = (room.getPropertyName().trim() + "-" + room.getRoomNumber().trim()).toUpperCase();
        if (roomMasterMap.containsKey(key)) {
            currentExpected = roomMasterMap.get(key).getExpectedAmount();
        }
        if (currentExpected == 0) currentExpected = room.getStandardRent();

        RoomMaster updateObj = new RoomMaster(
                room.getPropertyName(),
                room.getRoomNumber(),
                room.getStandardRent(),
                newDue,
                currentExpected,
                selectedMonth,
                selectedYear
        );

        SupabaseClient.getService().upsertRoom(
                SupabaseClient.getKey(), SupabaseClient.getAuth(),
                "hostel_name,room_number,billing_month,billing_year", updateObj
        ).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if(response.isSuccessful()) {
                    // Pass mode to history
                    savePaymentHistory(room, amountPaid, mode);
                    Toast.makeText(MainActivity.this, "Payment Recorded (" + mode + ")!", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                } else {
                    Toast.makeText(MainActivity.this, "Error updating balance: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 3. Updated savePaymentHistory to accept 'mode'
    private void savePaymentHistory(RoomData room, double amount, String mode) {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());

        PaymentLog log = new PaymentLog(
                room.getPropertyName(),
                room.getRoomNumber(),
                amount,
                date,
                selectedMonth,
                selectedYear,
                mode // <--- Storing the mode (Cash/UPI)
        );

        SupabaseClient.getService().logPayment(SupabaseClient.getKey(), SupabaseClient.getAuth(), log)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        if (!response.isSuccessful()) {
                            android.util.Log.e("PAY_LOG", "Failed to log history: " + response.code());
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) { }
                });
    }

    @Override
    public void onUpdateRent(RoomData room) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Rent (" + selectedMonth + ")");
        builder.setMessage("Monthly rent for Room " + room.getRoomNumber());

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Set", (dialog, which) -> {
            String value = input.getText().toString();
            if (!value.isEmpty()) {
                double newRent = Double.parseDouble(value);
                RoomMaster master = new RoomMaster(
                        room.getPropertyName(),
                        room.getRoomNumber(),
                        newRent,
                        newRent, // Reset Due to Rent (assumption)
                        newRent, // Target
                        selectedMonth,
                        selectedYear
                );

                SupabaseClient.getService().upsertRoom(
                        SupabaseClient.getKey(),
                        SupabaseClient.getAuth(),
                        "hostel_name,room_number,billing_month,billing_year",
                        master
                ).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        if(response.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                            loadDashboardData();
                        } else {
                            Toast.makeText(MainActivity.this, "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onTenantMoveOut(RoomData room) {
        Tenant tenantToMove = null;
        for (Tenant t : masterList) {
            if (t.isActive() && t.getRoomNumber().equalsIgnoreCase(room.getRoomNumber())
                    && t.getHostelName().equalsIgnoreCase(room.getPropertyName())) {
                tenantToMove = t;
                break;
            }
        }
        if (tenantToMove != null) {
            Tenant finalTenant = tenantToMove;
            new AlertDialog.Builder(this)
                    .setTitle("Move Out")
                    .setMessage("Move out " + tenantToMove.getName() + "?")
                    .setPositiveButton("Date", (dialog, which) -> showMoveOutDatePicker(finalTenant))
                    .setNegativeButton("No", null).show();
        }
    }

    private void showMoveOutDatePicker(Tenant tenant) {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (v, year, month, day) -> {
            String date = String.format(Locale.US, "%02d-%02d-%d", day, month + 1, year);
            archiveTenantProcess(tenant, date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void archiveTenantProcess(Tenant tenant, String date) {
        tenant.setActive(false);
        tenant.setMoveOutDate(date);
        SupabaseClient.getService().saveToHistory(SupabaseClient.getKey(), SupabaseClient.getAuth(), tenant).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) deactivateInMainTable(tenant);
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void deactivateInMainTable(Tenant tenant) {
        SupabaseClient.getService().updateTenant(SupabaseClient.getKey(), SupabaseClient.getAuth(), "eq." + tenant.getId(), tenant).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) loadDashboardData();
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void handleRolloverRequest() {
        if (isFinishing()) return;

        new AlertDialog.Builder(this)
                .setTitle("New Month: " + selectedMonth)
                .setMessage("No financial records found. Would you like to carry forward dues from the previous month?")
                .setPositiveButton("Roll Over", (dialog, which) -> performAutoRollover())
                .setNegativeButton("Keep Empty", (dialog, which) -> {
                    calculateDashboardStats();
                    applyFilters(searchView.getQuery().toString(), spinnerFilter.getSelectedItem().toString());
                })
                .setCancelable(true)
                .show();
    }

    private void performAutoRollover() {
        String prevMonth = getPreviousMonth(selectedMonth);
        String prevYear = selectedMonth.equals("January") ? String.valueOf(Integer.parseInt(selectedYear) - 1) : selectedYear;
        performManualSync(prevMonth, prevYear);
    }

    private void calculateDashboardStats() {
        int occupiedCount = masterList.size();
        tvTenantsCount.setText(occupiedCount + " Rooms");
        tvPropertiesCount.setText(Math.max(0, 72 - occupiedCount) + " Rooms");

        double totalPending = 0;
        double totalExpected = 0;
        double totalCollected = 0;

        for (RoomMaster r : roomMasterMap.values()) {
            totalPending += r.getDueAmount();
            double target = (r.getExpectedAmount() > 0) ? r.getExpectedAmount() : r.getRentAmount();
            totalExpected += target;
            double collected = target - r.getDueAmount();
            if (collected > 0) totalCollected += collected;
        }

        tvTotalPending.setText("₹ " + (int)totalPending);
        tvTotalCollection.setText("₹ " + (int)totalCollected);
    }


    // --- HELPER: Parse Date ---
    private Date getReportDate(String m, String y) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.US);
            Date d = sdf.parse(m + " " + y);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            return c.getTime();
        } catch (Exception e) { return new Date(); }
    }

    // --- HELPER: Compare Dates ---
    private boolean isTenantValidForMonth(Tenant t, Date reportDate) {
        if (t.getMoveInDate() == null || t.getMoveInDate().isEmpty()) return true; // Assume old tenant
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            Date joining = sdf.parse(t.getMoveInDate());
            // Return TRUE if joined BEFORE or ON report date
            return joining.compareTo(reportDate) <= 0;
        } catch (Exception e) { return true; }
    }

    private int getPropertyPriority(String n) {
        if (n == null) return 99;
        String s = n.trim().toLowerCase();
        if (s.equals("hostel")) return 1;
        if (s.equals("home")) return 2;
        if (s.equals("rizz")) return 3;
        return 4;
    }

    private int compareRoomNumbers(String n1, String n2) {
        try {
            int num1 = Integer.parseInt(n1.replaceAll("\\D", ""));
            int num2 = Integer.parseInt(n2.replaceAll("\\D", ""));
            return Integer.compare(num1, num2);
        } catch (Exception e) { return n1.compareToIgnoreCase(n2); }
    }

    // UPDATED: Now accepts a Set of keys to exclude
    private List<Tenant> generateVacantList(Set<String> occupiedKeys) {
        List<Tenant> vacant = new ArrayList<>();
        // Note: We do NOT rely on 'masterList' here anymore for checking occupancy.
        // We rely on 'occupiedKeys' which has been filtered by date.

        addRange(vacant, occupiedKeys, "Hostel", 100, 119);
        addRange(vacant, occupiedKeys, "Hostel", 201, 215);
        addRange(vacant, occupiedKeys, "Hostel", 301, 318);
        addRange(vacant, occupiedKeys, "Home", 101, 105);
        addRange(vacant, occupiedKeys, "Home", 201, 204);
        addRange(vacant, occupiedKeys, "Rizz", 101, 104);
        addRange(vacant, occupiedKeys, "Rizz", 201, 202);
        return vacant;
    }

    private void addRange(List<Tenant> l, Set<String> occ, String p, int s, int e) {
        for (int i = s; i <= e; i++) {
            String k = (p.trim() + "-" + i).toUpperCase();
            if (!occ.contains(k)) {
                Tenant t = new Tenant(); t.setName("VACANT"); t.setHostelName(p); t.setRoomNumber(String.valueOf(i));
                l.add(t);
            }
        }
    }

    private void generateAndSharePDF() {
        // Your simple PDF logic remains (though PdfGenerator class is preferred)
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = doc.startPage(pi);
        Canvas c = page.getCanvas(); Paint p = new Paint(); p.setTextSize(12);
        c.drawText("Revenue Report: " + selectedMonth + " " + selectedYear, 20, 40, p);
        int y = 80;
        for (RoomData r : roomList) {
            if (!r.isVacant()) {
                String k = (r.getPropertyName().trim() + "-" + r.getRoomNumber().trim()).toUpperCase();
                double due = roomMasterMap.containsKey(k) ? roomMasterMap.get(k).getDueAmount() : 0;
                StringBuilder names = new StringBuilder();
                for (Tenant t : r.getTenantList()) names.append(t.getName()).append(" ");
                c.drawText(r.getPropertyName() + " " + r.getRoomNumber() + ": " + names.toString() + "(Due: ₹" + (int)due + ")", 20, y, p);
                y += 20; if (y > 800) break;
            }
        }
        doc.finishPage(page);
        File f = new File(getExternalCacheDir(), "Report.pdf");
        try { doc.writeTo(new FileOutputStream(f)); sharePdfFile(f); } catch (IOException e) {}
        doc.close();


    }

    // --- NEW LANDSCAPE REPORT GENERATOR ---
    private void generateActiveTenantReport(List<Tenant> masterList) {
        // 1. DATE LIMIT
        Date reportDateLimit = getReportDate(selectedMonth, selectedYear);

        // 2. FILTER & SORT
        List<Tenant> validTenants = new ArrayList<>();
        for (Tenant t : masterList) {
            if (!t.isActive() || "VACANT".equalsIgnoreCase(t.getName())) continue;
            if (!isTenantValidForMonth(t, reportDateLimit)) continue;
            validTenants.add(t);
        }

        Collections.sort(validTenants, (t1, t2) -> {
            int p1 = getReportPriority(t1.getHostelName());
            int p2 = getReportPriority(t2.getHostelName());
            if (p1 != p2) return Integer.compare(p1, p2);
            return compareRoomNumbers(t1.getRoomNumber(), t2.getRoomNumber());
        });

        // 3. PDF SETUP
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(842, 595, 1).create();
        PdfDocument.Page page = doc.startPage(pi);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint linePaint = new Paint();

        // --- FORCE WHITE BACKGROUND FOR PDF ---
        canvas.drawColor(Color.WHITE);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1);
        linePaint.setColor(Color.BLACK);

        // Title
        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        canvas.drawText("Tenant Directory: " + selectedMonth + " " + selectedYear, 300, 40, paint);

        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        String dateStr = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        canvas.drawText("Printed on: " + dateStr, 20, 60, paint);

        // Table Setup
        int y = 90;
        int margin = 20;
        int tableWidth = 800;
        int[] cols = {20, 60, 180, 320, 410, 520, 680, 765};
        int[] vLines = {55, 175, 315, 405, 515, 675, 760, 822};

        // Header Background & Text
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#EEEEEE"));
        canvas.drawRect(margin, y - 15, margin + tableWidth, y + 5, bgPaint);

        paint.setFakeBoldText(true);
        paint.setTextSize(11);
        canvas.drawText("S.No", cols[0] + 2, y, paint);
        canvas.drawText("Name", cols[1] + 2, y, paint);
        canvas.drawText("Room No.", cols[2] + 2, y, paint);
        canvas.drawText("Phone", cols[3] + 2, y, paint);
        canvas.drawText("Parent No", cols[4] + 2, y, paint);
        canvas.drawText("Occupation", cols[5] + 2, y, paint);
        canvas.drawText("Vehicle", cols[6] + 2, y, paint);
        canvas.drawText("Moved In", cols[7] + 2, y, paint);

        // Header Grid Lines
        canvas.drawLine(margin, y - 15, margin + tableWidth, y - 15, linePaint);
        canvas.drawLine(margin, y + 5, margin + tableWidth, y + 5, linePaint);
        canvas.drawLine(margin, y - 15, margin, y + 5, linePaint);
        for (int x : vLines) canvas.drawLine(x, y - 15, x, y + 5, linePaint);

        paint.setFakeBoldText(false);
        y += 20;

        int serialNo = 0;
        for (Tenant t : validTenants) {
            serialNo++;
            int rowHeight = 25;
            int textY = y + 15;

            // --- DRAW DATA (Using new val() and truncate() helpers for "NA") ---
            canvas.drawText(String.valueOf(serialNo), cols[0] + 2, textY, paint);

            // Name (Truncated)
            canvas.drawText(truncate(t.getName(), 18), cols[1] + 2, textY, paint);

            // Room (Combined)
            canvas.drawText(val(t.getHostelName()) + " - " + val(t.getRoomNumber()), cols[2] + 2, textY, paint);

            // Phone (Raw check)
            canvas.drawText(val(t.getPhone()), cols[3] + 2, textY, paint);

            // Parent Phone
            canvas.drawText(val(t.getParentPhone()), cols[4] + 2, textY, paint);

            // Company (Truncated)
            canvas.drawText(truncate(t.getCompanyName(), 20), cols[5] + 2, textY, paint);

            // Vehicle
            canvas.drawText(val(t.getVehicleNumber()), cols[6] + 2, textY, paint);

            // Date
            canvas.drawText(val(t.getMoveInDate()), cols[7] + 2, textY, paint);

            // Grid Lines for Row
            canvas.drawLine(margin, y + rowHeight, margin + tableWidth, y + rowHeight, linePaint);
            canvas.drawLine(margin, y, margin, y + rowHeight, linePaint);
            for (int x : vLines) canvas.drawLine(x, y, x, y + rowHeight, linePaint);

            y += rowHeight;

            if (y > 540) {
                doc.finishPage(page);
                page = doc.startPage(pi);
                canvas = page.getCanvas();
                y = 50;
            }
        }

        y += 20;
        paint.setFakeBoldText(true);
        paint.setColor(Color.BLUE);
        canvas.drawText("Total Active Tenants: " + serialNo, margin, y, paint);

        doc.finishPage(page);

        File f = new File(getExternalCacheDir(), "Active_Tenants_" + selectedMonth + ".pdf");
        try {
            doc.writeTo(new FileOutputStream(f));
            sharePdfFile(f);
        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        doc.close();
    }

    // --- HELPER METHOD FOR SORTING PRIORITY ---
    private String val(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "NA";
        }
        return text;
    }

    // Helper: Returns "NA" if empty, otherwise truncates long text
    private String truncate(String text, int length) {
        if (text == null || text.trim().isEmpty()) {
            return "NA";
        }
        return (text.length() > length) ? text.substring(0, length - 2) + ".." : text;
    }

    private int getReportPriority(String name) {
        if (name == null) return 99;

        String lower = name.trim().toLowerCase();

        if (lower.contains("hostel")) return 1; // Top Priority
        if (lower.contains("home")) return 2;   // Second Priority
        if (lower.contains("rizz")) return 3;   // Third Priority

        return 4; // Everything else
    }

    private void sharePdfFile(File f) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
        Intent i = new Intent(Intent.ACTION_SEND); i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_STREAM, uri); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Share"));
    }

    private void showSyncConfirmation() {
        String prevMonth = getPreviousMonth(selectedMonth);
        String prevYear = selectedMonth.equals("January") ? String.valueOf(Integer.parseInt(selectedYear) - 1) : selectedYear;

        new AlertDialog.Builder(this)
                .setTitle("Import Dues from " + prevMonth + "?")
                .setMessage("This will update " + selectedMonth + " records.\n\nLogic: New Due = Old Due + Rent.")
                .setPositiveButton("Sync Now", (dialog, which) -> performManualSync(prevMonth, prevYear))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performManualSync(String prevMonth, String prevYear) {
        Toast.makeText(this, "Step 1: Fetching " + prevMonth + "...", Toast.LENGTH_SHORT).show();

        SupabaseClient.getService().getRoomsFiltered(
                SupabaseClient.getKey(),
                SupabaseClient.getAuth(),
                "eq." + prevMonth,
                "eq." + prevYear
        ).enqueue(new Callback<List<RoomMaster>>() {
            @Override public void onResponse(Call<List<RoomMaster>> call, Response<List<RoomMaster>> responseOld) {
                if (responseOld.isSuccessful() && responseOld.body() != null && !responseOld.body().isEmpty()) {
                    List<RoomMaster> oldRecords = responseOld.body();

                    SupabaseClient.getService().getRoomsFiltered(
                            SupabaseClient.getKey(),
                            SupabaseClient.getAuth(),
                            "eq." + selectedMonth,
                            "eq." + selectedYear
                    ).enqueue(new Callback<List<RoomMaster>>() {
                        @Override public void onResponse(Call<List<RoomMaster>> call, Response<List<RoomMaster>> responseNew) {
                            Map<String, RoomMaster> currentMonthMap = new HashMap<>();
                            if (responseNew.isSuccessful() && responseNew.body() != null) {
                                for (RoomMaster r : responseNew.body()) {
                                    String key = (r.getHostelName().trim() + "-" + r.getRoomNumber().trim()).toUpperCase();
                                    currentMonthMap.put(key, r);
                                }
                            }
                            processSyncLoop(oldRecords, currentMonthMap);
                        }

                        @Override public void onFailure(Call<List<RoomMaster>> call, Throwable t) {
                            Toast.makeText(MainActivity.this, "Failed to verify " + selectedMonth + " data. Sync cancelled.", Toast.LENGTH_LONG).show();
                        }
                    });

                } else {
                    Toast.makeText(MainActivity.this, "No records found in " + prevMonth, Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<List<RoomMaster>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processSyncLoop(List<RoomMaster> oldRecords, Map<String, RoomMaster> currentMonthMap) {
        int totalUpdates = oldRecords.size();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        StringBuilder errorLog = new StringBuilder();

        for (RoomMaster old : oldRecords) {
            double totalTarget = old.getDueAmount() + old.getRentAmount();
            double alreadyPaid = 0;
            double rentToUse = old.getRentAmount();

            String key = (old.getHostelName().trim() + "-" + old.getRoomNumber().trim()).toUpperCase();

            if (currentMonthMap.containsKey(key)) {
                RoomMaster current = currentMonthMap.get(key);
                rentToUse = current.getRentAmount();
                double expectedBase = (current.getExpectedAmount() > 0) ? current.getExpectedAmount() : current.getRentAmount();
                alreadyPaid = Math.max(0, expectedBase - current.getDueAmount());
            }

            double newDue = Math.max(0, totalTarget - alreadyPaid);

            RoomMaster newRecord = new RoomMaster(
                    old.getHostelName(),
                    old.getRoomNumber(),
                    rentToUse,
                    newDue,
                    totalTarget,
                    selectedMonth,
                    selectedYear
            );

            SupabaseClient.getService().upsertRoom(
                    SupabaseClient.getKey(),
                    SupabaseClient.getAuth(),
                    "hostel_name,room_number,billing_month,billing_year",
                    newRecord
            ).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) successCount.incrementAndGet();
                    else failCount.incrementAndGet();
                    checkCompletion(successCount, failCount, totalUpdates, errorLog);
                }
                @Override public void onFailure(Call<Void> call, Throwable t) {
                    failCount.incrementAndGet();
                    checkCompletion(successCount, failCount, totalUpdates, errorLog);
                }
            });
        }
    }

    private void checkCompletion(AtomicInteger s, AtomicInteger f, int total, StringBuilder logs) {
        if (s.get() + f.get() == total) {
            if (f.get() > 0) Toast.makeText(MainActivity.this, "Sync Completed with some errors.", Toast.LENGTH_LONG).show();
            else Toast.makeText(MainActivity.this, "Sync Successful!", Toast.LENGTH_SHORT).show();
            loadDashboardData();
        }
    }

    private String getPreviousMonth(String month) {
        List<String> months = Arrays.asList("January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        int idx = months.indexOf(month);
        if (idx == -1) return "November";
        return (idx == 0) ? "December" : months.get(idx - 1);
    }
}
package com.example.propertymanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CsvExporter {

    // --- 1. EXPORT TENANT LIST (Filtered by Month/Year) ---
    public static void exportTenants(Context context, List<Tenant> masterList, String month, String year) {
        StringBuilder csvData = new StringBuilder();

        // Header Info
        csvData.append("Tenant Directory For:,").append(month).append(" ").append(year).append("\n\n");

        // Table Columns
        csvData.append("S.No,Name,Property - Room,Phone No,Parent Phone,Occupation,Vehicle,Moved In\n");

        // Date Logic for Filtering
        Date limitDate = getReportDate(month, year);
        int serialNo = 0;

        for (Tenant t : masterList) {
            // 1. Skip if not active or is a placeholder "VACANT"
            if (!t.isActive() || "VACANT".equalsIgnoreCase(t.getName())) continue;

            // 2. Skip if they joined AFTER this report month
            if (!isTenantValidForMonth(t, limitDate)) continue;

            serialNo++;

            // 3. Build Row
            csvData.append(serialNo).append(",");
            csvData.append(escape(t.getName())).append(",");

            // Property + Room Combined
            String propRoom = val(t.getHostelName()) + " - " + val(t.getRoomNumber());
            csvData.append(escape(propRoom)).append(",");

            csvData.append(escape(t.getPhone())).append(",");

            // Check if getParentPhone exists in your Tenant class (based on your snippet)
            csvData.append(escape(t.getParentPhone())).append(",");

            // Occupation (Company Name)
            csvData.append(escape(t.getCompanyName())).append(",");

            // Vehicle
            csvData.append(escape(t.getVehicleNumber())).append(",");

            csvData.append(escape(t.getMoveInDate())).append("\n");
        }

        String fileName = "Active_Tenants_" + month + "_" + System.currentTimeMillis() + ".csv";
        saveAndShare(context, csvData.toString(), fileName);
    }

    // --- 2. EXPORT PAYMENT HISTORY ---
    public static void exportPayments(Context context, List<PaymentLog> paymentList) {
        StringBuilder csvData = new StringBuilder();
        csvData.append("Date,Time,Property,Room No,Amount,Mode\n");

        for (PaymentLog log : paymentList) {
            String fullDate = log.getCreatedAt();
            String date = "Unknown";
            String time = "Unknown";

            if (fullDate != null && fullDate.contains("T")) {
                String[] parts = fullDate.split("T");
                date = parts[0];
                if (parts[1].length() >= 5) time = parts[1].substring(0, 5);
            }

            csvData.append(date).append(",");
            csvData.append(time).append(",");
            csvData.append(escape(log.getHostelName())).append(",");
            csvData.append(escape(log.getRoomNumber())).append(",");
            csvData.append(log.getAmountPaid()).append(",");

            String mode = log.getPaymentMode();
            csvData.append(mode != null ? mode : "Cash").append("\n");
        }

        saveAndShare(context, csvData.toString(), "Payment_Report.csv");
    }

    // --- 3. EXPORT RENT REPORT (Financials) ---
    public static void exportRentReport(Context context, List<RoomData> roomList, String month, String year, String propertyName) {
        StringBuilder csvData = new StringBuilder();

        csvData.append("Property Name:,").append(escape(propertyName)).append("\n");
        csvData.append("Month:,").append(month).append(" ").append(year).append("\n\n");
        csvData.append("Room,Tenant Name,Rent,Old Due,Total Due,Paid\n");

        double sumRent = 0, sumLastDue = 0, sumTotalDue = 0, sumPaid = 0;

        for (RoomData room : roomList) {
            if (room.isVacant()) continue;

            double rent = room.getStandardRent();
            double totalDue = room.getCurrentDue();
            double expected = room.getExpectedAmount();
            if (expected == 0) expected = Math.max(totalDue, rent);
            double lastDue = Math.max(0, expected - rent);
            double paid = Math.max(0, expected - totalDue);

            sumRent += rent; sumLastDue += lastDue; sumTotalDue += totalDue; sumPaid += paid;

            csvData.append(escape(room.getRoomNumber())).append(",");
            csvData.append(escape(room.getTenantName())).append(",");
            csvData.append((int)rent).append(",");
            csvData.append((int)lastDue).append(",");
            csvData.append((int)totalDue).append(",");
            csvData.append((int)paid).append("\n");
        }

        csvData.append("\nTOTALS:,");
        csvData.append(",");
        csvData.append((int)sumRent).append(",");
        csvData.append((int)sumLastDue).append(",");
        csvData.append((int)sumTotalDue).append(",");
        csvData.append((int)sumPaid).append("\n");

        String fileName = propertyName + "_Rent_Report_" + month + ".csv";
        saveAndShare(context, csvData.toString(), fileName);
    }

    // --- HELPERS ---

    private static void saveAndShare(Context context, String data, String fileName) {
        try {
            File file = new File(context.getExternalCacheDir(), fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(data);
            writer.flush();
            writer.close();

            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Property Manager Export: " + fileName);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Save Excel CSV via..."));
        } catch (Exception e) {
            Toast.makeText(context, "Error exporting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static String escape(String value) {
        if (value == null) return "-";
        return value.replace(",", " ");
    }

    private static String val(String value) {
        return (value == null || value.isEmpty()) ? "-" : value;
    }

    // --- DATE FILTER LOGIC (Included here to be self-contained) ---
    private static Date getReportDate(String month, String year) {
        try {
            // Set limit to END of the selected month
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.US);
            Date d = sdf.parse(month + " " + year);
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(d);
            c.set(java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
            c.set(java.util.Calendar.HOUR_OF_DAY, 23);
            return c.getTime();
        } catch (Exception e) { return new Date(); }
    }

    private static boolean isTenantValidForMonth(Tenant t, Date limitDate) {
        try {
            if (t.getMoveInDate() == null || t.getMoveInDate().length() < 10) return true;
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            Date moveIn = sdf.parse(t.getMoveInDate());
            // Tenant is valid if they moved in BEFORE or ON the report month end
            return moveIn != null && !moveIn.after(limitDate);
        } catch (Exception e) { return true; }
    }
}
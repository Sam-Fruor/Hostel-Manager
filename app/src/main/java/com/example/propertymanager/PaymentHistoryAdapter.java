package com.example.propertymanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {

    private List<PaymentLog> list;

    public PaymentHistoryAdapter(List<PaymentLog> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_history, parent, false);
        return new ViewHolder(v);
    }

    // Inside PaymentHistoryAdapter.java

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentLog log = list.get(position);

        // 1. Set Property Name
        holder.tvPropName.setText(log.getHostelName() + " " + log.getRoomNumber());

        // 2. Get Payment Mode (Cash vs UPI)
        String mode = log.getPaymentMode();
        if (mode == null || mode.isEmpty()) {
            mode = "Cash"; // Default for old records that didn't have a mode
        }

        // 3. Format Date & Append Mode
        // We take your existing time format and add " • Cash" or " • UPI" to it
        String dateTime = formatTimeAndDate(log.getCreatedAt());
        holder.tvDate.setText(dateTime + " • " + mode);

        // 4. Set Amount
        holder.tvAmount.setText("+ ₹" + (int)log.getAmountPaid());

        // Optional: You can make UPI stand out nicely
        if (mode.equalsIgnoreCase("UPI")) {
            // holder.tvDate.setTextColor(Color.parseColor("#673AB7")); // Uncomment for Purple text on UPI
        }
    }

    // --- NEW HELPER METHOD ---
    private String formatTimeAndDate(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) return "Unknown Date";

        try {
            // 1. Define the format Supabase sends (UTC)
            // Example: 2025-12-21T10:30:00.123456+00:00
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC")); // Tell Java this input is UTC

            // 2. Parse the string into a Date object
            java.util.Date date = inputFormat.parse(isoTimestamp);

            // 3. Define the output format you want (Local Time)
            // HH:mm = 24 Hour format (e.g., 14:30)
            // dd-MM-yyyy = Date
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("HH:mm\ndd-MM-yyyy", java.util.Locale.getDefault());

            // This automatically uses the phone's default timezone (IST)
            return outputFormat.format(date);

        } catch (Exception e) {
            // Fallback: just show the raw string if parsing fails
            return isoTimestamp;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPropName, tvDate, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPropName = itemView.findViewById(R.id.tvPropName);
            tvDate = itemView.findViewById(R.id.tvPaymentDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
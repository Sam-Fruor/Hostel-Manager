package com.example.propertymanager;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class PaymentLog implements Serializable {

    @SerializedName("hostel_name")
    private String hostelName;

    @SerializedName("room_number")
    private String roomNumber;

    @SerializedName("amount_paid")
    private double amountPaid;

    @SerializedName("payment_date")
    private String paymentDate;

    @SerializedName("billing_month") // Changed to match Supabase column usually named billing_month
    private String paymentMonth;

    @SerializedName("billing_year")  // Changed to match Supabase column usually named billing_year
    private String paymentYear;

    @SerializedName("payment_mode")
    private String paymentMode;

    @SerializedName("created_at")
    private String createdAt;

    // --- UPDATED CONSTRUCTOR (Now accepts 7 arguments) ---
    public PaymentLog(String hostelName, String roomNumber, double amountPaid, String paymentDate, String paymentMonth, String paymentYear, String paymentMode) {
        this.hostelName = hostelName;
        this.roomNumber = roomNumber;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMonth = paymentMonth;
        this.paymentYear = paymentYear;
        this.paymentMode = paymentMode; // Now correctly assigned
    }

    // --- GETTERS ---
    public String getHostelName() { return hostelName; }
    public String getRoomNumber() { return roomNumber; }
    public double getAmountPaid() { return amountPaid; }
    public String getPaymentDate() { return paymentDate; }

    public String getPaymentMonth() { return paymentMonth; }
    public String getPaymentYear() { return paymentYear; }

    public String getPaymentMode() { return paymentMode; }
    public String getCreatedAt() { return createdAt; }
}
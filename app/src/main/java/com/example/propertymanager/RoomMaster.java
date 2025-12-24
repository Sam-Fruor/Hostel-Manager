package com.example.propertymanager;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class RoomMaster implements Serializable {

    @SerializedName("hostel_name")
    private String hostelName;

    @SerializedName("room_number")
    private String roomNumber;

    @SerializedName("rent_amount")
    private double rentAmount;

    @SerializedName("due_amount")
    private double dueAmount;

    // --- NEW: TOTAL TARGET (Rent + Arrears) ---
    // This allows accurate calculation of "Collected" amounts
    @SerializedName("expected_amount")
    private double expectedAmount;

    @SerializedName("billing_month")
    private String billingMonth;

    @SerializedName("billing_year")
    private String billingYear;

    public RoomMaster() {}

    // --- FULL CONSTRUCTOR (7 Parameters) ---
    public RoomMaster(String hostelName, String roomNumber, double rentAmount, double dueAmount, double expectedAmount, String billingMonth, String billingYear) {
        this.hostelName = hostelName;
        this.roomNumber = roomNumber;
        this.rentAmount = rentAmount;
        this.dueAmount = dueAmount;
        this.expectedAmount = expectedAmount;
        this.billingMonth = billingMonth;
        this.billingYear = billingYear;
    }

    // Keep compatibility constructors (optional, but good for safety)
    public RoomMaster(String hostelName, String roomNumber, double rentAmount, double dueAmount) {
        this.hostelName = hostelName;
        this.roomNumber = roomNumber;
        this.rentAmount = rentAmount;
        this.dueAmount = dueAmount;
    }

    // Getters and Setters
    public String getHostelName() { return hostelName; }
    public void setHostelName(String hostelName) { this.hostelName = hostelName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public double getRentAmount() { return rentAmount; }
    public void setRentAmount(double rentAmount) { this.rentAmount = rentAmount; }

    public double getDueAmount() { return dueAmount; }
    public void setDueAmount(double dueAmount) { this.dueAmount = dueAmount; }

    public double getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(double expectedAmount) { this.expectedAmount = expectedAmount; }

    public String getBillingMonth() { return billingMonth; }
    public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }

    public String getBillingYear() { return billingYear; }
    public void setBillingYear(String billingYear) { this.billingYear = billingYear; }
}
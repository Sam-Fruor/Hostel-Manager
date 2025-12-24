package com.example.propertymanager;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Tenant implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    @SerializedName("hostel_name")
    private String hostelName;

    @SerializedName("floor")
    private String floor;

    @SerializedName("room_number")
    private String roomNumber;

    // Matches Database Column: id_proof_front
    @SerializedName("aadhar_front_url")
    private String aadharFrontUrl;

    // Matches Database Column: id_proof_back
    @SerializedName("aadhar_back_url")
    private String aadharBackUrl;

    // Matches Database Column: tenant_form
    @SerializedName("form_photo_url")
    private String formPhotoUrl;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("move_in_date")
    private String moveInDate;

    @SerializedName("move_out_date")
    private String moveOutDate;

    @SerializedName("parent_phone")
    private String parentPhone;

    @SerializedName("company_name")
    private String companyName;

    @SerializedName("vehicle_number")
    private String vehicleNumber;

    @SerializedName("vehicle_type")
    private String vehicleType;

    public Tenant() {}

    public Tenant(String name, String phone, String hostelName, String floor, String roomNumber,
                  String frontUrl, String backUrl, String formUrl, String moveInDate,
                  String parentPhone, String companyName) {
        this.name = name;
        this.phone = phone;
        this.hostelName = hostelName;
        this.floor = floor;
        this.roomNumber = roomNumber;
        this.isActive = true;
        this.aadharFrontUrl = frontUrl;
        this.aadharBackUrl = backUrl;
        this.formPhotoUrl = formUrl;
        this.moveInDate = moveInDate;
        this.parentPhone = parentPhone;
        this.companyName = companyName;
    }

    // --- GETTERS ---
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getHostelName() { return hostelName; }
    public String getRoomNumber() { return roomNumber; }
    public String getFloor() { return floor; }

    public String getAadharFrontUrl() { return aadharFrontUrl; }
    public String getAadharBackUrl() { return aadharBackUrl; }
    public String getFormPhotoUrl() { return formPhotoUrl; }

    public boolean isActive() { return isActive; }
    public String getMoveInDate() { return moveInDate; }

    // ADDED THIS TO FIX THE ADAPTER ERROR
    public String getMoveOutDate() { return moveOutDate; }

    public String getParentPhone() { return parentPhone; }
    public String getCompanyName() { return companyName; }
    public String getVehicleNumber() { return vehicleNumber; }
    public String getVehicleType() { return vehicleType; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setHostelName(String hostelName) { this.hostelName = hostelName; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setFloor(String floor) { this.floor = floor; }

    public void setAadharFrontUrl(String aadharFrontUrl) { this.aadharFrontUrl = aadharFrontUrl; }
    public void setAadharBackUrl(String aadharBackUrl) { this.aadharBackUrl = aadharBackUrl; }
    public void setFormPhotoUrl(String formPhotoUrl) { this.formPhotoUrl = formPhotoUrl; }

    public void setActive(boolean active) { isActive = active; }
    public void setMoveInDate(String moveInDate) { this.moveInDate = moveInDate; }
    public void setMoveOutDate(String moveOutDate) { this.moveOutDate = moveOutDate; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
}
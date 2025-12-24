package com.example.propertymanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoomData implements Serializable {
    private String propertyName;
    private String roomNumber;
    private List<Tenant> tenantList;

    private double standardRent;
    private double currentDue;

    // Stores the Total Target (Rent + Arrears) for accurate math
    private double expectedAmount;

    // --- NEW FIELD FOR DATE FILTERING ---
    private boolean displayAsVacant = false;

    // --- NEW FIELD FOR DUE DATE ---
    private String dueDate = "-";

    public RoomData(String propertyName, String roomNumber) {
        this.propertyName = propertyName;
        this.roomNumber = roomNumber;
        this.tenantList = new ArrayList<>();
        this.standardRent = 0;
        this.currentDue = 0;
        this.expectedAmount = 0;
        this.displayAsVacant = false;
    }

    public void addTenant(Tenant t) {
        tenantList.add(t);
    }

    public String getPropertyName() { return propertyName; }
    public String getRoomNumber() { return roomNumber; }
    public List<Tenant> getTenantList() { return tenantList; }

    public void setDisplayAsVacant(boolean isVacant) {
        this.displayAsVacant = isVacant;
    }

    public boolean isVacant() {
        // If forced vacant by date filter, return true
        if (this.displayAsVacant) return true;

        if (tenantList.isEmpty()) return true;
        return tenantList.size() == 1 && "VACANT".equals(tenantList.get(0).getName());
    }

    public double getStandardRent() { return standardRent; }
    public void setStandardRent(double standardRent) { this.standardRent = standardRent; }

    public double getCurrentDue() { return currentDue; }
    public void setCurrentDue(double currentDue) { this.currentDue = currentDue; }

    public double getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(double expectedAmount) { this.expectedAmount = expectedAmount; }

    // --- NEW GETTER & SETTER ---
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public int getOccupantCount() {
        if (isVacant()) return 0;
        return tenantList.size();
    }

    public String getTenantName() {
        // If forced vacant by date filter, return empty
        if (this.displayAsVacant) return "";

        if (tenantList == null || tenantList.isEmpty()) {
            return ""; // Vacant
        }

        // If there is only one tenant, return their name
        if (tenantList.size() == 1) {
            return tenantList.get(0).getName();
        }

        // If multiple tenants, join them with commas
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tenantList.size(); i++) {
            sb.append(tenantList.get(i).getName());
            if (i < tenantList.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
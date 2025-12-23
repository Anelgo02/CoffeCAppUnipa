package com.example.coffecappunipa.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DistributorState {

    public static class FaultItem {
        private String code; //es:F-12
        private String description; // distributor_faults.description
        private LocalDateTime createdAt; //distributor_faults.created_at

        public String getCode() {
            return code;
        }
        public void setCode(String code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

    }

    private String code; // distributors.code (UNIPA-001)
    private String locationName; //distributors.location_name
    private String status; // distributors.status (ACTIVE/MAINTENANCE/FAULT)


    private int coffeeLevel;
    private int milkLevel;
    private int sugarLevel;
    private int cupsLevel;

    private final List<FaultItem> faults = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    public String getLocationName() {
        return locationName;
    }
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public int getCoffeeLevel() {
        return coffeeLevel;
    }
    public void setCoffeeLevel(int coffeeLevel) {
        this.coffeeLevel = coffeeLevel;
    }
    public int getMilkLevel() {
        return milkLevel;
    }
    public void setMilkLevel(int milkLevel) {
        this.milkLevel = milkLevel;
    }
    public int getSugarLevel() {
        return sugarLevel;
    }
    public void setSugarLevel(int sugarLevel) {
        this.sugarLevel = sugarLevel;
    }
    public int getCupsLevel() {
        return cupsLevel;
    }
    public void setCupsLevel(int cupsLevel) {
        this.cupsLevel = cupsLevel;
    }
    public List<FaultItem> getFaults() {
        return faults;
    }


}

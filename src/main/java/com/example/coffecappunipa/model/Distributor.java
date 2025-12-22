package com.example.coffecappunipa.model;

public class Distributor {
    private long id;
    private String code;          // es: D-001
    private String locationName;  // es: Edificio 1
    private String status;        // ACTIVE / MAINTENANCE / FAULT

    public Distributor() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
package com.example.coffecappunipa.model;

import java.math.BigDecimal;

public class User {
    private long id;
    private String username;
    private String email;
    private String role;   // CUSTOMER / MAINTAINER / MANAGER
    private BigDecimal credit;

    public User() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
}
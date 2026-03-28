package com.epda.crs.model;

import com.epda.crs.enums.AccountStatus;
import com.epda.crs.enums.UserRole;
import java.io.Serializable;
import java.time.LocalDateTime;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String password;
    private UserRole role;
    private AccountStatus status;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(Long id, String username, String fullName, String email,
                String password, UserRole role, AccountStatus status) {
        this.id        = id;
        this.username  = username;
        this.fullName  = fullName;
        this.email     = email;
        this.password  = password;
        this.role      = role;
        this.status    = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Add this near the top with the other variables
    private LocalDateTime lastLogin;

    // Add these getters and setters at the bottom
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
}

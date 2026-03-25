package com.epda.crs.service;

import com.epda.crs.dao.UserDAO;
import com.epda.crs.exception.AuthenticationException;
import com.epda.crs.model.User;
import com.epda.crs.enums.AccountStatus;
import java.util.Optional;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Stateless
public class AuthService {
    
    @Inject
    private UserDAO userDAO;

    @Inject
    private AuditLogService auditLogService;

    public User login(String username, String password) {
        Optional<User> user = userDAO.findByUsername(username);
        String hashedPassword = hashPassword(password);
        
        if (user.isPresent() && user.get().getPassword().equals(hashedPassword)) {
            if (user.get().getStatus() == AccountStatus.INACTIVE) {
                auditLogService.logAction(username, "LOGIN_FAILED", "User", user.get().getId(), "Attempted login on inactive account");
                throw new AuthenticationException("Account is inactive. Please contact the administrator.");
            }
            auditLogService.logAction(username, "LOGIN_SUCCESS", "User", user.get().getId(), "User logged in successfully");
            return user.get();
        }
        
        auditLogService.logAction(username, "LOGIN_FAILED", "User", null, "Invalid credentials");
        throw new AuthenticationException("Invalid username or password.");
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }
}
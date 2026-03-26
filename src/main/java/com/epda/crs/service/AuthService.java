package com.epda.crs.service;

import com.epda.crs.dao.UserDAO;
import com.epda.crs.exception.AuthenticationException;
import com.epda.crs.model.User;
import com.epda.crs.enums.AccountStatus;
import java.util.Optional;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Stateless
public class AuthService {

    @Inject
    private UserDAO userDAO;

    @EJB
    private AuditLogService auditLogService;

    public User login(String username, String password) {
        Optional<User> userOptional = userDAO.findByUsername(username);
        if (userOptional.isEmpty()) {
            auditLogService.logAction(username, "LOGIN_FAILED", "User", null, "Invalid credentials");
            throw new AuthenticationException("Invalid username or password.");
        }

        User user = userOptional.get();
        if (!verifyPassword(password, user.getPassword())) {
            auditLogService.logAction(username, "LOGIN_FAILED", "User", user.getId(), "Invalid credentials");
            throw new AuthenticationException("Invalid username or password.");
        }

        if (user.getStatus() == AccountStatus.INACTIVE) {
            auditLogService.logAction(username, "LOGIN_FAILED", "User", user.getId(), "Attempted login on inactive account");
            throw new AuthenticationException("Account is inactive. Please contact the administrator.");
        }

        auditLogService.logAction(username, "LOGIN_SUCCESS", "User", user.getId(), "User logged in successfully");
        return user;
        
    }

    public static String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            new java.security.SecureRandom().nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    private boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            byte[] combined = Base64.getDecoder().decode(hashedPassword);
            byte[] salt = new byte[16];
            System.arraycopy(combined, 0, salt, 0, 16);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(plainPassword.getBytes());
            for (int i = 0; i < hash.length; i++) {
                if (hash[i] != combined[i + 16]) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

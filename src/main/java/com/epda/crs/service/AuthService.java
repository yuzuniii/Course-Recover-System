package com.epda.crs.service;

import com.epda.crs.dao.UserDAO;
import com.epda.crs.exception.AuthenticationException;
import com.epda.crs.model.User;
import com.epda.crs.enums.AccountStatus;
import java.util.Optional;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.mindrot.jbcrypt.BCrypt;

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
        if (!BCrypt.checkpw(password, user.getPassword())) {
            auditLogService.logAction(username, "LOGIN_FAILED", "User", user.getId(), "Invalid credentials");
            throw new AuthenticationException("Invalid username or password.");
        }

        if (user.getStatus() == AccountStatus.INACTIVE) {
            auditLogService.logAction(username, "LOGIN_FAILED", "User", user.getId(), "Attempted login on inactive account");
            throw new AuthenticationException("Account is inactive. Please contact the administrator.");
        }

        userDAO.updateLastLogin(user.getId());
        user.setLastLogin(java.time.LocalDateTime.now());
        auditLogService.logAction(username, "LOGIN_SUCCESS", "User", user.getId(), "User logged in successfully");
        return user;
        
    }

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}

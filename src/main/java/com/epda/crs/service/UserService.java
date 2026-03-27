package com.epda.crs.service;

import com.epda.crs.dao.UserDAO;
import com.epda.crs.model.User;
import com.epda.crs.enums.AccountStatus;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

@Stateless
public class UserService {

    @Inject
    private UserDAO userDAO;

    @EJB
    private AuditLogService auditLogService;

    public List<User> getUsers() { 
        return userDAO.findAll(); 
    }

    public void saveUser(User user, String actorUsername) {
        // SAVING AS PLAIN TEXT (Removed BCrypt Hashing)
        if (user.getId() == null || user.getId() == 0) {
            userDAO.save(user); 
            auditLogService.logAction(actorUsername, "CREATE_USER", "User", user.getId(), "Created new user: " + user.getUsername());
        } else {
            userDAO.update(user); 
            auditLogService.logAction(actorUsername, "UPDATE_USER", "User", user.getId(), "Updated user details: " + user.getUsername());
        }
    }

    public void deleteUser(Long userId, String actorUsername) {
        userDAO.delete(userId);
        auditLogService.logAction(actorUsername, "DELETE_USER", "User", userId, "Deleted user account.");
    }

    public User authenticate(String username, String plainPassword) {
        String cleanUsername = (username != null) ? username.trim() : "";
        String cleanPassword = (plainPassword != null) ? plainPassword.trim() : "";

        System.out.println("[AUTH] Checking PLAIN TEXT login for: " + cleanUsername);
        
        Optional<User> userOpt = userDAO.findByUsername(cleanUsername);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // SIMPLE STRING COMPARISON
            if (cleanPassword.equals(user.getPassword())) {
                if (user.getStatus() == AccountStatus.ACTIVE) {
                    System.out.println("[AUTH] SUCCESS: Plain text password matches!");
                    userDAO.updateLastLogin(user.getId());
                    return user;
                } else {
                    System.out.println("[AUTH] FAILURE: Account is " + user.getStatus());
                    return null;
                }
            } else {
                System.out.println("[AUTH] FAILURE: Passwords do not match.");
            }
        } else {
            System.out.println("[AUTH] FAILURE: User not found.");
        }
        return null;
    }
}
package com.epda.crs.service;

import com.epda.crs.dao.UserDAO;
import com.epda.crs.model.User;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

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
        if (user.getId() == null || user.getId() == 0) {
            // Hash password before saving new user
            user.setPassword(AuthService.hashPassword(user.getPassword()));
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
}

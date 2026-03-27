package com.epda.crs.bean;

import com.epda.crs.model.User;
import com.epda.crs.service.UserService;
import com.epda.crs.service.AuditLogService;
import java.io.IOException;
import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@SessionScoped
public class LoginBean implements Serializable {
    
    @Inject
    private UserService userService;

    @Inject
    private AuditLogService auditLogService;
    
    private String username;
    private String password;
    private User currentUser;

    public String login() {
        try {
            System.out.println("[LOGIN BEAN] Attempting login for: " + username);
            currentUser = userService.authenticate(username, password);
            
            if (currentUser != null) {
                System.out.println("[LOGIN BEAN] Success! Setting session...");
                FacesContext.getCurrentInstance().getExternalContext()
                    .getSessionMap().put("currentUser", currentUser);
                
                // SAFETY NET: If the Audit Log crashes, DO NOT break the login!
                try {
                    auditLogService.logAction(currentUser.getUsername(), "LOGIN_SUCCESS", "SYSTEM", currentUser.getId(), "User logged in successfully");
                } catch (Exception auditEx) {
                    System.err.println("[LOGIN BEAN] Audit Log Failed, but letting login proceed: " + auditEx.getMessage());
                }
                
                return "/pages/dashboard.xhtml?faces-redirect=true";
            } else {
                System.out.println("[LOGIN BEAN] Authentication failed (returned null).");
                try {
                    auditLogService.logAction(username, "LOGIN_FAILED", "SYSTEM", null, "Failed login attempt");
                } catch (Exception auditEx) {}

                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid credentials or disabled account", null));
                return null;
            }
            
        } catch (Exception e) {
            // CRITICAL FIX: Print the actual system error to the console and the screen!
            System.err.println("[LOGIN BEAN] CRITICAL EXCEPTION: " + e.getMessage());
            e.printStackTrace(); 
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_FATAL, "System Error: " + e.getMessage(), "Check server console."));
            
            return null; 
        }
    }

    public void logout() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        if (currentUser != null) {
            try {
                auditLogService.logAction(currentUser.getUsername(), "LOGOUT", "SYSTEM", currentUser.getId(), "User logged out");
            } catch(Exception e) {}
        }
        context.getExternalContext().invalidateSession();
        currentUser = null;
        context.getExternalContext().redirect(context.getExternalContext().getRequestContextPath() + "/pages/login.xhtml");
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public User getCurrentUser() { return currentUser; }
}
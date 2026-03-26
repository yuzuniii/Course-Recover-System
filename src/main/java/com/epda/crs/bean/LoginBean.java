package com.epda.crs.bean;

import com.epda.crs.exception.AuthenticationException;
import com.epda.crs.model.User;
import com.epda.crs.enums.UserRole;
import com.epda.crs.service.AuthService;
import java.io.IOException;
import java.io.Serializable;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@SessionScoped
public class LoginBean implements Serializable {
    
    @EJB
    private AuthService authService;
    
    private String username;
    private String password;
    private User currentUser;

    public String login() {
        try {
            currentUser = authService.login(username, password);
            
            // 1. Establish Session
            FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().put("currentUser", currentUser);
            
            // 2. Role-Based Redirect
            if (currentUser.getRole() == UserRole.COURSE_ADMINISTRATOR) {
                return "/pages/users.xhtml?faces-redirect=true";
            } else if (currentUser.getRole() == UserRole.ACADEMIC_OFFICER) {
                return "/pages/dashboard.xhtml?faces-redirect=true";
            }
            
            // Default fallback
            return "/pages/dashboard.xhtml?faces-redirect=true";
            
        } catch (AuthenticationException exception) {
            // Catches normal login errors (e.g., wrong password)
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, exception.getMessage(), null));
            return null; 
            
        } catch (Exception e) {
            // Catches severe system/database crashes
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            
            String errorMessage = "System Error: " + rootCause.getMessage();
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_FATAL, errorMessage, null));
                
            return null; 
        }
    }

    public void logout() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().invalidateSession();
        currentUser = null;
        context.getExternalContext().redirect(context.getExternalContext().getRequestContextPath() + "/pages/login.xhtml");
    }

    // ==========================================
    // GETTERS AND SETTERS (Required by JSF)
    // ==========================================
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public User getCurrentUser() { return currentUser; }
}

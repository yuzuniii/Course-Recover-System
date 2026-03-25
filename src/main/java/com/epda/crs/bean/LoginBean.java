package com.epda.crs.bean;

import com.epda.crs.exception.AuthenticationException;
import com.epda.crs.model.User;
import com.epda.crs.service.AuthService;
import java.io.IOException;
import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@SessionScoped
public class LoginBean implements Serializable {
    private final AuthService authService = new AuthService();
    private String username;
    private String password;
    private User currentUser;

    public String login() {
        try {
            currentUser = authService.login(username, password);
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentUser", currentUser);
            return "/pages/dashboard.xhtml?faces-redirect=true";
        } catch (AuthenticationException exception) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, exception.getMessage(), null));
            return null;
        }
    }

    public void logout() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().invalidateSession();
        currentUser = null;
        context.getExternalContext().redirect(context.getExternalContext().getRequestContextPath() + "/login.xhtml");
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public User getCurrentUser() { return currentUser; }
}

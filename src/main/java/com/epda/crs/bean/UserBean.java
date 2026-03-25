package com.epda.crs.bean;

import com.epda.crs.model.User;
import com.epda.crs.service.UserService;
import com.epda.crs.enums.UserRole;
import com.epda.crs.enums.AccountStatus;
import java.io.Serializable;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.primefaces.PrimeFaces;

@Named
@ViewScoped
public class UserBean implements Serializable {
    private final UserService userService = new UserService();
    
    private List<User> users;
    private User selectedUser;

    @PostConstruct
    public void init() {
        users = userService.getUsers();
    }

    // Prepare a blank user for the "Add New User" dialog
    public void prepareNewUser() {
        this.selectedUser = new User();
        this.selectedUser.setStatus(AccountStatus.ACTIVE);
    }

    // Save or Update user
    public void saveUser() {
        try {
            userService.saveUser(selectedUser);
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage("User Saved Successfully"));
            
            // Refresh list and hide dialog
            users = userService.getUsers(); 
            PrimeFaces.current().executeScript("PF('userDialog').hide()");
            PrimeFaces.current().ajax().update("form:messages", "form:dt-users");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not save user."));
        }
    }

    // Deactivate or Activate user
    public void toggleStatus(User user) {
        boolean activate = user.getStatus() != AccountStatus.ACTIVE;
        userService.toggleUserStatus(user.getId(), activate);
        users = userService.getUsers(); // Refresh data
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage("User status updated."));
    }

    // Getters and Setters
    public List<User> getUsers() { return users; }
    public User getSelectedUser() { return selectedUser; }
    public void setSelectedUser(User selectedUser) { this.selectedUser = selectedUser; }

    // Helpers for dropdowns in UI
    public UserRole[] getRoles() { return UserRole.values(); }
    public AccountStatus[] getStatuses() { return AccountStatus.values(); }
}
package com.epda.crs.bean;

import com.epda.crs.model.User;
import com.epda.crs.service.UserService;
import com.epda.crs.enums.UserRole;
import com.epda.crs.enums.AccountStatus;
import java.io.Serializable;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.primefaces.PrimeFaces;

@Named
@ViewScoped
public class UserBean implements Serializable {
    
    // FIX 1: Use @Inject instead of 'new UserService()'
    @Inject
    private UserService userService;
    
    private List<User> users;
    private User selectedUser;

    @PostConstruct
    public void init() {
        users = userService.getUsers();
    }

    // FIX 2: Helper method to retrieve the logged-in user's username from the session
    private String getCurrentUsername() {
        User currentUser = (User) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("currentUser");
        return currentUser != null ? currentUser.getUsername() : "System";
    }

    // Prepare a blank user for the "Add New User" dialog
    public void prepareNewUser() {
        this.selectedUser = new User();
        this.selectedUser.setStatus(AccountStatus.ACTIVE);
    }

    // Prepare an existing user for the "Edit User" dialog
    public void prepareEditUser(User user) {
        this.selectedUser = user;
    }

    // Save or Update user
    public void saveUser() {
        try {
            String actorUsername = getCurrentUsername();
            boolean isNew = (selectedUser.getId() == null || selectedUser.getId() == 0);
            
            userService.saveUser(selectedUser, actorUsername);
            
            String msg = isNew ? "User Created Successfully" : "User Updated Successfully";
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", msg));
            
            // Refresh list and update UI
            users = userService.getUsers(); 
            PrimeFaces.current().executeScript("PF('userDialog').hide()");
            PrimeFaces.current().ajax().update("dt-users", "globalMessages");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not save user: " + e.getMessage()));
        }
    }

    public void deleteUser() {
        try {
            if (selectedUser != null && selectedUser.getId() != null) {
                String actorUsername = getCurrentUsername();
                userService.deleteUser(selectedUser.getId(), actorUsername);
                
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "User Deleted Successfully"));
                
                // Refresh list and update UI
                users = userService.getUsers(); 
                PrimeFaces.current().executeScript("PF('userDialog').hide()");
                PrimeFaces.current().ajax().update("dt-users", "globalMessages");
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not delete user: " + e.getMessage()));
        }
    }

    // Getters and Setters
    public List<User> getUsers() { return users; }
    public User getSelectedUser() { return selectedUser; }
    public void setSelectedUser(User selectedUser) { this.selectedUser = selectedUser; }

    // Helpers for dropdowns in UI
    public UserRole[] getRoles() { return UserRole.values(); }
    public AccountStatus[] getStatuses() { return AccountStatus.values(); }
}
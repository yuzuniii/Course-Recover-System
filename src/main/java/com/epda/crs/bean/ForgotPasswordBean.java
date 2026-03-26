package com.epda.crs.bean;

import com.epda.crs.model.User;
import com.epda.crs.service.UserService;
import com.epda.crs.service.AuthService;
import com.epda.crs.util.EmailUtil;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class ForgotPasswordBean implements Serializable {

    @EJB
    private UserService userService;

    private String email;

    public void resetPassword() {
        FacesContext context = FacesContext.getCurrentInstance();
        
        try {
            // Find user by email
            Optional<User> optionalUser = userService.getUsers().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();

            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                
                // Generate a temporary 8-character password
                String tempPassword = UUID.randomUUID().toString().substring(0, 8);
                
                // Hash the new password and save it
                user.setPassword(AuthService.hashPassword(tempPassword));
                userService.saveUser(user, "SYSTEM_RESET");

                // Send Email Notification
                String subject = "CRS Password Reset";
                String body = "Hello " + user.getFullName() + ",\n\n"
                            + "Your password has been reset. Your temporary password is: " + tempPassword + "\n\n"
                            + "Please log in and change your password immediately.";
                
                EmailUtil.sendEmail(user.getEmail(), subject, body);

                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "A temporary password has been sent to your email."));
            } else {
                // Security best practice: Don't reveal if email exists or not, just show generic success
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "If the email exists, a reset link has been sent."));
            }
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to process password reset. Please try again later."));
        }
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

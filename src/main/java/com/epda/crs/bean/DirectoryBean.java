package com.epda.crs.bean;

import com.epda.crs.enums.UserRole;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@ViewScoped
public class DirectoryBean implements Serializable {

    private String viewType; // "USERS" or "STUDENTS"

    @Inject
    private LoginBean loginBean;

    @PostConstruct
    public void init() {
        // Strict RBAC: Officers always default to STUDENTS and cannot see USERS
        if (loginBean.getCurrentUser() != null && loginBean.getCurrentUser().getRole() != UserRole.COURSE_ADMINISTRATOR) {
            viewType = "STUDENTS";
        } else {
            // Admins default to STUDENTS but can toggle
            viewType = "STUDENTS";
        }
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        // Server-side RBAC enforcement
        if ("USERS".equals(viewType) && (loginBean.getCurrentUser() == null || loginBean.getCurrentUser().getRole() != UserRole.COURSE_ADMINISTRATOR)) {
            this.viewType = "STUDENTS";
        } else {
            this.viewType = viewType;
        }
    }
}

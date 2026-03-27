package com.epda.crs.filter;

import com.epda.crs.model.User;
import com.epda.crs.enums.UserRole;
import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebFilter("/pages/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        // 1. Skip filter for login page and resources
        if (path.endsWith("/login.xhtml") || path.contains("/jakarta.faces.resource/")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Check session
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            System.out.println("[AuthFilter] BLOCKED (No Session): " + path);
            res.sendRedirect(req.getContextPath() + "/pages/login.xhtml");
            return;
        }

        // 3. Enforce RBAC safely (Prevent NullPointerException if Role is missing)
        if (path.endsWith("/directory.xhtml") || path.endsWith("/audit-log.xhtml") || path.endsWith("/manage-courses.xhtml")) {
            if (path.endsWith("/directory.xhtml") && currentUser.getRole() == UserRole.ACADEMIC_OFFICER) {
                chain.doFilter(request, response);
                return;
            }

            if (currentUser.getRole() == null || currentUser.getRole() != UserRole.COURSE_ADMINISTRATOR) {
                System.out.println("[AuthFilter] RBAC BLOCKED: Non-admin tried to access " + path);
                res.sendRedirect(req.getContextPath() + "/pages/dashboard.xhtml");
                return;
            }
        }

        // Authorized -> proceed
        chain.doFilter(request, response);
    }
}

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
        HttpSession session = req.getSession(false);
        
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String requestURI = req.getRequestURI();

        if (currentUser == null) {
            // Not logged in, redirect to login page
            res.sendRedirect(req.getContextPath() + "/pages/login.xhtml");
            return;
        }

        // Role-Based Access Control enforcement
        if (requestURI.contains("/users.xhtml") && currentUser.getRole() != UserRole.COURSE_ADMINISTRATOR) {
            // Academic Officers should not manage users
            res.sendRedirect(req.getContextPath() + "/pages/dashboard.xhtml");
            return;
        }

        chain.doFilter(request, response);
    }
}
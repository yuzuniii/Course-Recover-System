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

        // 1. Identify if the user is trying to access a public page
        boolean isLoginPage = requestURI.contains("/pages/login.xhtml");
        boolean isForgotPasswordPage = requestURI.contains("/pages/forgot-password.xhtml");

        // 2. If not logged in AND not on a public page, redirect to login
        if (currentUser == null && !isLoginPage && !isForgotPasswordPage) {
            res.sendRedirect(req.getContextPath() + "/pages/login.xhtml");
            return;
        }

        // 3. (Optional but recommended) If already logged in, don't let them sit on the login page
        if (currentUser != null && (isLoginPage || isForgotPasswordPage)) {
            res.sendRedirect(req.getContextPath() + "/pages/dashboard.xhtml");
            return;
        }

        // 4. Role-Based Access Control enforcement (only applies if logged in)
        if (currentUser != null && requestURI.contains("/users.xhtml") && currentUser.getRole() != UserRole.COURSE_ADMINISTRATOR) {
            res.sendRedirect(req.getContextPath() + "/pages/dashboard.xhtml");
            return;
        }

        // 5. Allow the request to proceed
        chain.doFilter(request, response);
    }
}
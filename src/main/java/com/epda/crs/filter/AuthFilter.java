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
        
        // Force create a session if one doesn't exist
        HttpSession session = req.getSession(true); 
        User currentUser = (User) session.getAttribute("currentUser");

        // ==========================================
        // LOGIN BYPASS: Automatically inject an Admin
        // ==========================================
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setId(1L);
            currentUser.setUsername("admin");
            currentUser.setFullName("Auto Admin Bypass");
            currentUser.setRole(UserRole.COURSE_ADMINISTRATOR);
            
            session.setAttribute("currentUser", currentUser);
        }
        // ==========================================

        // Let every request go through without checking anything
        chain.doFilter(request, response);
    }
}
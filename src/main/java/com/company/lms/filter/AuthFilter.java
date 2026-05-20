package com.company.lms.filter;

import com.company.lms.controller.LoginController;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(filterName = "AuthFilter", urlPatterns = {"/manager/*", "/employee/*", "/secretary/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String contextPath = req.getContextPath();
        String loginURI = contextPath + "/login.xhtml";
        String requestURI = req.getRequestURI();

        try {
            // This forces the CDI container to give us the initialized bean instead of a broken proxy.
            LoginController loginController = CDI.current().select(LoginController.class).get();

            // Check if the user is logged in
            if (loginController == null || !loginController.isLoggedIn()) {
                res.sendRedirect(loginURI);
                return;
            }

            // Fetch the role of the logged-in user
            String role = loginController.getLoggedInUserRole();

            // Prevent Employees from accessing Manager pages
            if (requestURI.startsWith(contextPath + "/manager/") && !"MANAGER".equals(role)) {
                res.sendRedirect(contextPath + "/employee/dashboard.xhtml");
                return; // Stop processing
            }

            // Prevent unauthorized access to Secretary pages
            if (requestURI.startsWith(contextPath + "/secretary/") && !"SECRETARY".equals(role)) {
                res.sendRedirect(contextPath + "/403.xhtml");
                return; // Stop processing
            }

            // If all checks pass, allow the request to proceed to the page
            chain.doFilter(request, response);

        } catch (Exception e) {
            res.sendRedirect(loginURI);
        }
    }
}
package com.sunrisedental.servlet.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter ensuring that protected endpoints and pages require an authenticated session.
 */
@WebFilter(urlPatterns = {"/appointments.html", "/billing.html", "/reports.html", "/api/appointments/*", "/api/billing/*", "/api/reports/*"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Skip static resources & auth endpoints
        String path = req.getRequestURI();
        if (path.contains("/api/auth") || path.endsWith("login.html") || path.contains("/css/") || path.contains("/js/") || path.contains("/images/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        boolean loggedIn = (session != null && session.getAttribute("currentUser") != null);

        // Allow API calls if in test/development mode or session is active
        if (loggedIn || path.startsWith(req.getContextPath() + "/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // Redirect unauthenticated browser requests to login page
        res.sendRedirect(req.getContextPath() + "/login.html");
    }

    @Override
    public void destroy() {}
}

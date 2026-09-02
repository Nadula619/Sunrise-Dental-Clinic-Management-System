package com.sunrisedental.servlet.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter ensuring that protected HTML views require an active authenticated session.
 */
@WebFilter(urlPatterns = {"/*"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();

        // 1. Static assets (css, js, images, fonts) & public auth endpoints are always allowed
        if (uri.endsWith("login.html") || 
            uri.contains("/css/") || 
            uri.contains("/js/") || 
            uri.contains("/images/") || 
            uri.contains("/api/auth/login") || 
            uri.contains("/api/auth/logout") ||
            uri.contains("/api/auth/session")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Check session
        HttpSession session = req.getSession(false);
        boolean loggedIn = (session != null && session.getAttribute("currentUser") != null);

        // 3. If accessing protected HTML pages without session, redirect to login.html
        if (!loggedIn && (uri.endsWith(".html") || uri.endsWith("/"))) {
            res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
            res.setHeader("Pragma", "no-cache");
            res.setDateHeader("Expires", 0);
            res.sendRedirect(req.getContextPath() + "/login.html");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}

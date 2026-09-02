package com.sunrisedental.servlet;

import com.sunrisedental.model.User;
import com.sunrisedental.service.AuthService;
import com.sunrisedental.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Web Service Servlet for Staff Authentication and Session Management.
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/*"})
public class AuthServlet extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        this.authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || "/session".equals(pathInfo)) {
            HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute("currentUser") != null) {
                User user = (User) session.getAttribute("currentUser");
                Map<String, Object> userData = new HashMap<>();
                userData.put("username", user.getUsername());
                userData.put("fullName", user.getFullName());
                userData.put("role", user.getRole());
                userData.put("email", user.getEmail());
                JsonUtil.sendSuccess(resp, "Active session found", userData);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "No active user session");
            }
        } else if ("/staff".equals(pathInfo)) {
            JsonUtil.sendSuccess(resp, "Staff list retrieved", authService.getAllStaff());
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if ("/login".equals(pathInfo)) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            Map<?, ?> loginData = JsonUtil.fromJson(sb.toString(), Map.class);
            if (loginData == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid login payload");
                return;
            }

            String username = (String) loginData.get("username");
            String password = (String) loginData.get("password");

            User user = authService.authenticate(username, password);
            if (user != null) {
                HttpSession session = req.getSession(true);
                session.setAttribute("currentUser", user);
                session.setMaxInactiveInterval(30 * 60); // 30 minutes timeout

                Map<String, Object> respData = new HashMap<>();
                respData.put("username", user.getUsername());
                respData.put("fullName", user.getFullName());
                respData.put("role", user.getRole());
                respData.put("email", user.getEmail());

                JsonUtil.sendSuccess(resp, "Login successful. Welcome " + user.getFullName(), respData);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
            }
        } else if ("/logout".equals(pathInfo)) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            JsonUtil.sendSuccess(resp, "Logged out successfully", null);
        } else if ("/register".equals(pathInfo)) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            Map<?, ?> data = JsonUtil.fromJson(sb.toString(), Map.class);
            if (data == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid registration payload");
                return;
            }

            String username = (String) data.get("username");
            String password = (String) data.get("password");
            String fullName = (String) data.get("fullName");
            String role = (String) data.get("role");
            String email = (String) data.get("email");

            boolean success = authService.registerStaff(username, password, fullName, role, email);
            if (success) {
                JsonUtil.sendSuccess(resp, "Staff account created successfully", null);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Username already exists or invalid data");
            }
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }
}

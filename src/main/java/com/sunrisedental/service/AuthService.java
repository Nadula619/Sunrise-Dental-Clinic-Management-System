package com.sunrisedental.service;

import com.sunrisedental.dao.IUserDAO;
import com.sunrisedental.dao.UserDAOImpl;
import com.sunrisedental.model.User;
import com.sunrisedental.util.PasswordUtil;

import java.util.List;

/**
 * Service handling staff authentication, role authorization, and account security.
 */
public class AuthService {

    private final IUserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAOImpl();
        initDefaultAdmin();
    }

    public AuthService(IUserDAO userDAO) {
        this.userDAO = userDAO;
        initDefaultAdmin();
    }

    private void initDefaultAdmin() {
        if (userDAO.findByUsername("admin") == null) {
            User admin = new User(
                    "admin",
                    PasswordUtil.hashPassword("admin123"),
                    "System Administrator",
                    "ADMIN",
                    "admin@sunrisedental.lk"
            );
            userDAO.createUser(admin);
        }
        if (userDAO.findByUsername("receptionist") == null) {
            User receptionist = new User(
                    "receptionist",
                    PasswordUtil.hashPassword("rec123"),
                    "Anoma Fernando",
                    "RECEPTIONIST",
                    "reception@sunrisedental.lk"
            );
            userDAO.createUser(receptionist);
        }
    }

    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            return null;
        }

        if (PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public boolean registerStaff(String username, String password, String fullName, String role, String email) {
        if (username == null || password == null || userDAO.findByUsername(username.trim()) != null) {
            return false;
        }
        String hash = PasswordUtil.hashPassword(password);
        User user = new User(username.trim(), hash, fullName, role != null ? role.toUpperCase() : "RECEPTIONIST", email);
        return userDAO.createUser(user);
    }

    public List<User> getAllStaff() {
        return userDAO.findAllUsers();
    }
}

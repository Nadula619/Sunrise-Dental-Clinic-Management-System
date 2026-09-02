package com.sunrisedental.service;

import com.sunrisedental.dao.IUserDAO;
import com.sunrisedental.model.User;
import com.sunrisedental.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TDD Test Suite for AuthService (Staff Authentication and BCrypt Verification).
 */
public class AuthServiceTest {

    private IUserDAO mockUserDAO;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockUserDAO = Mockito.mock(IUserDAO.class);
        User testAdmin = new User(
                "admin",
                PasswordUtil.hashPassword("admin123"),
                "Administrator",
                "ADMIN",
                "admin@sunrisedental.lk"
        );
        when(mockUserDAO.findByUsername("admin")).thenReturn(testAdmin);
        authService = new AuthService(mockUserDAO);
    }

    @Test
    @DisplayName("Should authenticate successfully with correct credentials")
    void testAuthenticationSuccess() {
        User user = authService.authenticate("admin", "admin123");
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
        assertEquals("ADMIN", user.getRole());
    }

    @Test
    @DisplayName("Should fail authentication with incorrect password")
    void testAuthenticationFailureWrongPassword() {
        User user = authService.authenticate("admin", "wrongpassword");
        assertNull(user, "User should be null when password is incorrect");
    }

    @Test
    @DisplayName("Should fail authentication for non-existent user")
    void testAuthenticationNonExistentUser() {
        when(mockUserDAO.findByUsername("nonexistent")).thenReturn(null);
        User user = authService.authenticate("nonexistent", "secret");
        assertNull(user);
    }

    @Test
    @DisplayName("Should register new staff member with hashed password")
    void testRegisterStaff() {
        when(mockUserDAO.findByUsername("newuser")).thenReturn(null);
        when(mockUserDAO.createUser(any(User.class))).thenReturn(true);

        boolean registered = authService.registerStaff("newuser", "pass123", "New Doctor", "DENTIST", "doc@sunrisedental.lk");
        assertTrue(registered);
    }
}

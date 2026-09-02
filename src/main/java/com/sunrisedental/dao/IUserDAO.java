package com.sunrisedental.dao;

import com.sunrisedental.model.User;
import java.util.List;

/**
 * Data Access Object Interface for Staff Users.
 */
public interface IUserDAO {
    User findByUsername(String username);
    User findById(String id);
    boolean createUser(User user);
    List<User> findAllUsers();
    boolean updateUser(User user);
    boolean deleteUser(String id);
}

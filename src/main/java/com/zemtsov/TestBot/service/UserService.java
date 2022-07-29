package com.zemtsov.TestBot.service;

import com.zemtsov.TestBot.models.User;

import java.util.List;
import java.util.Optional;


public interface UserService {

    List<User> getAllUsers();
    Optional<User> findUserById(Long id);
    User saveUser(User user);
    void updateUser(Long id, String email);

}

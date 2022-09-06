package com.zemtsov.TestBot.service;

import com.zemtsov.TestBot.models.User;
import com.zemtsov.TestBot.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository repository;

    public UserServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<User> getAllUsers() {
        return repository.findAll();
    }

    @Override
    public Optional<User> findUserById(Long id) {
        return repository.findById(id);
    }

    @Override
    public User saveUser(User user) {
        return repository.save(user);
    }

    @Override
    @Transactional
    public void updateUser(Long id, String email, String gender) {
        User user = repository.findById(id).orElseThrow(() -> new IllegalStateException("User with id = " + id + " doesn't exist"));

        if (email != null && !email.isEmpty()) {
            user.setEmail(email);
        }

        if (gender != null && !gender.isEmpty()) {
            user.setGender(gender);
        }
    }
}

package com.zemtsov.TestBot.repositories;

import com.zemtsov.TestBot.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

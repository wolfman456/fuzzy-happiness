package com.gamer.fowever.tabletopservice.repository;

import com.gamer.fowever.tabletopservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailKey(String emailKey);

    Optional<User> findByUsernameIgnoreCaseOrEmailKey(String username, String emailKey);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailKey(String emailKey);
}
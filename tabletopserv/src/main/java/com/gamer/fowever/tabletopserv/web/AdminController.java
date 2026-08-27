package com.gamer.fowever.tabletopserv.web;

import com.gamer.fowever.tabletopserv.dto.UserSummary;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<UserSummary> users() {
        return userRepository.findAll().stream().map(UserSummary::from).toList();
    }
}
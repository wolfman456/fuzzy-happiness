package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopapi.AdminApi;
import com.gamer.fowever.tabletopapi.dto.UserSummary;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
import com.gamer.fowever.tabletopservice.support.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminControllerImpl implements AdminApi {

    private final UserRepository userRepository;

    public AdminControllerImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    @Override
    public List<UserSummary> users() {
        return userRepository.findAll().stream().map(Dtos::userSummary).toList();
    }
}
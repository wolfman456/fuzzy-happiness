package com.gamer.fowever.tabletopapi;

import com.gamer.fowever.tabletopapi.dto.UserSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/api/admin")
public interface AdminApi {

    @GetMapping("/users")
    List<UserSummary> users();
}
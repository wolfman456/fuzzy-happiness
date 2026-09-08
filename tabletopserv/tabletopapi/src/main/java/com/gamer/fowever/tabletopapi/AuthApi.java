package com.gamer.fowever.tabletopapi;

import com.gamer.fowever.tabletopapi.dto.AuthResponse;
import com.gamer.fowever.tabletopapi.dto.LoginRequest;
import com.gamer.fowever.tabletopapi.dto.RegisterRequest;
import com.gamer.fowever.tabletopapi.dto.RegisterResponse;
import com.gamer.fowever.tabletopapi.dto.ResendRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/auth")
public interface AuthApi {

    @PostMapping("/register")
    ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request);

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request);

    @GetMapping("/verify")
    ResponseEntity<String> verify(@RequestParam("token") String token);

    @PostMapping("/resend-verification")
    ResponseEntity<String> resendVerification(@Valid @RequestBody ResendRequest request);
}
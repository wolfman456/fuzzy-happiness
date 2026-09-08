package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopapi.AuthApi;
import com.gamer.fowever.tabletopapi.dto.AuthResponse;
import com.gamer.fowever.tabletopapi.dto.LoginRequest;
import com.gamer.fowever.tabletopapi.dto.RegisterRequest;
import com.gamer.fowever.tabletopapi.dto.RegisterResponse;
import com.gamer.fowever.tabletopapi.dto.ResendRequest;
import com.gamer.fowever.tabletopservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthControllerImpl implements AuthApi {

    private final AuthService authService;

    public AuthControllerImpl(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified. You can now log in.");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@Valid @RequestBody ResendRequest request) {
        authService.resendVerification(request.identifier());
        return ResponseEntity.accepted().body("If the account exists and is unverified, a new link has been sent.");
    }
}
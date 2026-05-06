package com.financetracker.controller;

import com.financetracker.dto.LoginRequest;
import com.financetracker.dto.LoginResponse;
import com.financetracker.dto.auth.LogoutRequest;
import com.financetracker.dto.auth.RefreshTokenRequest;
import com.financetracker.dto.auth.TokenResponse;
import com.financetracker.entity.User;
import com.financetracker.service.JwtService;
import com.financetracker.service.UserService;
import com.financetracker.service.api.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/signin")
    public ResponseEntity<TokenResponse> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        String deviceInfo = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();

        TokenResponse tokenResponse = authenticationService.authenticate(
                loginRequest.getUsername(),
                loginRequest.getPassword(),
                deviceInfo,
                ipAddress
        );

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            TokenResponse tokenResponse = authenticationService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(tokenResponse);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) LogoutRequest logoutRequest,
                                    Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;

        authenticationService.logout(
                logoutRequest != null ? logoutRequest.getRefreshToken() : null,
                logoutRequest != null && logoutRequest.isLogoutAllDevices(),
                username
        );

        return ResponseEntity.ok().body("Logged out successfully");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        User createdUser = userService.registerUser(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> getCurrentUser(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        List<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        LoginResponse response = new LoginResponse(
                null, // Не возвращаем токен здесь
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles
        );

        return ResponseEntity.ok(response);
    }
}
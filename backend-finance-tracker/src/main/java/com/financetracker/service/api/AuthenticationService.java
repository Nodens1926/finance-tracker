package com.financetracker.service.api;

import com.financetracker.dto.auth.TokenResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationService {
    TokenResponse authenticate(String username, String password, String deviceInfo, String ipAddress);
    TokenResponse refreshToken(String refreshToken);
    void logout(String refreshToken, boolean logoutAllDevices, String username);
}
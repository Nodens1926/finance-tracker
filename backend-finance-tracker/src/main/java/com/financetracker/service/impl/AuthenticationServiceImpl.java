package com.financetracker.service.impl;

import com.financetracker.dto.auth.TokenResponse;
import com.financetracker.entity.RefreshToken;
import com.financetracker.entity.User;
import com.financetracker.service.JwtService;
import com.financetracker.service.RefreshTokenService;
import com.financetracker.service.UserService;
import com.financetracker.service.api.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public TokenResponse authenticate(String username, String password, String deviceInfo, String ipAddress) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(username);

        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(username, deviceInfo, ipAddress);

        return new TokenResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtService.getAccessExpirationMs() / 1000,
                jwtService.getRefreshExpirationMs() / 1000
        );
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(String refreshToken) {
        // Проверка старого refresh token
        RefreshToken storedToken = refreshTokenService.verifyRefreshToken(refreshToken);
        User user = storedToken.getUser();

        // [ИЗМЕНЕНИЕ] Отзываем старый токен (ротация)
        refreshTokenService.revokeToken(refreshToken);

        // [ИЗМЕНЕНИЕ] Создаем НОВЫЙ refresh token
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
                user.getUsername(),
                storedToken.getDeviceInfo(),
                storedToken.getIpAddress()
        );

        UserDetails userDetails = userService.loadUserByUsername(user.getUsername());
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        // [ИЗМЕНЕНИЕ] Возвращаем НОВЫЙ refresh token
        return new TokenResponse(
                newAccessToken,
                newRefreshToken.getToken(),
                "Bearer",
                jwtService.getAccessExpirationMs() / 1000,
                jwtService.getRefreshExpirationMs() / 1000
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken, boolean logoutAllDevices, String username) {
        if (logoutAllDevices) {
            refreshTokenService.revokeAllUserTokens(username);
        } else if (refreshToken != null) {
            refreshTokenService.revokeToken(refreshToken);
        }
    }
}
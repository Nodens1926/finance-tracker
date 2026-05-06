package com.financetracker.service;

import com.financetracker.entity.RefreshToken;
import com.financetracker.entity.User;
import com.financetracker.repository.RefreshTokenRepository;
import com.financetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpirationMs;

    // [ИЗМЕНЕНИЕ] Кэш использованных токенов для защиты от повторного использования
    private final Map<String, Instant> usedTokens = new ConcurrentHashMap<>();

    @Transactional
    public RefreshToken createRefreshToken(String username, String deviceInfo, String ipAddress) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshTokenRepository.findByUser(user).stream()
                .filter(token -> deviceInfo != null && deviceInfo.equals(token.getDeviceInfo()) && !token.isRevoked())
                .forEach(token -> token.setRevoked(true));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken verifyRefreshToken(String token) {
        log.info("Verifying refresh token: {}", token);

        // [ИЗМЕНЕНИЕ] Проверка на повторное использование токена
        if (usedTokens.containsKey(token)) {
            log.warn("Token reuse detected: {}", token);
            RefreshToken compromisedToken = refreshTokenRepository.findByToken(token).orElse(null);
            if (compromisedToken != null) {
                // Отзываем ВСЕ токены пользователя при компрометации
                revokeAllUserTokens(compromisedToken.getUser().getUsername());
                throw new RuntimeException("Security alert: Token reuse detected. All sessions have been revoked.");
            }
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.error("Refresh token not found in database: {}", token);
                    return new RuntimeException("Refresh token not found");
                });

        // [ИЗМЕНЕНИЕ] Помечаем токен как использованный
        usedTokens.put(token, Instant.now());

        log.info("Found refresh token for user: {}, revoked: {}, expiry: {}",
                refreshToken.getUser().getUsername(),
                refreshToken.isRevoked(),
                refreshToken.getExpiryDate());

        if (refreshToken.isRevoked()) {
            log.error("Refresh token was revoked: {}", token);
            throw new RuntimeException("Refresh token was revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            log.error("Refresh token expired: {}", token);
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // [ИЗМЕНЕНИЕ] Удаляем из кэша использованных токенов при отзыве
        usedTokens.remove(token);
    }

    @Transactional
    public void revokeAllUserTokens(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        refreshTokenRepository.revokeAllUserTokens(user);

        // [ИЗМЕНЕНИЕ] Очищаем кэш для всех токенов пользователя
        refreshTokenRepository.findByUser(user).forEach(token ->
                usedTokens.remove(token.getToken())
        );
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());

        // [ИЗМЕНЕНИЕ] Очищаем старые записи из кэша (старше 1 дня)
        Instant oneDayAgo = Instant.now().minusSeconds(86400);
        usedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(oneDayAgo));

        log.info("Cleaned up expired refresh tokens");
    }
}
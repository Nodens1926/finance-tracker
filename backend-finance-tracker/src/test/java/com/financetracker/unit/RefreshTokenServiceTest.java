package com.financetracker.unit;

import com.financetracker.entity.RefreshToken;
import com.financetracker.entity.User;
import com.financetracker.repository.RefreshTokenRepository;
import com.financetracker.repository.UserRepository;
import com.financetracker.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setToken("test-token-123");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiryDate(Instant.now().plusSeconds(3600));
        testRefreshToken.setRevoked(false);
    }

    @Test
    void createRefreshToken_shouldCreateAndSave() {
        // given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(List.of());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // when
        RefreshToken result = refreshTokenService.createRefreshToken("testuser", "Chrome", "127.0.0.1");

        // then
        assertNotNull(result);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_shouldRevokeExistingDeviceTokens() {
        // given
        RefreshToken existingToken = new RefreshToken();
        existingToken.setRevoked(false);
        existingToken.setDeviceInfo("Chrome");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(List.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // when
        refreshTokenService.createRefreshToken("testuser", "Chrome", "127.0.0.1");

        // then
        assertTrue(existingToken.isRevoked());
    }

    @Test
    void verifyRefreshToken_withValidToken_shouldReturnToken() {
        // given
        when(refreshTokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testRefreshToken));

        // when
        RefreshToken result = refreshTokenService.verifyRefreshToken("test-token-123");

        // then
        assertNotNull(result);
        assertEquals("test-token-123", result.getToken());
    }

    @Test
    void verifyRefreshToken_withRevokedToken_shouldThrowException() {
        // given
        testRefreshToken.setRevoked(true);
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(testRefreshToken));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> refreshTokenService.verifyRefreshToken("revoked-token"));
        assertEquals("Refresh token was revoked", exception.getMessage());
    }

    @Test
    void verifyRefreshToken_withExpiredToken_shouldThrowException() {
        // given
        testRefreshToken.setExpiryDate(Instant.now().minusSeconds(3600));
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(testRefreshToken));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> refreshTokenService.verifyRefreshToken("expired-token"));
        assertEquals("Refresh token expired", exception.getMessage());
        verify(refreshTokenRepository).delete(testRefreshToken);
    }

    @Test
    void revokeToken_shouldSetRevokedTrue() {
        // given
        when(refreshTokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // when
        refreshTokenService.revokeToken("test-token-123");

        // then
        assertTrue(testRefreshToken.isRevoked());
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    void revokeAllUserTokens_shouldRevokeAll() {
        // given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // when
        refreshTokenService.revokeAllUserTokens("testuser");

        // then
        verify(refreshTokenRepository).revokeAllUserTokens(testUser);
    }
}
package com.financetracker.unit;

import com.financetracker.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Устанавливаем значения через reflection (как @Value)
        ReflectionTestUtils.setField(jwtService, "secret", "mySuperSecretKeyForJwtThatIsLongEnough1234567890");
        ReflectionTestUtils.setField(jwtService, "accessExpirationMs", 900000L); // 15 минут
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604800000L); // 7 дней

        userDetails = new User("testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void generateAccessToken_shouldReturnValidToken() {
        // when
        String token = jwtService.generateAccessToken(userDetails);

        // then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateRefreshToken_shouldReturnValidToken() {
        // when
        String token = jwtService.generateRefreshToken(userDetails);

        // then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        // given
        String token = jwtService.generateAccessToken(userDetails);

        // when
        String username = jwtService.extractUsername(token);

        // then
        assertEquals("testuser", username);
    }

    @Test
    void validateToken_withValidToken_shouldReturnTrue() {
        // given
        String token = jwtService.generateAccessToken(userDetails);

        // when
        Boolean isValid = jwtService.validateToken(token, userDetails);

        // then
        assertTrue(isValid);
    }

    @Test
    void validateToken_withWrongUsername_shouldReturnFalse() {
        // given
        String token = jwtService.generateAccessToken(userDetails);
        UserDetails wrongUser = new User("wronguser", "password", List.of());

        // when
        Boolean isValid = jwtService.validateToken(token, wrongUser);

        // then
        assertFalse(isValid);
    }

    @Test
    void validateRefreshToken_withValidToken_shouldReturnTrue() {
        // given
        String token = jwtService.generateRefreshToken(userDetails);

        // when
        Boolean isValid = jwtService.validateRefreshToken(token, userDetails);

        // then
        assertTrue(isValid);
    }

    @Test
    void validateRefreshToken_withAccessToken_shouldReturnFalse() {
        // given
        String accessToken = jwtService.generateAccessToken(userDetails);

        // when
        Boolean isValid = jwtService.validateRefreshToken(accessToken, userDetails);

        // then
        assertFalse(isValid); // type mismatch: access vs refresh
    }

    @Test
    void getAccessExpirationMs_shouldReturnConfiguredValue() {
        // when
        Long expiration = jwtService.getAccessExpirationMs();

        // then
        assertEquals(900000L, expiration);
    }

    @Test
    void getRefreshExpirationMs_shouldReturnConfiguredValue() {
        // when
        Long expiration = jwtService.getRefreshExpirationMs();

        // then
        assertEquals(604800000L, expiration);
    }
}
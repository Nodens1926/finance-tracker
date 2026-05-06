package com.financetracker.unit;

import com.financetracker.client.ExchangeRateClient;
import com.financetracker.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exchangeRateService, "retryAttempts", 3);
    }

    @Test
    void convertAmount_sameCurrency_shouldReturnSameAmount() {
        // given
        BigDecimal amount = new BigDecimal("100.00");

        // when
        BigDecimal result = exchangeRateService.convertAmount(amount, "USD", "USD");

        // then
        assertEquals(amount, result);
        verify(exchangeRateClient, never()).getExchangeRate(any(), any());
    }

    @Test
    void convertAmount_withValidRate_shouldConvert() {
        // given
        BigDecimal amount = new BigDecimal("100.00");
        when(exchangeRateClient.getExchangeRate("USD", "EUR")).thenReturn(0.92);

        // when
        BigDecimal result = exchangeRateService.convertAmount(amount, "USD", "EUR");

        // then
        BigDecimal expected = new BigDecimal("92.00");
        assertEquals(expected, result);
        verify(exchangeRateClient).getExchangeRate("USD", "EUR");
    }

    @Test
    void convertAmount_whenApiFails_shouldUseFallback() {
        // given
        BigDecimal amount = new BigDecimal("100.00");
        when(exchangeRateClient.getExchangeRate("USD", "EUR")).thenThrow(new RuntimeException("API Error"));

        // when
        BigDecimal result = exchangeRateService.convertAmount(amount, "USD", "EUR");

        // then
        // Fallback: EUR/USD = 0.92 -> 100 * 0.92 = 92.00
        BigDecimal expected = new BigDecimal("92.00");
        assertEquals(expected, result);
    }

    @Test
    void convertAmount_withUnsupportedCurrency_shouldUseFallbackAndNotCrash() {
        // given
        BigDecimal amount = new BigDecimal("100.00");
        when(exchangeRateClient.getExchangeRate("XYZ", "USD")).thenThrow(new RuntimeException("Unknown currency"));

        // when
        // Fallback doesn't have XYZ, so returns original amount
        BigDecimal result = exchangeRateService.convertAmount(amount, "XYZ", "USD");

        // then
        assertNotNull(result);
    }

    @Test
    void convertAmount_shouldRoundToTwoDecimals() {
        // given
        BigDecimal amount = new BigDecimal("10.33");
        when(exchangeRateClient.getExchangeRate("USD", "EUR")).thenReturn(0.923456);

        // when
        BigDecimal result = exchangeRateService.convertAmount(amount, "USD", "EUR");

        // then
        // 10.33 * 0.923456 = 9.539... округляется до 9.54
        assertEquals(2, result.scale());
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void getSupportedCurrencies_shouldReturnFromClient() {
        // given
        var expectedRates = Map.of("USD", 1.0, "EUR", 0.92);
        when(exchangeRateClient.getSupportedCurrencies()).thenReturn(expectedRates);

        // when
        var result = exchangeRateService.getSupportedCurrencies();

        // then
        assertEquals(expectedRates, result);
    }

    @Test
    void getSupportedCurrencies_whenClientFails_shouldReturnFallback() {
        // given
        when(exchangeRateClient.getSupportedCurrencies()).thenThrow(new RuntimeException("API Error"));

        // when
        var result = exchangeRateService.getSupportedCurrencies();

        // then
        assertNotNull(result);
        assertTrue(result.containsKey("USD"));
        assertTrue(result.containsKey("EUR"));
        assertTrue(result.containsKey("RUB"));
    }
}
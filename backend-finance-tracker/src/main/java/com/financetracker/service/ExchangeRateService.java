package com.financetracker.service;

import com.financetracker.client.ExchangeRateClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final ExchangeRateClient exchangeRateClient;

    // Fallback курсы (на случай полной недоступности API)
    private static final Map<String, Double> FALLBACK_RATES = Map.of(
            "USD", 1.0,
            "EUR", 0.92,
            "GBP", 0.79,
            "RUB", 92.5
    );

    @Value("${exchange.api.retry-attempts:3}")
    private int retryAttempts;

    @Retryable(
            value = {RuntimeException.class},
            maxAttemptsExpression = "${exchange.api.retry-attempts:3}",
            backoff = @Backoff(delayExpression = "${exchange.api.retry-delay:1000}", multiplier = 2)
    )
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        try {
            double rate = exchangeRateClient.getExchangeRate(fromCurrency, toCurrency);
            BigDecimal converted = amount.multiply(BigDecimal.valueOf(rate))
                    .setScale(2, RoundingMode.HALF_UP);
            log.info("Converted {} {} to {} {} at rate {}", amount, fromCurrency, converted, toCurrency, rate);
            return converted;
        } catch (Exception e) {
            log.error("Conversion failed, using fallback rate for {}/{}", fromCurrency, toCurrency);
            return getFallbackConversion(amount, fromCurrency, toCurrency);
        }
    }

    private BigDecimal getFallbackConversion(BigDecimal amount, String fromCurrency, String toCurrency) {
        Double fromRate = FALLBACK_RATES.get(fromCurrency.toUpperCase());
        Double toRate = FALLBACK_RATES.get(toCurrency.toUpperCase());

        if (fromRate == null || toRate == null) {
            log.warn("No fallback rate for {}/{}", fromCurrency, toCurrency);
            return amount;
        }

        double conversionRate = toRate / fromRate;
        return amount.multiply(BigDecimal.valueOf(conversionRate))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, Double> getSupportedCurrencies() {
        try {
            return exchangeRateClient.getSupportedCurrencies();
        } catch (Exception e) {
            log.warn("Failed to fetch currencies, returning fallback list");
            return FALLBACK_RATES;
        }
    }
}
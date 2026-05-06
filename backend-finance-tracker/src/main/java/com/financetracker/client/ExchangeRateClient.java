package com.financetracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ExchangeRateClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // Кэш курсов валют на 10 минут
    private final Map<String, CachedRate> cache = new ConcurrentHashMap<>();

    @Value("${exchange.api.base-url}")
    private String baseUrl;

    @Value("${exchange.api.timeout}")
    private long timeout;

    public ExchangeRateClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.exchangerate.host")
                .build();
    }

    public record CachedRate(double rate, long timestamp) {}

    public double getExchangeRate(String fromCurrency, String toCurrency) {
        String cacheKey = fromCurrency + "_" + toCurrency;
        CachedRate cached = cache.get(cacheKey);

        // Кэш на 10 минут
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < 600000) {
            log.debug("Using cached rate for {}/{}: {}", fromCurrency, toCurrency, cached.rate);
            return cached.rate;
        }

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri("/convert?from={from}&to={to}&amount=1", fromCurrency, toCurrency)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                double rate = json.path("result").asDouble();

                if (rate > 0) {
                    cache.put(cacheKey, new CachedRate(rate, System.currentTimeMillis()));
                    log.info("Fetched rate {}/{} = {}", fromCurrency, toCurrency, rate);
                    return rate;
                }
            }
        } catch (RestClientException e) {
            log.error("Exchange rate API call failed: {}", e.getMessage());
            if (cached != null) {
                log.warn("Using stale cache for {}/{}", fromCurrency, toCurrency);
                return cached.rate;
            }
            throw new RuntimeException("Failed to fetch exchange rate: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching exchange rate", e);
        }

        throw new RuntimeException("Unable to get exchange rate for " + fromCurrency + "/" + toCurrency);
    }

    public Map<String, Double> getSupportedCurrencies() {
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri("/symbols")
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                JsonNode symbols = json.path("symbols");
                Map<String, Double> result = new HashMap<>();
                symbols.fields().forEachRemaining(entry -> result.put(entry.getKey(), 0.0));
                return result;
            }
        } catch (Exception e) {
            log.error("Failed to fetch supported currencies", e);
        }
        return Map.of("USD", 0.0, "EUR", 0.0, "GBP", 0.0, "RUB", 0.0);
    }
}
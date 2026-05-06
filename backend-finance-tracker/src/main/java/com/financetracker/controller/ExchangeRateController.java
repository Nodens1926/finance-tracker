package com.financetracker.controller;

import com.financetracker.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange-rate")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/convert")
    public ResponseEntity<Map<String, Object>> convert(
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "USD") String from,
            @RequestParam(defaultValue = "USD") String to) {

        BigDecimal converted = exchangeRateService.convertAmount(amount, from, to);

        Map<String, Object> response = new HashMap<>();
        response.put("originalAmount", amount);
        response.put("fromCurrency", from.toUpperCase());
        response.put("convertedAmount", converted);
        response.put("toCurrency", to.toUpperCase());
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/currencies")
    public ResponseEntity<Map<String, Double>> getCurrencies() {
        return ResponseEntity.ok(exchangeRateService.getSupportedCurrencies());
    }
}
package com.financetracker.service;

import com.financetracker.dto.seo.MetaTags;
import org.springframework.stereotype.Service;

@Service
public class SeoService {

    public MetaTags generateTransactionMeta(Long id, String category, String amount, String type) {
        return MetaTags.builder()
                .title(type + " Transaction: " + category + " - $" + amount + " | Finance Tracker")
                .description("View details of your " + category + " " + type.toLowerCase() + " transaction of $" + amount)
                .canonical("https://yourdomain.com/transactions/" + id)
                .ogTitle("Financial Transaction: " + category)
                .ogDescription(type + " of $" + amount + " in category " + category)
                .ogType("article")
                .build();
    }
}
package com.financetracker.dto.seo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetaTags {
    private String title;
    private String description;
    private String keywords;
    private String canonical;
    private String robots;

    // Open Graph
    private String ogTitle;
    private String ogDescription;
    private String ogImage;
    private String ogUrl;
    private String ogType;

    // Twitter Card
    private String twitterCard;
    private String twitterTitle;
    private String twitterDescription;
    private String twitterImage;

    // JSON-LD (передаётся отдельно)
    private String jsonLd;
}
package com.financetracker.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RobotsController {

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobotsTxt() {
        String robotsTxt = """
                # Allow all crawlers
                User-agent: *
                Allow: /
                
                # Sitemap location
                Sitemap: https://yourdomain.com/sitemap.xml
                
                # Disallow admin and API endpoints (keep internal)
                Disallow: /api/
                Disallow: /h2-console/
                Disallow: /actuator/
                
                # Disallow auth pages (noindex)
                Disallow: /login
                Disallow: /register
                Disallow: /logout
                
                # Crawl delay to be nice to the server
                Crawl-delay: 1
                """;
        return ResponseEntity.ok(robotsTxt);
    }
}
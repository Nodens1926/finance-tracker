package com.financetracker.service;

import com.financetracker.entity.Transaction;
import com.financetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SitemapService {

    private final TransactionRepository transactionRepository;

    private static final String BASE_URL = "https://finance-tracker.com";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String generateSitemap() {
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Статические страницы
        addUrl(sitemap, "/", "1.0", "daily", LocalDate.now());
        addUrl(sitemap, "/about", "0.8", "monthly", LocalDate.now());
        addUrl(sitemap, "/contact", "0.6", "monthly", LocalDate.now());
        addUrl(sitemap, "/privacy", "0.5", "yearly", LocalDate.now());
        addUrl(sitemap, "/terms", "0.5", "yearly", LocalDate.now());

        // Динамические страницы транзакций (только последние 100 для производительности)
        List<Transaction> recentTransactions = transactionRepository.findAll()
                .stream()
                .limit(100)
                .collect(Collectors.toList());

        for (Transaction transaction : recentTransactions) {
            String url = "/transactions/" + transaction.getId();
            LocalDate lastMod = transaction.getDate();
            addUrl(sitemap, url, "0.7", "weekly", lastMod);
        }

        sitemap.append("</urlset>");
        return sitemap.toString();
    }

    private void addUrl(StringBuilder sitemap, String path, String priority, String changefreq, LocalDate lastMod) {
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(BASE_URL).append(path).append("</loc>\n");
        sitemap.append("    <lastmod>").append(lastMod.format(DATE_FORMAT)).append("</lastmod>\n");
        sitemap.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        sitemap.append("    <priority>").append(priority).append("</priority>\n");
        sitemap.append("  </url>\n");
    }
}
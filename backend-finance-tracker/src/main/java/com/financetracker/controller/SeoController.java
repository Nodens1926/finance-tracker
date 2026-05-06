package com.financetracker.controller;

import com.financetracker.dto.seo.MetaTags;
import com.financetracker.service.SeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class SeoController {

    private final SeoService seoService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Финансовый Трекер - Управляйте своими финансами");
        model.addAttribute("description", "Отслеживайте доходы и расходы, анализируйте бюджет, достигайте финансовых целей. Бесплатный финансовый трекер.");
        model.addAttribute("keywords", "финансы, бюджет, трекер расходов, личные финансы, учет доходов");
        model.addAttribute("canonical", "https://yourdomain.com/");
        model.addAttribute("ogTitle", "Финансовый Трекер");
        model.addAttribute("ogDescription", "Управляйте своими финансами легко и удобно");
        return "home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "О проекте | Финансовый Трекер");
        model.addAttribute("description", "Мы помогаем людям контролировать финансы, экономить деньги и достигать целей.");
        model.addAttribute("canonical", "https://yourdomain.com/about");
        return "about";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("title", "Политика конфиденциальности | Финансовый Трекер");
        model.addAttribute("description", "Узнайте, как мы защищаем ваши персональные данные.");
        return "privacy";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("title", "Условия использования | Финансовый Трекер");
        model.addAttribute("description", "Ознакомьтесь с условиями использования сервиса.");
        return "terms";
    }
}
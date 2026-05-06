--ЗАПУСК БЕКА
mvn clean spring-boot:run

--ЗАПУСК ФРОНТА
cd ~/projects/finance-tracker/finance-tracker-frontend
npm run dev

ТЕХНИЧЕСКАЯ SEO ИНФРАСТРУКТУРА------------------------------------------------------------------------------------------

**1.1 Sitemap.xml (динамическая генерация)**
   SitemapController.java — эндпоинт /sitemap.xml
   SitemapService.java — генерация XML с приоритетами и датами

   Теория: Sitemap указывает поисковым роботам, какие страницы индексировать, с какой частотой и приоритетом. 
   Динамическая генерация важна для приложений с изменяющимся контентом (транзакции, пользователи).

**1.2 Robots.txt**
   RobotsController.java — эндпоинт /robots.txt

   Теория: Управляет поведением поисковых роботов. Закрытые маршруты (/api/**, /h2-console/**) исключаются из индексации. 
   Указывается ссылка на sitemap.

**1.3 HTTP статусы (уже были в проекте)**
   Анализ: GlobalExceptionHandler уже корректно обрабатывал

**1.4 Структурированные данные JSON-LD**
    JsonLdService.java — генерация Schema.org разметки для транзакций

   Теория: JSON-LD (JavaScript Object Notation for Linked Data) — формат для встраивания структурированных данных. 
   Поисковики используют их для создания Rich Snippets (звёздочки, цены, даты).

   Генерирует JSON блок в формате Schema.org который встраивается в HTML-траницу. Поисковики используют эти данные для 
   создания RichSnippers, расширенных результатов поиска с дополнительной информацией. Это повышает кликабельность ссылки 
   в выдаче.

SEO КОНТРОЛЛЕРЫ И МЕТА-ТЕГИ---------------------------------------------------------------------------------------------

   Это HTML-теги в разделе <head> которые описывают содержимое страницы для поисковиков и социальных сетей

**2.1 SeoController.java**
    Публичные страницы: /, /about, /privacy, /terms
    Динамическая страница транзакции: /transactions/{id}

**2.2 MetaTags DTO**

    dto/seo/MetaTags.java — содержит:

        Стандартные мета-теги (title, description, keywords)

        Canonical URL (предотвращает дубли)

        Open Graph (Facebook, LinkedIn)

        Twitter Card

   Теория:

       Canonical URL — указывает поисковику основную версию страницы, когда контент доступен по нескольким URL
   
       Open Graph — протокол для социальных сетей: заголовок, описание, изображение при шаринге
   
       Twitter Card — аналогично для Twitter

**2.3 SeoService.java**
   Генератор мета-тегов на основе данных транзакции

ИНТЕГРАЦИЯ СТОРОННЕГО API-----------------------------------------------------------------------------------------------

**3.1 Выбор API**
   Выбран: exchangerate.host — бесплатный, без API-ключа, стабильный

**3.2 Серверный слой интеграции**
   ExchangeRateClient.java	HTTP-клиент (RestClient) для внешнего API
   ExchangeRateService.java	Бизнес-логика с retry и fallback
   ExchangeRateController.java	REST эндпоинт /api/exchange-rate/*

**3.3 Обработка ошибок**
   Реализовано:
      java
      
      @Retryable(
      value = {RuntimeException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2)
   )
   
       Таймауты — через AbortController на клиенте (8 секунд)
   
       Повторные попытки — 3 попытки с экспоненциальной задержкой (1с, 2с, 4с)
   
       Кэширование — результаты кэшируются на 10 минут (ConcurrentHashMap)
   
       Graceful degradation — при недоступности API используются fallback-курсы
   
   Теория:
   
       Circuit Breaker (реализован через retry) — предотвращает каскадные отказы
   
       Fallback — резервное поведение при недоступности внешнего сервиса
   
       Timeout — защита от зависания при медленных ответах

**3.4 Безопасность: переменные окружения**
   Добавлено в application.properties:
      
      exchange.api.base-url=https://api.exchangerate.host
      exchange.api.timeout=5000
      exchange.api.retry-attempts=3
      exchange.api.retry-delay=1000
   
   Теория: Ключи доступа, URL и чувствительные параметры выносятся в окружение, чтобы не попадать в репозиторий 
   и позволять переопределение на разных средах (dev/stage/prod).

**3.5 Нормализация ответа**
   Клиент преобразует внешний JSON:
   {"result": 0.92}
   
   во внутренний формат:
   {
   "originalAmount": 100,
   "fromCurrency": "USD",
   "convertedAmount": 92.00,
   "toCurrency": "EUR",
   "success": true
   }

ИЗМЕНЕНИЯ В СУЩЕСТВУЮЩИХ ФАЙЛАХ-----------------------------------------------------------------------------------------
**4.1 SecurityConfig.java**
   Добавлены публичные маршруты для SEO:
   .requestMatchers("/", "/about", "/privacy", "/terms", "/contact").permitAll()
   .requestMatchers("/robots.txt", "/sitemap.xml").permitAll()
   .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

ФРОНТЕНД----------------------------------------------------------------------------------------------------------------
1. Установка и настройка SEO-инфраструктуры

   Установлен пакет react-helmet-async для управления мета-тегами
   Добавлен HelmetProvider в корневое приложение (main.tsx)
   Создан компонент HelmetSEO.tsx для динамической генерации мета-тегов

2. Реализация мета-тегов и Open Graph

   В index.html добавлены базовые мета-теги (description, keywords, robots, canonical)
   Добавлены Open Graph теги для Facebook и LinkedIn
   Добавлены Twitter Card теги
   На страницах Dashboard, Login, Register добавлены динамические мета-теги через HelmetSEO
   Для авторизованных страниц установлен noIndex для закрытых страниц

3. Canonical URL

   Добавлена поддержка канонических URL через компонент HelmetSEO
   Каждая страница может задавать свой canonical URL

4. JSON-LD структурированные данные

   Добавлен JSON-LD для Dashboard (тип WebApplication)
   Данные встраиваются через script type="application/ld+json"

5. Интеграция стороннего API (ExchangeRate)

   Создан компонент ExchangeRateWidget.tsx
   Реализована конвертация валют через бекенд-эндпоинт /api/exchange-rate/convert
   Добавлен фиксированный список из 9 валют

6. Обработка состояний и graceful degradation

   Реализовано состояние загрузки (isLoading) с анимацией
   Реализовано состояние ошибки (error) с выводом сообщения
   Реализован пустой результат (при отсутствии конвертации)
   При недоступности API используются fallback-курсы (офлайн-режим)
   Добавлен таймаут запроса 8 секунд через AbortController

7. Оптимизация производительности

   Добавлен loading="lazy" для изображений в Dashboard
   Пагинация и фильтрация уже были реализованы в проекте
   Использование useCallback для оптимизации ререндеров

Изменённые файлы

    src/main.tsx – обёртка в HelmetProvider
    src/App.tsx – добавлен HelmetProvider и SEO для маршрутов
    src/pages/Dashboard.tsx – добавлены HelmetSEO и виджет валют
    index.html – расширены мета-теги

Созданные файлы

    src/components/SEO/HelmetSEO.tsx
    src/components/Widgets/ExchangeRateWidget.tsx

Проверка результатов

    Мета-теги отображаются в HTML-коде страниц
    Конвертер валют работает с реальным API и fallback-режимом
    При недоступности API виджет продолжает работать через резервные курсы
    Существующий функционал (транзакции, авторизация, аналитика) не нарушен
# Production-Quality URL Shortener Service

This repository implements a production-grade URL Shortener Service using Java 17+, Spring Boot 3.x, and H2 database. It is designed for technical interview portfolios, showcasing clean architecture, test coverage, validation, exception handling, and custom business logic.

---

## FEATURES
- **URL Shortening**: Generates a 6-character Base62 code derived from the MD5 hash of the original URL and timestamp, with collision resolution.
- **Custom Aliases**: Allows users to specify a unique custom alias.
- **Redirection (HTTP 302)**: Instantly redirects users to the original URL while registering atomic click counts and analytics.
- **Expiry Features**: Supports link expiration in days. Trying to access an expired link returns HTTP 410 Gone.
- **Rate Limiting**: Built-in simple thread-safe sliding-window rate limiter per client IP (max 10 requests per minute) to prevent API abuse.
- **QR Code Generation**: Generates high-quality PNG QR codes using the ZXing library.
- **Click Analytics**: Tracks click timestamps in a separate table, allowing visualization of click distribution by the hour.
- **Interactive Documentation**: Swagger UI automatically exposed for sandbox testing.
- **Preloaded Sample Data**: Database is prepopulated with 5 distinct entries on startup (including active, expired, and highly clicked links).

---

## TECH STACK
- **Language**: Java 17
- **Framework**: Spring Boot 3.3.0
- **Database**: H2 In-Memory Database (JPA / Hibernate)
- **APIs & Docs**: OpenAPI 3 / Springdoc Swagger UI
- **Testing**: JUnit 5, Mockito, Spring Boot Test (MockMvc)
- **Utilities**: Lombok, ZXing (QR Code)

---

## PROJECT STRUCTURE

```text
C:/Users/syams_/.gemini/antigravity/scratch/url-shortener/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── urlshortener/
    │   │           ├── UrlShortenerApplication.java
    │   │           ├── config/
    │   │           │   ├── DataLoader.java
    │   │           │   ├── RateLimiterFilter.java
    │   │           │   └── SwaggerConfig.java
    │   │           ├── controller/
    │   │           │   ├── RedirectController.java
    │   │           │   └── UrlController.java
    │   │           ├── dto/
    │   │           │   ├── StatsResponse.java
    │   │           │   ├── UrlRequest.java
    │   │           │   └── UrlResponse.java
    │   │           ├── exception/
    │   │           │   ├── DuplicateAliasException.java
    │   │           │   ├── ErrorResponse.java
    │   │           │   ├── GlobalExceptionHandler.java
    │   │           │   ├── InvalidUrlException.java
    │   │           │   ├── UrlExpiredException.java
    │   │           │   └── UrlNotFoundException.java
    │   │           ├── model/
    │   │           │   ├── ClickAnalytic.java
    │   │           │   └── UrlMapping.java
    │   │           ├── repository/
    │   │           │   ├── ClickAnalyticRepository.java
    │   │           │   └── UrlMappingRepository.java
    │   │           └── service/
    │   │               ├── Base62Encoder.java
    │   │               ├── UrlService.java
    │   │               └── UrlServiceImpl.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/
            └── com/
                └── urlshortener/
                    ├── Base62EncoderTest.java
                    ├── UrlControllerTest.java
                    └── UrlServiceTest.java
```

---

## RUNNING THE APPLICATION

To compile and launch the application locally, run the following command in the project root directory:

```bash
mvn spring-boot:run
```

Once running, the server is available at `http://localhost:8080`.

To run the unit and integration test suites:

```bash
mvn test
```

---

## H2 DATABASE CONSOLE

The H2 database runs in-memory and can be inspected in real-time.
- **Console URL**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- **JDBC URL**: `jdbc:h2:mem:urlshortenerdb`
- **Username**: `sa`
- **Password**: *(leave blank)*

---

## SWAGGER UI

You can explore, test, and document all endpoints interactively through Swagger UI.
- **Swagger UI URL**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) (redirects to `/swagger-ui/index.html`)
- **API Docs Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## API ENDPOINTS

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **POST** | `/api/shorten` | Shorten a long URL (supports optional custom alias & expiration) |
| **GET** | `/{shortCode}` | Resolve short code and redirect to original URL (302 Redirection) |
| **GET** | `/api/info/{shortCode}` | Fetch full details and analytics for a short code |
| **GET** | `/api/urls` | Get a paginated, sorted list of all shortened URLs |
| **PUT** | `/api/urls/{shortCode}` | Update target original URL or extend link expiration |
| **DELETE** | `/api/urls/{shortCode}` | Delete a shortened URL mapping |
| **GET** | `/api/stats` | Retrieve service stats, most-clicked links, and hourly click history |
| **GET** | `/api/urls/{shortCode}/qrcode` | Generate and download a PNG QR code redirecting to the short URL |

---

## SAMPLE CURL COMMANDS

### 1. Shorten a URL (Auto-Generated Short Code)
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.google.com", "expiryDays": 10}'
```

### 2. Shorten a URL with Custom Alias
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://github.com", "customAlias": "myrepo"}'
```

### 3. Redirection (Resolve Short Code)
```bash
curl -i http://localhost:8080/myrepo
```
*(Notice the `HTTP/1.1 302 Found` and `Location: https://github.com` headers)*

### 4. Fetch Details of a Short Code
```bash
curl http://localhost:8080/api/info/myrepo
```

### 5. List All URLs (Paginated & Sorted)
```bash
curl "http://localhost:8080/api/urls?page=0&size=5&sort=clickCount,desc"
```

### 6. Update URL Details (Modify Original URL or Extend Expiry)
```bash
curl -X PUT http://localhost:8080/api/urls/myrepo \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://github.com/trending", "expiryDays": 30}'
```

### 7. Retrieve System Diagnostics & Hourly Click Distribution
```bash
curl http://localhost:8080/api/stats
```

### 8. Download QR Code image
```bash
curl -o qrcode.png http://localhost:8080/api/urls/myrepo/qrcode
```

### 9. Delete a URL Mapping
```bash
curl -X DELETE http://localhost:8080/api/urls/myrepo -i
```
*(Returns `HTTP/1.1 204 No Content`)*

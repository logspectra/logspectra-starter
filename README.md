# LogSpectra Spring Boot Starter

> Plug-and-play structured JSON logging for Spring Boot microservices.  
> Add the dependency → configure two lines → done.

---

## Features

| Feature | Detail |
|---|---|
| **Structured JSON logs** | Every log line is a valid JSON object (logstash-logback-encoder) |
| **Automatic context** | `service`, `endpoint`, `method`, `traceId` in every record |
| **Trace propagation** | `X-Trace-Id` header read/generated per request |
| **Kafka shipping** | Logs sent to Kafka via async, non-blocking appender |
| **MDC lifecycle** | MDC populated before request, cleared after — always |
| **Exception logging** | Global handler logs all uncaught exceptions with full context |
| **Zero user code** | No manual MDC, no manual Kafka config, no manual filter registration |
| **Override-friendly** | Every bean is `@ConditionalOnMissingBean` — user beans always win |

---

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>com.logspectra</groupId>
    <artifactId>logspectra-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure `application.yml`

```yaml
logspectra:
  service-name: user-service
  kafka:
    bootstrap-servers: localhost:9092
    topic: logs-topic
```

That's it. Every log statement your code (or Spring) makes will now be:
- Formatted as JSON
- Enriched with `service`, `endpoint`, `method`, `traceId`
- Sent to the Kafka topic

---

## Sample Log Output

```json
{
  "@timestamp": "2024-04-02T10:23:45.123Z",
  "@version": "1",
  "message": "Processing payment for order 98765",
  "logger_name": "com.example.PaymentService",
  "thread_name": "http-nio-8080-exec-3",
  "level": "INFO",
  "service": "payment-service",
  "endpoint": "/api/payments",
  "method": "POST",
  "traceId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

---

## Configuration Reference

```yaml
logspectra:
  enabled: true                          # false → disables the entire starter
  service-name: my-service               # required — injected as "service" field

  kafka:
    bootstrap-servers: localhost:9092    # required — broker list
    topic: application-logs             # required — target topic
    acks: "1"                            # 0 | 1 | all
    retries: 3                           # producer retry count
    max-block-ms: 2000                   # max ms to block on Kafka buffer full
```

---

## Trace ID Propagation

```
Client  ──── X-Trace-Id: abc-123 ────►  Service A (MDC: traceId=abc-123)
                                              │
                                              │── X-Trace-Id: abc-123 ──► Service B
```

- **Present** → reused as-is from the `X-Trace-Id` request header
- **Absent** → a new UUID is generated
- **Response** → always echoed back in the `X-Trace-Id` response header

---

## How It Works

```
Request arrives
     │
     ▼
LoggingFilter (HIGHEST_PRECEDENCE)
     ├── Read / generate traceId
     ├── MDC.put(service, endpoint, method, traceId)
     ▼
Your Controllers / Services
     ├── log.info("Processing...")  ──► JSON includes all MDC fields
     ▼
LoggingFilter finally block
     └── MDC.clear() ← always runs, even on exception
```

### Kafka Pipeline

```
Logger.info(...)
     │
     ▼
Logback root logger
     │
     ├──► ConsoleAppender (JSON to stdout)
     │
     └──► AsyncAppender (non-blocking queue: 512 events)
               │
               ▼
          KafkaAppender
               │
               ▼
     Kafka Topic: logs-topic
```

---

## Overriding Defaults

Every bean is protected with `@ConditionalOnMissingBean`.  
Define your own bean to override any part of the starter:

```java
@Bean
public LoggingFilter myCustomFilter(LogSpectraProperties props) {
    // Your implementation replaces the starter's
    return new MyEnhancedLoggingFilter(props);
}
```

---

## Disabling the Starter

```yaml
logspectra:
  enabled: false
```

All starter beans are skipped — no filter, no Kafka appender, no exception handler.

---

## Building & Installing

```bash
# Build and install to local Maven repository
mvn clean install

# Run tests only
mvn test

# Build without tests
mvn clean package -DskipTests
```

---

## Dependencies

| Library | Purpose |
|---|---|
| `spring-boot-starter` | Core Spring Boot |
| `spring-boot-starter-web` | Servlet filter support |
| `logstash-logback-encoder` | JSON log formatting |
| `logback-kafka-appender` | Kafka log shipping |
| `kafka-clients` | Kafka producer |

---

## Project Structure

```
logspectra-starter/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/logspectra/
    │   │   ├── autoconfigure/
    │   │   │   └── LogSpectraAutoConfiguration.java   ← Spring Boot entry point
    │   │   ├── config/
    │   │   │   ├── KafkaAppenderConfig.java            ← Logback Kafka wiring
    │   │   │   └── MdcKeys.java                        ← MDC field name constants
    │   │   ├── filter/
    │   │   │   └── LoggingFilter.java                  ← Per-request MDC population
    │   │   ├── properties/
    │   │   │   └── LogSpectraProperties.java            ← @ConfigurationProperties
    │   │   └── exception/
    │   │       └── GlobalExceptionLoggingHandler.java  ← Catch-all exception logger
    │   └── resources/
    │       ├── logback-spring.xml                       ← Default Logback config
    │       └── META-INF/spring/
    │           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/
        └── java/com/logspectra/
            ├── autoconfigure/LogSpectraAutoConfigurationTest.java
            ├── filter/LoggingFilterTest.java
            └── properties/LogSpectraPropertiesTest.java
```

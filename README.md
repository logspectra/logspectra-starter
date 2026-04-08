# LogSpectra Spring Boot Starter

Structured logging starter for Spring Boot microservices that enriches logs with request context and ships JSON logs to Kafka.

## What this starter provides

- Auto-configured request logging context (`traceId`, endpoint, method, thread, logger)
- Kafka log shipping with JSON payloads
- Sensible defaults with opt-in/out properties
- Spring Boot auto-configuration friendly overrides

## Compatibility

- Java: `21` (enforced by Maven Enforcer)
- Spring Boot BOM: `3.2.4`
- Packaging: Maven JAR starter

## Maven coordinates

```xml
<dependency>
  <groupId>com.github.logspectra</groupId>
  <artifactId>logspectra-starter</artifactId>
  <version>1.0.2</version>
</dependency>
```

## Minimal configuration

```yaml
logspectra:
  enabled: true
  service-name: my-service
  project-id: my-project
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092
    topic: application-logs
```

Notes:
- `logspectra.project-id` is required.
- `environment` is resolved from active Spring profiles; if none are active, `default` is used.

## Expected JSON fields

The starter is designed to emit these keys in Kafka JSON logs:

- `projectId`
- `timestamp`
- `level`
- `service`
- `traceId`
- `spanId` (when available)
- `thread`
- `logger`
- `message`
- `endpoint`
- `exception`
- `stackTrace`
- `host`
- `environment`

## Build and test

Prerequisite: Java 21 and Maven 3.9+.

PowerShell:

```powershell
mvn clean test
mvn clean package
mvn clean install
```

Bash/Zsh:

```bash
mvn clean test
mvn clean package
mvn clean install
```

## Local development

- Main code: `src/main/java/com/logspectra`
- Resources: `src/main/resources`
- Tests: `src/test/java/com/logspectra`

## Release artifacts

A successful package/install generates starter artifacts under `target/`, including:

- `logspectra-starter-<version>.jar`
- `logspectra-starter-<version>-sources.jar`
- `logspectra-starter-<version>-javadoc.jar`

## Contributing

Please read `CONTRIBUTING.md` before starting work.

Important: open an issue first and wait for maintainers to confirm scope before opening a pull request.

## License

- Full text: `LICENSE`
- Attribution notices: `NOTICE`
This project is licensed under the Apache License 2.0.

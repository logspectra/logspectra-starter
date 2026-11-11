package com.logspectra.registry;

import com.logspectra.properties.LogSpectraProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * On startup, POSTs this service's code-source config to logspectra-backend.
 * Backend stores it in Redis for code-aware analysis.
 */
public class ServiceCodeRegistryReporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ServiceCodeRegistryReporter.class);

    private final LogSpectraProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    public ServiceCodeRegistryReporter(LogSpectraProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled() || !properties.getRegistry().isEnabled()) {
            return;
        }
        if (!properties.getCodeSource().hasRegistrationPayload()) {
            log.info("[LogSpectra] Skipping code registry — set logspectra.code-source.git-url or local-path");
            return;
        }

        try {
            String json = buildJson();
            String url = properties.getRegistry().getBackendUrl() + "/api/v1/services/code-registry";

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

            String token = properties.getRegistry().getToken();
            if (token != null && !token.isBlank()) {
                builder.header("X-LogSpectra-Registry-Token", token);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[LogSpectra] Registered code source with backend service={} gitUrl={} status={}",
                    properties.getServiceName(),
                    properties.getCodeSource().getGitUrl(),
                    response.statusCode());
            } else {
                log.warn("[LogSpectra] Code registry rejected status={} body={}",
                    response.statusCode(), truncate(response.body()));
            }
        } catch (Exception ex) {
            log.warn("[LogSpectra] Failed to register code source with backend (analysis may lack code context): {}",
                ex.getMessage());
        }
    }

    private String buildJson() {
        LogSpectraProperties.CodeSource code = properties.getCodeSource();
        List<String> roots = code.getSourceRoots();
        String rootsJson = roots.stream()
            .map(this::quote)
            .collect(Collectors.joining(",", "[", "]"));

        return "{"
            + "\"projectId\":" + quote(properties.getProjectId()) + ","
            + "\"serviceName\":" + quote(properties.getServiceName()) + ","
            + "\"gitUrl\":" + quote(nullToEmpty(code.getGitUrl())) + ","
            + "\"localPath\":" + quote(nullToEmpty(code.getLocalPath())) + ","
            + "\"defaultBranch\":" + quote(code.getDefaultBranch()) + ","
            + "\"sourceRoots\":" + rootsJson
            + "}";
    }

    private String quote(String value) {
        String v = value == null ? "" : value;
        return "\"" + v
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            + "\"";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 200 ? body : body.substring(0, 200) + "...";
    }
}

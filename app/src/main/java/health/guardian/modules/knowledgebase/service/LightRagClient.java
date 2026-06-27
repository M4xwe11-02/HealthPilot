package health.guardian.modules.knowledgebase.service;

import health.guardian.common.config.LightRagProperties;
import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Thin HTTP adapter for the external LightRAG server.
 */
@Slf4j
@Service
public class LightRagClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final LightRagProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LightRagClient(LightRagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(nullToDefault(properties.getConnectTimeout(), Duration.ofSeconds(5)))
            .build();
    }

    public LightRagQueryResult query(String question) {
        ensureEnabled();
        try {
            HttpRequest request = buildJsonPost("/query", buildQueryPayload(question));
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ensureSuccess(response.statusCode(), response.body());

            Map<String, Object> body = objectMapper.readValue(response.body(), MAP_TYPE);
            Object answer = body.get("response");
            return new LightRagQueryResult(answer == null ? "" : answer.toString());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED, "LightRAG 响应解析失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED, "LightRAG 请求被中断");
        }
    }

    public Flux<String> queryStream(String question) {
        if (!properties.isEnabled()) {
            return Flux.just("【错误】LightRAG 未启用，请配置 APP_LIGHTRAG_ENABLED=true 和 APP_LIGHTRAG_BASE_URL。");
        }

        return Flux.create(sink -> {
            AtomicBoolean cancelled = new AtomicBoolean(false);
            final Thread[] workerRef = new Thread[1];

            workerRef[0] = Thread.ofVirtual().name("lightrag-stream-", 0).start(() -> {
                try {
                    HttpRequest request = buildJsonPost("/query/stream", buildQueryPayload(question));
                    HttpResponse<Stream<String>> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofLines()
                    );

                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        sink.error(new BusinessException(
                            ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED,
                            "LightRAG 流式查询失败: HTTP " + response.statusCode()
                        ));
                        return;
                    }

                    try (Stream<String> lines = response.body()) {
                        lines
                            .takeWhile(line -> !cancelled.get() && !Thread.currentThread().isInterrupted())
                            .forEach(line -> emitNdjsonChunk(line, sink));
                    }

                    if (!cancelled.get() && !sink.isCancelled()) {
                        sink.complete();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (!cancelled.get() && !sink.isCancelled()) {
                        sink.error(e);
                    }
                } catch (Exception e) {
                    if (!cancelled.get() && !sink.isCancelled()) {
                        sink.error(e);
                    }
                }
            });

            sink.onCancel(() -> {
                cancelled.set(true);
                if (workerRef[0] != null) {
                    workerRef[0].interrupt();
                }
            });
        });
    }

    public LightRagInsertResult insertText(String text, String fileSource) {
        ensureEnabled();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", text);
            payload.put("file_source", fileSource);

            HttpRequest request = buildJsonPost("/documents/text", payload);
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ensureSuccess(response.statusCode(), response.body());

            Map<String, Object> body = objectMapper.readValue(response.body(), MAP_TYPE);
            return new LightRagInsertResult(
                stringValue(body.get("status")),
                stringValue(body.get("message")),
                stringValue(body.get("track_id"))
            );
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_UPLOAD_FAILED, "LightRAG 入库响应解析失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_UPLOAD_FAILED, "LightRAG 入库请求被中断");
        }
    }

    private Map<String, Object> buildQueryPayload(String question) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", question);
        payload.put("mode", properties.getDefaultMode());
        payload.put("include_references", true);
        payload.put("include_chunk_content", false);
        return payload;
    }

    private HttpRequest buildJsonPost(String path, Map<String, Object> payload) throws IOException {
        String json = objectMapper.writeValueAsString(payload);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(buildUrl(path)))
            .timeout(nullToDefault(properties.getRequestTimeout(), Duration.ofMinutes(3)))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + properties.getApiKey().trim());
        }
        return builder.build();
    }

    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }

    private void emitNdjsonChunk(String line, reactor.core.publisher.FluxSink<String> sink) {
        if (line == null || line.isBlank() || sink.isCancelled()) {
            return;
        }
        try {
            Map<String, Object> body = objectMapper.readValue(line.trim(), MAP_TYPE);
            Object chunk = body.get("response");
            if (chunk != null && !chunk.toString().isEmpty()) {
                sink.next(chunk.toString());
            }
        } catch (Exception e) {
            log.debug("忽略无法解析的 LightRAG 流片段: {}", line);
        }
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException(
                ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED,
                "LightRAG 未启用，请配置 APP_LIGHTRAG_ENABLED=true 和 APP_LIGHTRAG_BASE_URL"
            );
        }
    }

    private void ensureSuccess(int statusCode, String body) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        String message = body == null ? "" : body.substring(0, Math.min(body.length(), 300));
        throw new BusinessException(
            ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED,
            "LightRAG 请求失败: HTTP " + statusCode + " " + message
        );
    }

    private static Duration nullToDefault(Duration value, Duration defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    public record LightRagQueryResult(String response) {
    }

    public record LightRagInsertResult(String status, String message, String trackId) {
    }
}

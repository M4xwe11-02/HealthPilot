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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
    private static final String WORKSPACE_HEADER = "LIGHTRAG-WORKSPACE";
    private static final String ISOLATION_BLOCKED_RESPONSE =
        "抱歉，LightRAG 返回了当前知识库之外的引用，已为避免串库被拦截。请重试或切换到当前检索引擎。";
    private static final int STREAM_RESPONSE_BUFFER_LIMIT = 8192;

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

    public LightRagQueryResult query(String question, String workspace, List<String> allowedReferenceSources) {
        ensureEnabled();
        try {
            HttpRequest request = buildJsonPost("/query", buildQueryPayload(question), workspace);
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ensureSuccess(response.statusCode(), response.body());

            Map<String, Object> body = objectMapper.readValue(response.body(), MAP_TYPE);
            ReferenceCheck referenceCheck = validateReferences(body.get("references"), allowedReferenceSources);
            if (!referenceCheck.allowed()) {
                log.warn("LightRAG response blocked by source isolation: workspace={}, reason={}, rejectedPaths={}",
                    workspace, referenceCheck.reason(), referenceCheck.rejectedPaths());
                return new LightRagQueryResult(ISOLATION_BLOCKED_RESPONSE);
            }
            Object answer = body.get("response");
            return new LightRagQueryResult(answer == null ? "" : answer.toString());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED, "LightRAG 响应解析失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED, "LightRAG 请求被中断");
        }
    }

    public Flux<String> queryStream(String question, String workspace, List<String> allowedReferenceSources) {
        if (!properties.isEnabled()) {
            return Flux.just("【错误】LightRAG 未启用，请配置 APP_LIGHTRAG_ENABLED=true 和 APP_LIGHTRAG_BASE_URL。");
        }

        return Flux.create(sink -> {
            AtomicBoolean cancelled = new AtomicBoolean(false);
            final Thread[] workerRef = new Thread[1];

            workerRef[0] = Thread.ofVirtual().name("lightrag-stream-", 0).start(() -> {
                ReferenceGuard referenceGuard = new ReferenceGuard(allowedReferenceSources);
                StringBuilder responseBuffer = new StringBuilder();
                try {
                    HttpRequest request = buildJsonPost("/query/stream", buildQueryPayload(question), workspace);
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
                        Iterator<String> iterator = lines.iterator();
                        while (!cancelled.get() && !Thread.currentThread().isInterrupted() && iterator.hasNext()) {
                            StreamDecision decision = emitNdjsonChunk(
                                iterator.next(),
                                sink,
                                referenceGuard,
                                responseBuffer
                            );
                            if (decision == StreamDecision.STOP) {
                                return;
                            }
                        }
                    }

                    if (!cancelled.get() && !sink.isCancelled()) {
                        if (referenceGuard.shouldBlockBufferedResponse()) {
                            log.warn("LightRAG stream blocked because references were missing: workspace={}", workspace);
                            sink.next(ISOLATION_BLOCKED_RESPONSE);
                        } else if (!responseBuffer.isEmpty()) {
                            sink.next(responseBuffer.toString());
                        }
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

    public LightRagInsertResult insertText(String text, String fileSource, String workspace) {
        ensureEnabled();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", text);
            payload.put("file_source", fileSource);

            HttpRequest request = buildJsonPost("/documents/text", payload, workspace);
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

    public LightRagTrackStatus getTrackStatus(String trackId, String workspace) {
        ensureEnabled();
        if (trackId == null || trackId.isBlank()) {
            return new LightRagTrackStatus("UNKNOWN", null, List.of());
        }

        try {
            HttpRequest request = buildJsonGet("/documents/track_status/" + trackId.trim(), workspace);
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ensureSuccess(response.statusCode(), response.body());

            Map<String, Object> body = objectMapper.readValue(response.body(), MAP_TYPE);
            Object documents = body.get("documents");
            if (!(documents instanceof Iterable<?> docs)) {
                return new LightRagTrackStatus("UNKNOWN", null, List.of());
            }

            List<String> docIds = new ArrayList<>();
            boolean hasDocument = false;
            boolean allCompleted = true;
            boolean anyFailed = false;
            String firstStatus = null;
            String error = null;

            for (Object doc : docs) {
                if (!(doc instanceof Map<?, ?> docMap)) {
                    continue;
                }
                hasDocument = true;
                String docId = stringValue(docMap.get("id"));
                if (docId != null && !docId.isBlank()) {
                    docIds.add(docId.trim());
                }
                String status = stringValue(docMap.get("status"));
                if (firstStatus == null) {
                    firstStatus = status;
                }
                String normalized = normalizeTrackStatus(status);
                if ("FAILED".equals(normalized)) {
                    anyFailed = true;
                    error = stringValue(docMap.get("error_msg"));
                }
                if (!"COMPLETED".equals(normalized)) {
                    allCompleted = false;
                }
            }

            if (!hasDocument) {
                return new LightRagTrackStatus("UNKNOWN", null, List.of());
            }
            if (anyFailed) {
                return new LightRagTrackStatus("FAILED", error, docIds);
            }
            if (allCompleted) {
                return new LightRagTrackStatus("COMPLETED", null, docIds);
            }
            return new LightRagTrackStatus(normalizeTrackStatus(firstStatus), null, docIds);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED, "LightRAG 状态响应解析失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED, "LightRAG 状态请求被中断");
        }
    }

    public LightRagDeleteResult deleteDocuments(List<String> docIds, String workspace) {
        ensureEnabled();
        List<String> normalizedDocIds = docIds == null ? List.of() : docIds.stream()
            .filter(id -> id != null && !id.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
        if (normalizedDocIds.isEmpty()) {
            return new LightRagDeleteResult("skipped", "No LightRAG document ids to delete");
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("doc_ids", normalizedDocIds);
            payload.put("delete_file", false);
            payload.put("delete_llm_cache", true);

            HttpRequest request = buildJsonDelete("/documents/delete_document", payload, workspace);
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ensureSuccess(response.statusCode(), response.body());

            Map<String, Object> body = objectMapper.readValue(response.body(), MAP_TYPE);
            return new LightRagDeleteResult(
                stringValue(body.get("status")),
                stringValue(body.get("message"))
            );
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED, "LightRAG 删除响应解析失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_QUERY_FAILED, "LightRAG 删除请求被中断");
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

    private HttpRequest buildJsonPost(String path, Map<String, Object> payload, String workspace) throws IOException {
        String json = objectMapper.writeValueAsString(payload);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(buildUrl(path)))
            .timeout(nullToDefault(properties.getRequestTimeout(), Duration.ofMinutes(3)))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        addRequestHeaders(builder, workspace);
        return builder.build();
    }

    private HttpRequest buildJsonGet(String path, String workspace) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(buildUrl(path)))
            .timeout(nullToDefault(properties.getRequestTimeout(), Duration.ofMinutes(3)))
            .GET();

        addRequestHeaders(builder, workspace);
        return builder.build();
    }

    private HttpRequest buildJsonDelete(String path, Map<String, Object> payload, String workspace) throws IOException {
        String json = objectMapper.writeValueAsString(payload);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(buildUrl(path)))
            .timeout(nullToDefault(properties.getRequestTimeout(), Duration.ofMinutes(3)))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .method("DELETE", HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        addRequestHeaders(builder, workspace);
        return builder.build();
    }

    private void addRequestHeaders(HttpRequest.Builder builder, String workspace) {
        addAuthHeader(builder);
        if (workspace != null && !workspace.isBlank()) {
            builder.header(WORKSPACE_HEADER, workspace.trim());
        }
    }

    private void addAuthHeader(HttpRequest.Builder builder) {
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.header("X-API-Key", properties.getApiKey().trim());
        }
    }

    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }

    private StreamDecision emitNdjsonChunk(
        String line,
        reactor.core.publisher.FluxSink<String> sink,
        ReferenceGuard referenceGuard,
        StringBuilder responseBuffer
    ) {
        if (line == null || line.isBlank() || sink.isCancelled()) {
            return StreamDecision.CONTINUE;
        }
        try {
            Map<String, Object> body = objectMapper.readValue(line.trim(), MAP_TYPE);
            if (body.containsKey("references")) {
                ReferenceCheck referenceCheck = referenceGuard.validate(body.get("references"));
                if (!referenceCheck.allowed()) {
                    log.warn("LightRAG stream blocked by source isolation: reason={}, rejectedPaths={}",
                        referenceCheck.reason(), referenceCheck.rejectedPaths());
                    sink.next(ISOLATION_BLOCKED_RESPONSE);
                    sink.complete();
                    return StreamDecision.STOP;
                }
                if (!responseBuffer.isEmpty()) {
                    sink.next(responseBuffer.toString());
                    responseBuffer.setLength(0);
                }
            }

            Object chunk = body.get("response");
            if (chunk != null && !chunk.toString().isEmpty()) {
                referenceGuard.markResponseSeen();
                if (referenceGuard.needsReferenceBeforeEmitting()) {
                    responseBuffer.append(chunk);
                    if (responseBuffer.length() > STREAM_RESPONSE_BUFFER_LIMIT) {
                        log.warn("LightRAG stream blocked because response arrived before references");
                        sink.next(ISOLATION_BLOCKED_RESPONSE);
                        sink.complete();
                        return StreamDecision.STOP;
                    }
                } else {
                    sink.next(chunk.toString());
                }
            }
        } catch (Exception e) {
            log.debug("忽略无法解析的 LightRAG 流片段: {}", line);
        }
        return StreamDecision.CONTINUE;
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

    private static String normalizeTrackStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = status.trim().toUpperCase();
        if (normalized.contains(".")) {
            normalized = normalized.substring(normalized.lastIndexOf('.') + 1);
        }
        return switch (normalized) {
            case "PROCESSED", "COMPLETED", "SUCCESS" -> "COMPLETED";
            case "FAILED", "ERROR" -> "FAILED";
            case "PENDING", "PROCESSING", "PREPROCESSED", "SUBMITTED", "SUBMITTING" -> "PROCESSING";
            default -> normalized;
        };
    }

    private static ReferenceCheck validateReferences(Object references, List<String> allowedReferenceSources) {
        List<String> normalizedAllowedSources = normalizeAllowedSources(allowedReferenceSources);
        if (normalizedAllowedSources.isEmpty()) {
            return ReferenceCheck.allow();
        }
        if (!(references instanceof Iterable<?> refs)) {
            return ReferenceCheck.deny("missing references", List.of());
        }

        boolean hasReference = false;
        List<String> rejectedPaths = new ArrayList<>();
        for (Object ref : refs) {
            hasReference = true;
            String path = extractReferencePath(ref);
            if (path == null || path.isBlank()) {
                rejectedPaths.add("<missing file_path>");
                continue;
            }
            if (!isAllowedReferencePath(path, normalizedAllowedSources)) {
                rejectedPaths.add(path);
            }
        }

        if (!hasReference) {
            return ReferenceCheck.deny("empty references", List.of());
        }
        if (!rejectedPaths.isEmpty()) {
            return ReferenceCheck.deny("references outside selected knowledge bases", rejectedPaths);
        }
        return ReferenceCheck.allow();
    }

    private static List<String> normalizeAllowedSources(List<String> allowedReferenceSources) {
        if (allowedReferenceSources == null || allowedReferenceSources.isEmpty()) {
            return List.of();
        }
        return allowedReferenceSources.stream()
            .filter(source -> source != null && !source.isBlank())
            .map(LightRagClient::normalizeReferencePath)
            .distinct()
            .toList();
    }

    private static String extractReferencePath(Object reference) {
        if (reference instanceof Map<?, ?> refMap) {
            for (String key : List.of("file_path", "file_source", "source", "path", "document_source")) {
                Object value = refMap.get(key);
                if (value != null && !value.toString().isBlank()) {
                    return value.toString();
                }
            }
            return null;
        }
        return reference == null ? null : reference.toString();
    }

    private static boolean isAllowedReferencePath(String path, List<String> allowedSources) {
        String normalizedPath = normalizeReferencePath(path);
        int sourceMarker = normalizedPath.indexOf("knowledge-base/");
        if (sourceMarker >= 0) {
            normalizedPath = normalizedPath.substring(sourceMarker);
        }
        String canonicalPath = normalizedPath;
        String referenceFilename = filenameOf(canonicalPath);
        return allowedSources.stream().anyMatch(allowedSource ->
            canonicalPath.equals(allowedSource)
                || referenceFilename.equals(filenameOf(allowedSource))
        );
    }

    private static String filenameOf(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private static String normalizeReferencePath(String value) {
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private enum StreamDecision {
        CONTINUE,
        STOP
    }

    private static final class ReferenceGuard {
        private final List<String> allowedReferenceSources;
        private boolean validatedReferences;
        private boolean responseSeen;

        private ReferenceGuard(List<String> allowedReferenceSources) {
            this.allowedReferenceSources = normalizeAllowedSources(allowedReferenceSources);
            this.validatedReferences = this.allowedReferenceSources.isEmpty();
        }

        private ReferenceCheck validate(Object references) {
            ReferenceCheck check = validateReferences(references, allowedReferenceSources);
            if (check.allowed()) {
                validatedReferences = true;
            }
            return check;
        }

        private void markResponseSeen() {
            responseSeen = true;
        }

        private boolean needsReferenceBeforeEmitting() {
            return !validatedReferences;
        }

        private boolean shouldBlockBufferedResponse() {
            return responseSeen && !validatedReferences;
        }
    }

    private record ReferenceCheck(boolean allowed, String reason, List<String> rejectedPaths) {
        private static ReferenceCheck allow() {
            return new ReferenceCheck(true, null, List.of());
        }

        private static ReferenceCheck deny(String reason, List<String> rejectedPaths) {
            return new ReferenceCheck(false, reason, rejectedPaths);
        }
    }

    public record LightRagQueryResult(String response) {
    }

    public record LightRagInsertResult(String status, String message, String trackId) {
    }

    public record LightRagTrackStatus(String status, String error, List<String> docIds) {
    }

    public record LightRagDeleteResult(String status, String message) {
    }
}

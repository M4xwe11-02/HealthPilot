package health.guardian.modules.knowledgebase.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import health.guardian.common.config.LightRagProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LightRAG HTTP client isolation tests")
class LightRagClientTest {

    private HttpServer server;
    private final AtomicReference<CapturedRequest> capturedRequest = new AtomicReference<>();

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("query sends workspace header and accepts selected knowledge-base references")
    void querySendsWorkspaceHeaderAndAcceptsAllowedReferences() throws Exception {
        LightRagClient client = startClientWithResponse("""
            {"response":"ok","references":[{"file_path":"knowledge-base/42/doc.txt"}]}
            """);

        LightRagClient.LightRagQueryResult result = client.query(
            "question",
            "user_7",
            List.of("knowledge-base/42/")
        );

        assertEquals("ok", result.response());
        CapturedRequest request = capturedRequest.get();
        assertNotNull(request);
        assertEquals("/query", request.path());
        assertEquals("POST", request.method());
        assertEquals("user_7", request.workspace());
        assertEquals("secret-key", request.apiKey());
        assertTrue(request.body().contains("\"query\":\"question\""));
    }

    @Test
    @DisplayName("query blocks responses that cite sources outside selected knowledge bases")
    void queryBlocksReferencesOutsideSelectedKnowledgeBases() throws Exception {
        LightRagClient client = startClientWithResponse("""
            {"response":"leaked answer","references":[{"file_path":"knowledge-base/99/secret.txt"}]}
            """);

        LightRagClient.LightRagQueryResult result = client.query(
            "question",
            "user_7",
            List.of("knowledge-base/42/")
        );

        assertTrue(result.response().contains("拦截"));
        assertFalse(result.response().contains("leaked answer"));
    }

    @Test
    @DisplayName("query rejects nested source paths that only contain an allowed prefix later")
    void queryBlocksNestedSpoofedKnowledgeBasePath() throws Exception {
        LightRagClient client = startClientWithResponse("""
            {"response":"leaked answer","references":[{"file_path":"knowledge-base/99/knowledge-base/42/secret.txt"}]}
            """);

        LightRagClient.LightRagQueryResult result = client.query(
            "question",
            "user_7",
            List.of("knowledge-base/42/")
        );

        assertTrue(result.response().contains("拦截"));
        assertFalse(result.response().contains("leaked answer"));
    }

    @Test
    @DisplayName("stream blocks response chunks when references are outside selected knowledge bases")
    void streamBlocksReferencesOutsideSelectedKnowledgeBases() throws Exception {
        LightRagClient client = startClientWithResponse("""
            {"references":[{"file_path":"knowledge-base/99/secret.txt"}]}
            {"response":"leaked stream"}
            """);

        List<String> chunks = client.queryStream(
                "question",
                "user_7",
                List.of("knowledge-base/42/")
            )
            .collectList()
            .block(Duration.ofSeconds(3));

        assertNotNull(chunks);
        String joined = String.join("", chunks);
        assertTrue(joined.contains("拦截"));
        assertFalse(joined.contains("leaked stream"));
    }

    private LightRagClient startClientWithResponse(String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            capturedRequest.set(capture(exchange));
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        LightRagProperties properties = new LightRagProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setApiKey("secret-key");
        return new LightRagClient(properties, new ObjectMapper());
    }

    private CapturedRequest capture(HttpExchange exchange) throws IOException {
        return new CapturedRequest(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            exchange.getRequestHeaders().getFirst("LIGHTRAG-WORKSPACE"),
            exchange.getRequestHeaders().getFirst("X-API-Key"),
            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
        );
    }

    private record CapturedRequest(String method, String path, String workspace, String apiKey, String body) {
    }
}

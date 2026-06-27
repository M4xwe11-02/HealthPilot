package health.guardian.modules.knowledgebase;

import health.guardian.modules.knowledgebase.service.KnowledgeBaseDeleteService;
import health.guardian.modules.knowledgebase.service.KnowledgeBaseListService;
import health.guardian.modules.knowledgebase.service.KnowledgeBaseQueryRouterService;
import health.guardian.modules.knowledgebase.service.KnowledgeBaseUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("KnowledgeBaseController Spring MVC integration test")
class KnowledgeBaseControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private KnowledgeBaseUploadService uploadService;

    @Mock
    private KnowledgeBaseQueryRouterService queryRouterService;

    @Mock
    private KnowledgeBaseListService listService;

    @Mock
    private KnowledgeBaseDeleteService deleteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        KnowledgeBaseController controller = new KnowledgeBaseController(
            uploadService,
            queryRouterService,
            listService,
            deleteService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("upload knowledge base returns unified success response")
    void uploadKnowledgeBaseReturnsSuccessResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "java-guide.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "Spring Boot and Redis interview notes".getBytes(StandardCharsets.UTF_8)
        );

        when(uploadService.uploadKnowledgeBase(file, "Java Guide", "Backend"))
            .thenReturn(Map.of(
                "knowledgeBase", Map.of(
                    "id", 1L,
                    "name", "Java Guide",
                    "category", "Backend",
                    "vectorStatus", "PENDING"
                ),
                "storage", Map.of(
                    "fileKey", "knowledge-base/java-guide.txt",
                    "fileUrl", "http://localhost:9000/interview-guide/java-guide.txt"
                ),
                "lightRag", Map.of(
                    "submitted", true,
                    "status", "SUBMITTING"
                ),
                "duplicate", false
            ));

        mockMvc.perform(multipart("/api/knowledgebase/upload")
                .file(file)
                .param("name", "Java Guide")
                .param("category", "Backend"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.message", is("success")))
            .andExpect(jsonPath("$.data.knowledgeBase.id", is(1)))
            .andExpect(jsonPath("$.data.knowledgeBase.name", is("Java Guide")))
            .andExpect(jsonPath("$.data.knowledgeBase.vectorStatus", is("PENDING")))
            .andExpect(jsonPath("$.data.lightRag.submitted", is(true)));

        verify(uploadService).uploadKnowledgeBase(file, "Java Guide", "Backend");
    }
}

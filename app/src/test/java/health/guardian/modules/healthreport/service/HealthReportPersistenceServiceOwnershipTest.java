package health.guardian.modules.healthreport.service;

import health.guardian.infrastructure.file.FileHashService;
import health.guardian.infrastructure.mapper.HealthReportMapper;
import health.guardian.modules.auth.service.CurrentUserService;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import health.guardian.modules.healthreport.repository.HealthReportAnalysisRepository;
import health.guardian.modules.healthreport.repository.HealthReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthReportPersistenceService ownership")
class HealthReportPersistenceServiceOwnershipTest {

    @Mock
    private HealthReportRepository healthReportRepository;

    @Mock
    private HealthReportAnalysisRepository analysisRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HealthReportMapper healthReportMapper;

    @Mock
    private FileHashService fileHashService;

    @Mock
    private CurrentUserService currentUserService;

    private HealthReportPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new HealthReportPersistenceService(
            healthReportRepository,
            analysisRepository,
            objectMapper,
            healthReportMapper,
            fileHashService,
            currentUserService
        );
    }

    @Test
    @DisplayName("duplicate detection uses current user scope")
    void duplicateDetectionUsesCurrentUserScope() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.txt",
            "text/plain",
            "resume".getBytes(StandardCharsets.UTF_8)
        );
        HealthReportEntity existing = new HealthReportEntity();
        existing.setId(5L);
        existing.setFileHash("hash-1");
        existing.setAccessCount(1);

        when(fileHashService.calculateHash(file)).thenReturn("hash-1");
        when(currentUserService.requireCurrentUserId()).thenReturn(10L);
        when(healthReportRepository.findByOwner_IdAndFileHash(10L, "hash-1")).thenReturn(Optional.of(existing));

        Optional<HealthReportEntity> result = service.findExistingHealthReport(file);

        assertThat(result).contains(existing);
        verify(healthReportRepository).findByOwner_IdAndFileHash(10L, "hash-1");
    }
}

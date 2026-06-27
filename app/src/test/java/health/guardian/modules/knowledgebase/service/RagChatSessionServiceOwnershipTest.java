package health.guardian.modules.knowledgebase.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.infrastructure.mapper.KnowledgeBaseMapper;
import health.guardian.infrastructure.mapper.RagChatMapper;
import health.guardian.modules.auth.service.CurrentUserService;
import health.guardian.modules.knowledgebase.model.RagChatDTO.CreateSessionRequest;
import health.guardian.modules.knowledgebase.repository.KnowledgeBaseRepository;
import health.guardian.modules.knowledgebase.repository.RagChatMessageRepository;
import health.guardian.modules.knowledgebase.repository.RagChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagChatSessionService ownership")
class RagChatSessionServiceOwnershipTest {

    @Mock
    private RagChatSessionRepository sessionRepository;

    @Mock
    private RagChatMessageRepository messageRepository;

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private KnowledgeBaseQueryRouterService queryRouterService;

    @Mock
    private RagChatMapper ragChatMapper;

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private CurrentUserService currentUserService;

    private RagChatSessionService service;

    @BeforeEach
    void setUp() {
        service = new RagChatSessionService(
            sessionRepository,
            messageRepository,
            knowledgeBaseRepository,
            queryRouterService,
            ragChatMapper,
            knowledgeBaseMapper,
            currentUserService
        );
    }

    @Test
    @DisplayName("create session rejects knowledge bases outside current user scope")
    void createSessionRejectsKnowledgeBasesOutsideCurrentUserScope() {
        when(currentUserService.requireCurrentUserId()).thenReturn(10L);
        when(knowledgeBaseRepository.findAllByOwner_IdAndIdIn(10L, List.of(1L, 2L))).thenReturn(List.of());

        CreateSessionRequest request = new CreateSessionRequest(List.of(1L, 2L), "private chat");

        assertThatThrownBy(() -> service.createSession(request))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.NOT_FOUND.getCode());

        verify(sessionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

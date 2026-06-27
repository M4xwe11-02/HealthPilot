package health.guardian.modules.knowledgebase.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.auth.service.CurrentUserService;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseEntity;
import health.guardian.modules.knowledgebase.repository.KnowledgeBaseRepository;
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
@DisplayName("KnowledgeBaseCountService ownership")
class KnowledgeBaseCountServiceOwnershipTest {

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private CurrentUserService currentUserService;

    private KnowledgeBaseCountService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeBaseCountService(knowledgeBaseRepository, currentUserService);
    }

    @Test
    @DisplayName("question count update rejects ids outside current user scope")
    void questionCountUpdateRejectsIdsOutsideCurrentUserScope() {
        KnowledgeBaseEntity owned = new KnowledgeBaseEntity();
        owned.setId(1L);

        when(currentUserService.requireCurrentUserId()).thenReturn(10L);
        when(knowledgeBaseRepository.findAllByOwner_IdAndIdIn(10L, List.of(1L, 2L))).thenReturn(List.of(owned));

        assertThatThrownBy(() -> service.updateQuestionCounts(List.of(1L, 2L)))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.NOT_FOUND.getCode());

        verify(knowledgeBaseRepository, never()).incrementQuestionCountBatchForOwner(10L, List.of(1L, 2L));
    }
}

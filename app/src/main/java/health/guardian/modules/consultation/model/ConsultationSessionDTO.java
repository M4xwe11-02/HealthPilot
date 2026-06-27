package health.guardian.modules.consultation.model;

import java.util.List;

/**
 * 问诊会话DTO
 */
public record ConsultationSessionDTO(
    String sessionId,
    String reportText,
    int totalQuestions,
    int currentQuestionIndex,
    List<ConsultationQuestionDTO> questions,
    SessionStatus status
) {
    public enum SessionStatus {
        CREATED,      // 会话已创建
        IN_PROGRESS,  // 问诊进行中
        COMPLETED,    // 问诊已完成
        EVALUATED     // 已生成评估报告
    }
}

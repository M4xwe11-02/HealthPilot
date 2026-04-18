package health.guardian.infrastructure.mapper;

import health.guardian.modules.consultation.model.ConsultationAnswerEntity;
import health.guardian.modules.consultation.model.ConsultationDetailDTO;
import health.guardian.modules.consultation.model.ConsultationReportDTO;
import health.guardian.modules.consultation.model.ConsultationSessionEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * 问诊相关的对象映射器
 * 使用MapStruct自动生成转换代码
 * <p>
 * 注意：JSON字段需要在Service层手动处理
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConsultationMapper {

    // ========== QuestionEvaluation 映射 ==========

    /**
     * 将问诊答案实体转换为问题评估详情
     */
    @Mapping(target = "questionIndex", source = "questionIndex", qualifiedByName = "nullIndexToZero")
    @Mapping(target = "question", source = "question")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "userAnswer", source = "userAnswer")
    @Mapping(target = "score", source = "score", qualifiedByName = "nullScoreToZero")
    @Mapping(target = "feedback", source = "feedback")
    ConsultationReportDTO.QuestionEvaluation toQuestionEvaluation(ConsultationAnswerEntity entity);

    /**
     * 批量转换问诊答案实体
     */
    List<ConsultationReportDTO.QuestionEvaluation> toQuestionEvaluations(List<ConsultationAnswerEntity> entities);

    // ========== AnswerDetailDTO 映射 ==========

    /**
     * ConsultationAnswerEntity 转换为 AnswerDetailDTO
     * 注意：keyPoints 需要从 JSON 解析后传入
     */
    @Mapping(target = "keyPoints", source = "keyPoints")
    ConsultationDetailDTO.AnswerDetailDTO toAnswerDetailDTO(
        ConsultationAnswerEntity entity,
        List<String> keyPoints
    );

    /**
     * 批量转换（需要在 Service 层处理 JSON）
     */
    default List<ConsultationDetailDTO.AnswerDetailDTO> toAnswerDetailDTOList(
        List<ConsultationAnswerEntity> entities,
        java.util.function.Function<ConsultationAnswerEntity, List<String>> keyPointsExtractor
    ) {
        return entities.stream()
            .map(e -> toAnswerDetailDTO(e, keyPointsExtractor.apply(e)))
            .toList();
    }

    // ========== ConsultationDetailDTO 映射 ==========

    /**
     * ConsultationSessionEntity 转换为 ConsultationDetailDTO
     * 注意：questions, strengths, improvements, referenceAnswers, answers 需要在 Service 层处理
     */
    @Mapping(target = "status", expression = "java(session.getStatus().toString())")
    @Mapping(target = "evaluateStatus", expression = "java(session.getEvaluateStatus() != null ? session.getEvaluateStatus().name() : null)")
    @Mapping(target = "evaluateError", source = "session.evaluateError")
    @Mapping(target = "questions", source = "questions")
    @Mapping(target = "strengths", source = "strengths")
    @Mapping(target = "improvements", source = "improvements")
    @Mapping(target = "referenceAnswers", source = "referenceAnswers")
    @Mapping(target = "answers", source = "answers")
    ConsultationDetailDTO toDetailDTO(
        ConsultationSessionEntity session,
        List<Object> questions,
        List<String> strengths,
        List<String> improvements,
        List<Object> referenceAnswers,
        List<ConsultationDetailDTO.AnswerDetailDTO> answers
    );

    // ========== ConsultationSessionEntity 更新映射 ==========

    /**
     * 从 ConsultationReportDTO 更新 ConsultationSessionEntity
     * 注意：JSON 字段需要在 Service 层单独设置
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sessionId", ignore = true)
    @Mapping(target = "healthReport", ignore = true)
    @Mapping(target = "totalQuestions", ignore = true)
    @Mapping(target = "currentQuestionIndex", ignore = true)
    @Mapping(target = "questionsJson", ignore = true)
    @Mapping(target = "strengthsJson", ignore = true)
    @Mapping(target = "improvementsJson", ignore = true)
    @Mapping(target = "referenceAnswersJson", ignore = true)
    @Mapping(target = "answers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    void updateSessionFromReport(ConsultationReportDTO report, @MappingTarget ConsultationSessionEntity session);

    // ========== 问诊历史列表项映射 ==========

    /**
     * ConsultationSessionEntity 转换为简要信息 Map
     * 用于 HealthReportDetailDTO 中的问诊历史列表
     */
    default java.util.Map<String, Object> toConsultationHistoryItem(ConsultationSessionEntity session) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", session.getId());
        map.put("sessionId", session.getSessionId());
        map.put("totalQuestions", session.getTotalQuestions());
        map.put("status", session.getStatus().toString());
        map.put("evaluateStatus", session.getEvaluateStatus() != null ? session.getEvaluateStatus().name() : null);
        map.put("evaluateError", session.getEvaluateError());
        map.put("overallScore", session.getOverallScore());
        map.put("createdAt", session.getCreatedAt());
        map.put("completedAt", session.getCompletedAt());
        return map;
    }

    /**
     * 批量转换问诊历史
     */
    default List<Object> toConsultationHistoryList(List<ConsultationSessionEntity> sessions) {
        return sessions.stream()
            .map(this::toConsultationHistoryItem)
            .map(m -> (Object) m)
            .toList();
    }

    // ========== 工具方法 ==========

    @Named("nullIndexToZero")
    default int nullIndexToZero(Integer value) {
        return value != null ? value : 0;
    }

    @Named("nullScoreToZero")
    default int nullScoreToZero(Integer value) {
        return value != null ? value : 0;
    }
}

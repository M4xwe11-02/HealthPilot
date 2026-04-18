package health.guardian.modules.consultation.listener;

import health.guardian.common.async.AbstractStreamConsumer;
import health.guardian.common.constant.AsyncTaskStreamConstants;
import health.guardian.common.model.AsyncTaskStatus;
import health.guardian.infrastructure.redis.RedisService;
import health.guardian.modules.consultation.model.ConsultationQuestionDTO;
import health.guardian.modules.consultation.model.ConsultationReportDTO;
import health.guardian.modules.consultation.model.ConsultationSessionEntity;
import health.guardian.modules.consultation.repository.ConsultationSessionRepository;
import health.guardian.modules.consultation.service.AnswerEvaluationService;
import health.guardian.modules.consultation.service.ConsultationPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.stream.StreamMessageId;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 问诊评估 Stream 消费者
 * 负责从 Redis Stream 消费消息并执行评估
 */
@Slf4j
@Component
public class EvaluateStreamConsumer extends AbstractStreamConsumer<EvaluateStreamConsumer.EvaluatePayload> {

    private final ConsultationSessionRepository sessionRepository;
    private final AnswerEvaluationService evaluationService;
    private final ConsultationPersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    public EvaluateStreamConsumer(
        RedisService redisService,
        ConsultationSessionRepository sessionRepository,
        AnswerEvaluationService evaluationService,
        ConsultationPersistenceService persistenceService,
        ObjectMapper objectMapper
    ) {
        super(redisService);
        this.sessionRepository = sessionRepository;
        this.evaluationService = evaluationService;
        this.persistenceService = persistenceService;
        this.objectMapper = objectMapper;
    }

    record EvaluatePayload(String sessionId) {}

    @Override
    protected String taskDisplayName() {
        return "评估";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.CONSULTATION_EVALUATE_STREAM_KEY;
    }

    @Override
    protected String groupName() {
        return AsyncTaskStreamConstants.CONSULTATION_EVALUATE_GROUP_NAME;
    }

    @Override
    protected String consumerPrefix() {
        return AsyncTaskStreamConstants.CONSULTATION_EVALUATE_CONSUMER_PREFIX;
    }

    @Override
    protected String threadName() {
        return "evaluate-consumer";
    }

    @Override
    protected EvaluatePayload parsePayload(StreamMessageId messageId, Map<String, String> data) {
        String sessionId = data.get(AsyncTaskStreamConstants.FIELD_SESSION_ID);
        if (sessionId == null) {
            log.warn("消息格式错误，跳过: messageId={}", messageId);
            return null;
        }
        return new EvaluatePayload(sessionId);
    }

    @Override
    protected String payloadIdentifier(EvaluatePayload payload) {
        return "sessionId=" + payload.sessionId();
    }

    @Override
    protected void markProcessing(EvaluatePayload payload) {
        updateEvaluateStatus(payload.sessionId(), AsyncTaskStatus.PROCESSING, null);
    }

    @Override
    protected void processBusiness(EvaluatePayload payload) {
        String sessionId = payload.sessionId();
        Optional<ConsultationSessionEntity> sessionOpt = sessionRepository.findBySessionIdWithHealthReport(sessionId);
        if (sessionOpt.isEmpty()) {
            log.warn("会话已被删除，跳过评估任务: sessionId={}", sessionId);
            return;
        }

        ConsultationSessionEntity session = sessionOpt.get();
        List<ConsultationQuestionDTO> questions = objectMapper.readValue(
            session.getQuestionsJson(),
            new TypeReference<>() {}
        );

        List<health.guardian.modules.consultation.model.ConsultationAnswerEntity> answers =
            persistenceService.findAnswersBySessionId(sessionId);
        for (health.guardian.modules.consultation.model.ConsultationAnswerEntity answer : answers) {
            int index = answer.getQuestionIndex();
            if (index >= 0 && index < questions.size()) {
                ConsultationQuestionDTO question = questions.get(index);
                questions.set(index, question.withAnswer(answer.getUserAnswer()));
            }
        }

        String reportText = session.getHealthReport().getReportText();
        ConsultationReportDTO report = evaluationService.evaluateConsultation(sessionId, reportText, questions);
        persistenceService.saveReport(sessionId, report);
    }

    @Override
    protected void markCompleted(EvaluatePayload payload) {
        updateEvaluateStatus(payload.sessionId(), AsyncTaskStatus.COMPLETED, null);
    }

    @Override
    protected void markFailed(EvaluatePayload payload, String error) {
        updateEvaluateStatus(payload.sessionId(), AsyncTaskStatus.FAILED, error);
    }

    @Override
    protected void retryMessage(EvaluatePayload payload, int retryCount) {
        String sessionId = payload.sessionId();
        try {
            Map<String, String> message = Map.of(
                AsyncTaskStreamConstants.FIELD_SESSION_ID, sessionId,
                AsyncTaskStreamConstants.FIELD_RETRY_COUNT, String.valueOf(retryCount)
            );

            redisService().streamAdd(
                AsyncTaskStreamConstants.CONSULTATION_EVALUATE_STREAM_KEY,
                message,
                AsyncTaskStreamConstants.STREAM_MAX_LEN
            );
            log.info("评估任务已重新入队: sessionId={}, retryCount={}", sessionId, retryCount);

        } catch (Exception e) {
            log.error("重试入队失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            updateEvaluateStatus(sessionId, AsyncTaskStatus.FAILED, truncateError("重试入队失败: " + e.getMessage()));
        }
    }

    /**
     * 更新评估状态
     */
    private void updateEvaluateStatus(String sessionId, AsyncTaskStatus status, String error) {
        try {
            sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
                session.setEvaluateStatus(status);
                session.setEvaluateError(error);
                sessionRepository.save(session);
                log.debug("评估状态已更新: sessionId={}, status={}", sessionId, status);
            });
        } catch (Exception e) {
            log.error("更新评估状态失败: sessionId={}, status={}, error={}", sessionId, status, e.getMessage(), e);
        }
    }

}

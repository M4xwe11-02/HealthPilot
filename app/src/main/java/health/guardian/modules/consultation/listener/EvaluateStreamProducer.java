package health.guardian.modules.consultation.listener;

import health.guardian.common.async.AbstractStreamProducer;
import health.guardian.common.constant.AsyncTaskStreamConstants;
import health.guardian.common.model.AsyncTaskStatus;
import health.guardian.infrastructure.redis.RedisService;
import health.guardian.modules.consultation.repository.ConsultationSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 问诊评估任务生产者
 * 负责发送评估任务到 Redis Stream
 */
@Slf4j
@Component
public class EvaluateStreamProducer extends AbstractStreamProducer<String> {

    private final ConsultationSessionRepository sessionRepository;

    public EvaluateStreamProducer(RedisService redisService, ConsultationSessionRepository sessionRepository) {
        super(redisService);
        this.sessionRepository = sessionRepository;
    }

    /**
     * 发送评估任务到 Redis Stream
     *
     * @param sessionId 问诊会话ID
     */
    public void sendEvaluateTask(String sessionId) {
        sendTask(sessionId);
    }

    @Override
    protected String taskDisplayName() {
        return "评估";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.CONSULTATION_EVALUATE_STREAM_KEY;
    }

    @Override
    protected Map<String, String> buildMessage(String sessionId) {
        return Map.of(
            AsyncTaskStreamConstants.FIELD_SESSION_ID, sessionId,
            AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0"
        );
    }

    @Override
    protected String payloadIdentifier(String sessionId) {
        return "sessionId=" + sessionId;
    }

    @Override
    protected void onSendFailed(String sessionId, String error) {
        updateEvaluateStatus(sessionId, AsyncTaskStatus.FAILED, truncateError(error));
    }

    /**
     * 更新评估状态
     */
    private void updateEvaluateStatus(String sessionId, AsyncTaskStatus status, String error) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setEvaluateStatus(status);
            if (error != null) {
                session.setEvaluateError(error.length() > 500 ? error.substring(0, 500) : error);
            }
            sessionRepository.save(session);
        });
    }
}

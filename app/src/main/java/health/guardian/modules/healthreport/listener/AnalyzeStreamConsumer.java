package health.guardian.modules.healthreport.listener;

import health.guardian.common.async.AbstractStreamConsumer;
import health.guardian.common.constant.AsyncTaskStreamConstants;
import health.guardian.common.model.AsyncTaskStatus;
import health.guardian.infrastructure.redis.RedisService;
import health.guardian.modules.consultation.model.HealthReportAnalysisResponse;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import health.guardian.modules.healthreport.repository.HealthReportRepository;
import health.guardian.modules.healthreport.service.HealthReportGradingService;
import health.guardian.modules.healthreport.service.HealthReportPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.stream.StreamMessageId;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 健康分析 Stream 消费者
 * 负责从 Redis Stream 消费消息并执行 AI 分析
 */
@Slf4j
@Component
public class AnalyzeStreamConsumer extends AbstractStreamConsumer<AnalyzeStreamConsumer.AnalyzePayload> {

    private final HealthReportGradingService gradingService;
    private final HealthReportPersistenceService persistenceService;
    private final HealthReportRepository healthReportRepository;

    public AnalyzeStreamConsumer(
        RedisService redisService,
        HealthReportGradingService gradingService,
        HealthReportPersistenceService persistenceService,
        HealthReportRepository healthReportRepository
    ) {
        super(redisService);
        this.gradingService = gradingService;
        this.persistenceService = persistenceService;
        this.healthReportRepository = healthReportRepository;
    }

    record AnalyzePayload(Long healthReportId, String content) {}

    @Override
    protected String taskDisplayName() {
        return "健康分析";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.HEALTH_REPORT_ANALYZE_STREAM_KEY;
    }

    @Override
    protected String groupName() {
        return AsyncTaskStreamConstants.HEALTH_REPORT_ANALYZE_GROUP_NAME;
    }

    @Override
    protected String consumerPrefix() {
        return AsyncTaskStreamConstants.HEALTH_REPORT_ANALYZE_CONSUMER_PREFIX;
    }

    @Override
    protected String threadName() {
        return "analyze-consumer";
    }

    @Override
    protected AnalyzePayload parsePayload(StreamMessageId messageId, Map<String, String> data) {
        String healthReportIdStr = data.get(AsyncTaskStreamConstants.FIELD_HEALTH_REPORT_ID);
        String content = data.get(AsyncTaskStreamConstants.FIELD_CONTENT);
        if (healthReportIdStr == null || content == null) {
            log.warn("消息格式错误，跳过: messageId={}", messageId);
            return null;
        }
        return new AnalyzePayload(Long.parseLong(healthReportIdStr), content);
    }

    @Override
    protected String payloadIdentifier(AnalyzePayload payload) {
        return "healthReportId=" + payload.healthReportId();
    }

    @Override
    protected void markProcessing(AnalyzePayload payload) {
        updateAnalyzeStatus(payload.healthReportId(), AsyncTaskStatus.PROCESSING, null);
    }

    @Override
    protected void processBusiness(AnalyzePayload payload) {
        Long healthReportId = payload.healthReportId();
        if (!healthReportRepository.existsById(healthReportId)) {
            log.warn("体检报告已被删除，跳过分析任务: healthReportId={}", healthReportId);
            return;
        }

        HealthReportAnalysisResponse analysis = gradingService.analyzeHealthReport(payload.content());
        HealthReportEntity healthReport = healthReportRepository.findById(healthReportId).orElse(null);
        if (healthReport == null) {
            log.warn("体检报告在分析期间被删除，跳过保存结果: healthReportId={}", healthReportId);
            return;
        }
        persistenceService.saveAnalysis(healthReport, analysis);
    }

    @Override
    protected void markCompleted(AnalyzePayload payload) {
        updateAnalyzeStatus(payload.healthReportId(), AsyncTaskStatus.COMPLETED, null);
    }

    @Override
    protected void markFailed(AnalyzePayload payload, String error) {
        updateAnalyzeStatus(payload.healthReportId(), AsyncTaskStatus.FAILED, error);
    }

    @Override
    protected void retryMessage(AnalyzePayload payload, int retryCount) {
        Long healthReportId = payload.healthReportId();
        String content = payload.content();
        try {
            Map<String, String> message = Map.of(
                AsyncTaskStreamConstants.FIELD_HEALTH_REPORT_ID, healthReportId.toString(),
                AsyncTaskStreamConstants.FIELD_CONTENT, content,
                AsyncTaskStreamConstants.FIELD_RETRY_COUNT, String.valueOf(retryCount)
            );

            redisService().streamAdd(
                AsyncTaskStreamConstants.HEALTH_REPORT_ANALYZE_STREAM_KEY,
                message,
                AsyncTaskStreamConstants.STREAM_MAX_LEN
            );
            log.info("健康分析任务已重新入队: healthReportId={}, retryCount={}", healthReportId, retryCount);

        } catch (Exception e) {
            log.error("重试入队失败: healthReportId={}, error={}", healthReportId, e.getMessage(), e);
            updateAnalyzeStatus(healthReportId, AsyncTaskStatus.FAILED, truncateError("重试入队失败: " + e.getMessage()));
        }
    }

    /**
     * 更新分析状态
     */
    private void updateAnalyzeStatus(Long healthReportId, AsyncTaskStatus status, String error) {
        try {
            healthReportRepository.findById(healthReportId).ifPresent(healthReport -> {
                healthReport.setAnalyzeStatus(status);
                healthReport.setAnalyzeError(error);
                healthReportRepository.save(healthReport);
                log.debug("分析状态已更新: healthReportId={}, status={}", healthReportId, status);
            });
        } catch (Exception e) {
            log.error("更新分析状态失败: healthReportId={}, status={}, error={}", healthReportId, status, e.getMessage(), e);
        }
    }

}

package health.guardian.modules.healthreport.listener;

import health.guardian.common.async.AbstractStreamProducer;
import health.guardian.common.constant.AsyncTaskStreamConstants;
import health.guardian.common.model.AsyncTaskStatus;
import health.guardian.infrastructure.redis.RedisService;
import health.guardian.modules.healthreport.repository.HealthReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 健康分析任务生产者
 * 负责发送分析任务到 Redis Stream
 */
@Slf4j
@Component
public class AnalyzeStreamProducer extends AbstractStreamProducer<AnalyzeStreamProducer.AnalyzeTaskPayload> {

    private final HealthReportRepository healthReportRepository;

    record AnalyzeTaskPayload(Long healthReportId, String content) {}

    public AnalyzeStreamProducer(RedisService redisService, HealthReportRepository healthReportRepository) {
        super(redisService);
        this.healthReportRepository = healthReportRepository;
    }

    /**
     * 发送分析任务到 Redis Stream
     *
     * @param healthReportId 体检报告ID
     * @param content  体检报告内容
     */
    public void sendAnalyzeTask(Long healthReportId, String content) { // healthReportId就是主键ID，content就是reportText
        sendTask(new AnalyzeTaskPayload(healthReportId, content));
    }

    @Override
    protected String taskDisplayName() {
        return "分析";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.HEALTH_REPORT_ANALYZE_STREAM_KEY;
    }

    @Override
    protected Map<String, String> buildMessage(AnalyzeTaskPayload payload) {
        return Map.of(
            AsyncTaskStreamConstants.FIELD_HEALTH_REPORT_ID, payload.healthReportId().toString(),
                AsyncTaskStreamConstants.FIELD_CONTENT, payload.content(),
                AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0"
        );
    }

    @Override
    protected String payloadIdentifier(AnalyzeTaskPayload payload) {
        return "healthReportId=" + payload.healthReportId();
    }

    @Override
    protected void onSendFailed(AnalyzeTaskPayload payload, String error) {
        updateAnalyzeStatus(payload.healthReportId(), AsyncTaskStatus.FAILED, truncateError(error));
    }

    /**
     * 更新分析状态
     */
    private void updateAnalyzeStatus(Long healthReportId, AsyncTaskStatus status, String error) {
        healthReportRepository.findById(healthReportId).ifPresent(healthReport -> {
            healthReport.setAnalyzeStatus(status);
            if (error != null) {
                healthReport.setAnalyzeError(error.length() > 500 ? error.substring(0, 500) : error);
            }
            healthReportRepository.save(healthReport);
        });
    }
}

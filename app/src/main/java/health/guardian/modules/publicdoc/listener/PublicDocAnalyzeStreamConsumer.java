package health.guardian.modules.publicdoc.listener;

import health.guardian.common.async.AbstractStreamConsumer;
import health.guardian.common.constant.AsyncTaskStreamConstants;
import health.guardian.common.model.AsyncTaskStatus;
import health.guardian.infrastructure.redis.PublicDocCache;
import health.guardian.infrastructure.redis.RedisService;
import health.guardian.modules.publicdoc.model.PublicDocListItemDTO;
import health.guardian.modules.publicdoc.repository.PublicDocRepository;
import health.guardian.modules.publicdoc.service.PublicDocAnalysisService;
import health.guardian.modules.publicdoc.service.PublicDocAnalysisService.PublicDocAnalysisResult;
import health.guardian.modules.publicdoc.service.PublicDocPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.stream.StreamMessageId;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Slf4j
@Component
public class PublicDocAnalyzeStreamConsumer extends AbstractStreamConsumer<PublicDocAnalyzeStreamConsumer.AnalyzePayload> {

    private final PublicDocAnalysisService analysisService;
    private final PublicDocPersistenceService persistenceService;
    private final PublicDocRepository docRepository;
    private final PublicDocCache cache;
    private final ObjectMapper objectMapper;

    public PublicDocAnalyzeStreamConsumer(
        RedisService redisService,
        PublicDocAnalysisService analysisService,
        PublicDocPersistenceService persistenceService,
        PublicDocRepository docRepository,
        PublicDocCache cache,
        ObjectMapper objectMapper
    ) {
        super(redisService);
        this.analysisService = analysisService;
        this.persistenceService = persistenceService;
        this.docRepository = docRepository;
        this.cache = cache;
        this.objectMapper = objectMapper;
    }

    record AnalyzePayload(Long publicDocId, String content) {}

    @Override
    protected String taskDisplayName() { return "公共文档分析"; }

    @Override
    protected String streamKey() { return AsyncTaskStreamConstants.PUBLIC_DOC_ANALYZE_STREAM_KEY; }

    @Override
    protected String groupName() { return AsyncTaskStreamConstants.PUBLIC_DOC_ANALYZE_GROUP_NAME; }

    @Override
    protected String consumerPrefix() { return AsyncTaskStreamConstants.PUBLIC_DOC_ANALYZE_CONSUMER_PREFIX; }

    @Override
    protected String threadName() { return "publicdoc-analyze-consumer"; }

    @Override
    protected AnalyzePayload parsePayload(StreamMessageId messageId, Map<String, String> data) {
        String docIdStr = data.get(AsyncTaskStreamConstants.FIELD_PUBLIC_DOC_ID);
        String content = data.get(AsyncTaskStreamConstants.FIELD_CONTENT);
        if (docIdStr == null || content == null) {
            log.warn("公共文档消息格式错误，跳过: messageId={}", messageId);
            return null;
        }
        return new AnalyzePayload(Long.parseLong(docIdStr), content);
    }

    @Override
    protected String payloadIdentifier(AnalyzePayload payload) {
        return "publicDocId=" + payload.publicDocId();
    }

    @Override
    protected void markProcessing(AnalyzePayload payload) {
        persistenceService.updateStatus(payload.publicDocId(), AsyncTaskStatus.PROCESSING, null);
    }

    @Override
    protected void processBusiness(AnalyzePayload payload) {
        Long publicDocId = payload.publicDocId();
        if (!docRepository.existsById(publicDocId)) {
            log.warn("公共文档已被删除，跳过分析: publicDocId={}", publicDocId);
            return;
        }

        PublicDocAnalysisResult result = analysisService.analyze(payload.content());

        try {
            String keyPointsJson = objectMapper.writeValueAsString(result.keyPoints());
            String recommendationsJson = objectMapper.writeValueAsString(result.mainRecommendations());
            persistenceService.saveAnalysis(publicDocId, result.summary(), keyPointsJson,
                result.applicablePopulation(), recommendationsJson);
        } catch (Exception e) {
            throw new RuntimeException("保存公共文档分析结果失败: " + e.getMessage(), e);
        }

        cache.invalidateDetail(publicDocId);
        cache.invalidateList();
        log.info("公共文档AI分析完成: publicDocId={}", publicDocId);
    }

    @Override
    protected void markCompleted(AnalyzePayload payload) {
        persistenceService.updateStatus(payload.publicDocId(), AsyncTaskStatus.COMPLETED, null);
    }

    @Override
    protected void markFailed(AnalyzePayload payload, String error) {
        persistenceService.updateStatus(payload.publicDocId(), AsyncTaskStatus.FAILED, error);
        cache.invalidateDetail(payload.publicDocId());
        cache.invalidateList();
    }

    @Override
    protected void retryMessage(AnalyzePayload payload, int retryCount) {
        try {
            Map<String, String> message = Map.of(
                AsyncTaskStreamConstants.FIELD_PUBLIC_DOC_ID, payload.publicDocId().toString(),
                AsyncTaskStreamConstants.FIELD_CONTENT, payload.content(),
                AsyncTaskStreamConstants.FIELD_RETRY_COUNT, String.valueOf(retryCount)
            );
            redisService().streamAdd(
                AsyncTaskStreamConstants.PUBLIC_DOC_ANALYZE_STREAM_KEY,
                message,
                AsyncTaskStreamConstants.STREAM_MAX_LEN
            );
            log.info("公共文档分析任务重新入队: publicDocId={}, retryCount={}", payload.publicDocId(), retryCount);
        } catch (Exception e) {
            log.error("重试入队失败: publicDocId={}, error={}", payload.publicDocId(), e.getMessage(), e);
            persistenceService.updateStatus(payload.publicDocId(), AsyncTaskStatus.FAILED,
                truncateError("重试入队失败: " + e.getMessage()));
        }
    }
}

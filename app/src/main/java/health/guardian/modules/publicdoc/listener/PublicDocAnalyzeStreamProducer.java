package health.guardian.modules.publicdoc.listener;

import health.guardian.common.async.AbstractStreamProducer;
import health.guardian.common.constant.AsyncTaskStreamConstants;
import health.guardian.common.model.AsyncTaskStatus;
import health.guardian.infrastructure.redis.RedisService;
import health.guardian.modules.publicdoc.repository.PublicDocRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PublicDocAnalyzeStreamProducer extends AbstractStreamProducer<PublicDocAnalyzeStreamProducer.AnalyzeTaskPayload> {

    private final PublicDocRepository docRepository;

    record AnalyzeTaskPayload(Long publicDocId, String content) {}

    public PublicDocAnalyzeStreamProducer(RedisService redisService, PublicDocRepository docRepository) {
        super(redisService);
        this.docRepository = docRepository;
    }

    public void sendAnalyzeTask(Long publicDocId, String content) {
        sendTask(new AnalyzeTaskPayload(publicDocId, content));
    }

    @Override
    protected String taskDisplayName() {
        return "公共文档分析";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.PUBLIC_DOC_ANALYZE_STREAM_KEY;
    }

    @Override
    protected Map<String, String> buildMessage(AnalyzeTaskPayload payload) {
        return Map.of(
            AsyncTaskStreamConstants.FIELD_PUBLIC_DOC_ID, payload.publicDocId().toString(),
            AsyncTaskStreamConstants.FIELD_CONTENT, payload.content(),
            AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0"
        );
    }

    @Override
    protected String payloadIdentifier(AnalyzeTaskPayload payload) {
        return "publicDocId=" + payload.publicDocId();
    }

    @Override
    protected void onSendFailed(AnalyzeTaskPayload payload, String error) {
        docRepository.findById(payload.publicDocId()).ifPresent(doc -> {
            doc.setAnalyzeStatus(AsyncTaskStatus.FAILED);
            doc.setAnalyzeError(truncateError(error));
            docRepository.save(doc);
        });
    }
}

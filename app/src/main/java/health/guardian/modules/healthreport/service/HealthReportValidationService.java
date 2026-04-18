package health.guardian.modules.healthreport.service;

import health.guardian.common.ai.StructuredOutputInvoker;
import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

/**
 * 体检报告合法性校验服务（同步、轻量级）
 * 在文件存储前调用 AI 判断上传内容是否为体检报告。
 * 校验 AI 本身出错时采用 fail-open 策略，仅记录警告，放行上传。
 */
@Service
public class HealthReportValidationService {

    private static final Logger log = LoggerFactory.getLogger(HealthReportValidationService.class);

    private static final String SYSTEM_PROMPT = """
            你是一位医疗文档鉴别专家。
            你的唯一任务是判断用户提供的文本是否属于体检报告或医疗健康记录。
            体检报告通常包含：血常规、尿常规、生化指标、影像检查、体格检查等医疗数据。
            若文本为简历、文章、代码、新闻、合同等非医疗内容，则判定为无效。
            请仅输出一个 JSON 对象，格式如下：
            {"isValid": true} 或 {"isValid": false}
            不要输出任何解释、Markdown 或额外文字。
            """;

    private static final String USER_PROMPT_TEMPLATE =
            "请判断以下文本是否为体检报告：\n---\n%s\n---";

    // First 3000 chars are enough to determine document type; keeps the sync call fast
    private static final int MAX_TEXT_LENGTH = 3000;

    private record ValidationResult(boolean isValid) {}

    private final ChatClient chatClient;
    private final BeanOutputConverter<ValidationResult> outputConverter;
    private final StructuredOutputInvoker structuredOutputInvoker;

    public HealthReportValidationService(
            ChatClient.Builder chatClientBuilder,
            StructuredOutputInvoker structuredOutputInvoker) {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.outputConverter = new BeanOutputConverter<>(ValidationResult.class);
    }

    /**
     * 校验文本是否来自体检报告。
     * 若 AI 服务异常，采用 fail-open：记录 WARN 并直接返回（放行）。
     *
     * @param reportText 已提取的文件文本内容
     * @throws BusinessException(HEALTH_REPORT_NOT_VALID) 若确认为非体检报告
     */
    public void validateIsHealthReport(String reportText) {
        try {
            String truncated = reportText.length() > MAX_TEXT_LENGTH
                    ? reportText.substring(0, MAX_TEXT_LENGTH)
                    : reportText;

            String systemWithFormat = SYSTEM_PROMPT + "\n\n" + outputConverter.getFormat();
            String userPrompt = USER_PROMPT_TEMPLATE.formatted(truncated);

            ValidationResult result = structuredOutputInvoker.invoke(
                    chatClient,
                    systemWithFormat,
                    userPrompt,
                    outputConverter,
                    ErrorCode.HEALTH_REPORT_NOT_VALID,
                    "体检报告合法性校验失败：",
                    "体检报告合法性校验",
                    log
            );

            if (!result.isValid()) {
                log.warn("AI 判定上传内容不是体检报告，拒绝上传");
                throw new BusinessException(ErrorCode.HEALTH_REPORT_NOT_VALID);
            }

            log.info("AI 合法性校验通过，确认为体检报告");

        } catch (BusinessException e) {
            // Re-throw validation rejection; do NOT swallow in the catch-all below
            throw e;
        } catch (Exception e) {
            // Fail-open: validation AI call itself failed — log and allow upload
            log.warn("体检报告合法性校验 AI 调用异常，采用 fail-open 策略放行: {}", e.getMessage());
        }
    }
}

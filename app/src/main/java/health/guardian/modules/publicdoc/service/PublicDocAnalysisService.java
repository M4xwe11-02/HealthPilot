package health.guardian.modules.publicdoc.service;

import health.guardian.common.ai.StructuredOutputInvoker;
import health.guardian.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PublicDocAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(PublicDocAnalysisService.class);
    private static final int MAX_TEXT_LENGTH = 8000;

    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<PublicDocAnalysisResult> outputConverter;
    private final StructuredOutputInvoker structuredOutputInvoker;

    public record PublicDocAnalysisResult(
        String summary,
        List<String> keyPoints,
        String applicablePopulation,
        List<String> mainRecommendations
    ) {}

    public PublicDocAnalysisService(
            ChatClient.Builder chatClientBuilder,
            StructuredOutputInvoker structuredOutputInvoker,
            @Value("classpath:prompts/publicdoc-analysis-system.st") Resource systemPromptResource,
            @Value("classpath:prompts/publicdoc-analysis-user.st") Resource userPromptResource) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.systemPromptTemplate = new PromptTemplate(systemPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(userPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.outputConverter = new BeanOutputConverter<>(PublicDocAnalysisResult.class);
    }

    public PublicDocAnalysisResult analyze(String docText) {
        String truncated = docText.length() > MAX_TEXT_LENGTH
            ? docText.substring(0, MAX_TEXT_LENGTH)
            : docText;

        String systemPrompt = systemPromptTemplate.render() + "\n\n" + outputConverter.getFormat();
        String userPrompt = userPromptTemplate.render(Map.of("docText", truncated));

        log.info("开始分析公共文档，文本长度: {} 字符", truncated.length());
        return structuredOutputInvoker.invoke(
            chatClient,
            systemPrompt,
            userPrompt,
            outputConverter,
            ErrorCode.PUBLIC_DOC_ANALYSIS_FAILED,
            "公共文档分析失败：",
            "公共文档分析",
            logger
        );
    }
}

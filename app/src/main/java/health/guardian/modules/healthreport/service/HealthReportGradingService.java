package health.guardian.modules.healthreport.service;

import health.guardian.common.ai.StructuredOutputInvoker;
import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.consultation.model.HealthReportAnalysisResponse;
import health.guardian.modules.consultation.model.HealthReportAnalysisResponse.ScoreDetail;
import health.guardian.modules.consultation.model.HealthReportAnalysisResponse.Suggestion;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 体检报告健康评分服务
 * 使用Spring AI调用LLM对体检报告进行评分和建议
 */
@Service
public class HealthReportGradingService {
    
    private static final Logger log = LoggerFactory.getLogger(HealthReportGradingService.class);
    
    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<HealthReportAnalysisResponseDTO> outputConverter;
    private final StructuredOutputInvoker structuredOutputInvoker;
    
    // 中间DTO用于接收AI响应
    private record HealthReportAnalysisResponseDTO(
        int overallScore,
        ScoreDetailDTO scoreDetail,
        String summary,
        List<String> strengths,
        List<SuggestionDTO> suggestions
    ) {}
    
    private record ScoreDetailDTO(
        int lifestyleScore,
        int nutritionScore,
        int physicalFitnessScore,
        int mentalHealthScore,
        int preventiveScore
    ) {}
    
    private record SuggestionDTO(
        String category,
        String priority,
        String issue,
        String recommendation
    ) {}
    
    public HealthReportGradingService(
            ChatClient.Builder chatClientBuilder,
            StructuredOutputInvoker structuredOutputInvoker,
            @Value("classpath:prompts/healthreport-analysis-system.st") Resource systemPromptResource,
            @Value("classpath:prompts/healthreport-analysis-user.st") Resource userPromptResource) throws IOException {
        this.chatClient = chatClientBuilder.build(); // 这个走的是自动配置，没有走直接教程里面说的那种手动配置Client
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.systemPromptTemplate = new PromptTemplate(systemPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(userPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.outputConverter = new BeanOutputConverter<>(HealthReportAnalysisResponseDTO.class);
    }
    
    /**
     * 分析体检报告并返回健康评分和建议
     *
     * @param reportText 体检报告文本内容
     * @return 健康分析结果
     */
    public HealthReportAnalysisResponse analyzeHealthReport(String reportText) {
        log.info("开始分析体检报告，文本长度: {} 字符", reportText.length());
        
        try {
            // 加载系统提示词
            String systemPrompt = systemPromptTemplate.render();
            
            // 加载用户提示词并填充变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("reportText", reportText);
            String userPrompt = userPromptTemplate.render(variables);
            
            // 添加格式指令到系统提示词
            String systemPromptWithFormat = systemPrompt + "\n\n" + outputConverter.getFormat(); // 是一个SpringAI提供的约束AI用的标准的提示词，让AI强制输出json格式的
            
            // 调用AI
            HealthReportAnalysisResponseDTO dto;
            try {
                dto = structuredOutputInvoker.invoke(
                    chatClient,
                    systemPromptWithFormat,
                    userPrompt,
                    outputConverter,
                    ErrorCode.HEALTH_REPORT_ANALYSIS_FAILED,
                    "体检报告健康分析失败：",
                    "体检报告健康分析",
                    log
                );
                log.debug("AI响应解析成功: overallScore={}", dto.overallScore());
            } catch (Exception e) {
                log.error("体检报告健康分析AI调用失败: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.HEALTH_REPORT_ANALYSIS_FAILED, "体检报告健康分析失败：" + e.getMessage());
            }
            
            // 转换为业务对象
            HealthReportAnalysisResponse result = convertToResponse(dto, reportText);
            log.info("体检报告分析完成，健康总分: {}", result.overallScore());
            
            return result;
            
        } catch (Exception e) {
            log.error("体检报告健康分析失败: {}", e.getMessage(), e);
            return createErrorResponse(reportText, e.getMessage());
        }
    }
    
    /**
     * 转换DTO为业务对象
     */
    private HealthReportAnalysisResponse convertToResponse(HealthReportAnalysisResponseDTO dto, String originalText) {
        ScoreDetail scoreDetail = new ScoreDetail(
            dto.scoreDetail().lifestyleScore(),
            dto.scoreDetail().nutritionScore(),
            dto.scoreDetail().physicalFitnessScore(),
            dto.scoreDetail().mentalHealthScore(),
            dto.scoreDetail().preventiveScore()
        );
        
        List<Suggestion> suggestions = dto.suggestions().stream()
            .map(s -> new Suggestion(s.category(), s.priority(), s.issue(), s.recommendation()))
            .toList();
        
        return new HealthReportAnalysisResponse(
            dto.overallScore(),
            scoreDetail,
            dto.summary(),
            dto.strengths(),
            suggestions,
            originalText
        );
    }
    
    /**
     * 创建错误响应
     */
    private HealthReportAnalysisResponse createErrorResponse(String originalText, String errorMessage) {
        return new HealthReportAnalysisResponse(
            0,
            new ScoreDetail(0, 0, 0, 0, 0),
            "分析过程中出现错误: " + errorMessage,
            List.of(),
            List.of(new Suggestion(
                "系统",
                "高",
                "AI健康分析服务暂时不可用",
                "请稍后重试，或检查AI服务是否正常运行"
            )),
            originalText
        );
    }
}

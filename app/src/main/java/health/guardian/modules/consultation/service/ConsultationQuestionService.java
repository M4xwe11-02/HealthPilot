package health.guardian.modules.consultation.service;

import health.guardian.common.ai.StructuredOutputInvoker;
import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.consultation.model.ConsultationQuestionDTO;
import health.guardian.modules.consultation.model.ConsultationQuestionDTO.QuestionType;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 健康问诊问题生成服务
 * 基于体检报告内容生成针对性的健康问诊问题
 */
@Service
public class ConsultationQuestionService {
    
    private static final Logger log = LoggerFactory.getLogger(ConsultationQuestionService.class);
    
    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<QuestionListDTO> outputConverter;
    private final StructuredOutputInvoker structuredOutputInvoker;
    private final int followUpCount;
    
    // 问诊话题权重分配（按优先级）
    private static final double LIFESTYLE_RATIO = 0.25;       // 25% 生活方式
    private static final double NUTRITION_RATIO = 0.15;       // 15% 营养饮食
    private static final double EXERCISE_RATIO = 0.15;        // 15% 运动锻炼
    private static final double SYMPTOM_RATIO = 0.15;         // 15% 症状描述
    private static final double MEDICATION_RATIO = 0.10;      // 10% 用药情况
    private static final double MENTAL_HEALTH_RATIO = 0.10;   // 10% 心理健康
    private static final double SLEEP_RATIO = 0.05;           // 5% 睡眠质量
    private static final int MAX_FOLLOW_UP_COUNT = 2;
    
    // 中间DTO用于接收AI响应
    private record QuestionListDTO(
        List<QuestionDTO> questions
    ) {}
    
    private record QuestionDTO(
        String question,
        String type,
        String category,
        List<String> followUps
    ) {}
    
    public ConsultationQuestionService(
            ChatClient.Builder chatClientBuilder,
            StructuredOutputInvoker structuredOutputInvoker,
            @Value("classpath:prompts/consultation-question-system.st") Resource systemPromptResource,
            @Value("classpath:prompts/consultation-question-user.st") Resource userPromptResource,
            @Value("${app.consultation.follow-up-count:1}") int followUpCount) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.systemPromptTemplate = new PromptTemplate(systemPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(userPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.outputConverter = new BeanOutputConverter<>(QuestionListDTO.class);
        this.followUpCount = Math.max(0, Math.min(followUpCount, MAX_FOLLOW_UP_COUNT));
    }
    
    /**
     * 生成健康问诊问题
     * 
     * @param reportText 体检报告文本
     * @param questionCount 问题数量
     * @param historicalQuestions 历史问题列表（可选）
     * @return 健康问诊问题列表
     */
    public List<ConsultationQuestionDTO> generateQuestions(String reportText, int questionCount, List<String> historicalQuestions) {
        log.info("开始生成健康问诊问题，报告长度: {}, 问题数量: {}, 历史问题数: {}",
            reportText.length(), questionCount, historicalQuestions != null ? historicalQuestions.size() : 0);
        
        // 计算各类型问题数量
        QuestionDistribution distribution = calculateDistribution(questionCount);
        
        try {
            // 加载系统提示词
            String systemPrompt = systemPromptTemplate.render();
            
            // 加载用户提示词并填充变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("questionCount", questionCount);
            variables.put("lifestyleCount", distribution.lifestyle);
            variables.put("nutritionCount", distribution.nutrition);
            variables.put("exerciseCount", distribution.exercise);
            variables.put("symptomCount", distribution.symptom);
            variables.put("medicationCount", distribution.medication);
            variables.put("mentalHealthCount", distribution.mentalHealth);
            variables.put("sleepCount", distribution.sleep);
            variables.put("chronicDiseaseCount", distribution.chronicDisease);
            variables.put("followUpCount", followUpCount);
            variables.put("reportText", reportText);
            
            // 添加历史问题
            if (historicalQuestions != null && !historicalQuestions.isEmpty()) {
                String historicalText = String.join("\n", historicalQuestions);
                variables.put("historicalQuestions", historicalText);
            } else {
                variables.put("historicalQuestions", "暂无历史提问");
            }
            
            String userPrompt = userPromptTemplate.render(variables);
            
            // 添加格式指令到系统提示词
            String systemPromptWithFormat = systemPrompt + "\n\n" + outputConverter.getFormat();
            
            // 调用AI
            QuestionListDTO dto;
            try {
                dto = structuredOutputInvoker.invoke(
                    chatClient,
                    systemPromptWithFormat,
                    userPrompt,
                    outputConverter,
                    ErrorCode.CONSULTATION_QUESTION_GENERATION_FAILED,
                    "健康问诊问题生成失败：",
                    "结构化问诊问题生成",
                    log
                );
                log.debug("AI响应解析成功: questions count={}", dto.questions().size());
            } catch (Exception e) {
                log.error("健康问诊问题生成AI调用失败: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.CONSULTATION_QUESTION_GENERATION_FAILED, 
                    "健康问诊问题生成失败：" + e.getMessage());
            }
            
            // 转换为业务对象
            List<ConsultationQuestionDTO> questions = convertToQuestions(dto);
            log.info("成功生成 {} 个健康问诊问题", questions.size());
            
            return questions;
            
        } catch (Exception e) {
            log.error("生成健康问诊问题失败: {}", e.getMessage(), e);
            // 返回默认问题集
            return generateDefaultQuestions(questionCount);
        }
    }

    /**
     * 生成健康问诊问题（不带历史问题）
     */
    public List<ConsultationQuestionDTO> generateQuestions(String reportText, int questionCount) {
        return generateQuestions(reportText, questionCount, null);
    }
    
    /**
     * 计算各类型问题分布
     */
    private QuestionDistribution calculateDistribution(int total) {
        int lifestyle = Math.max(1, (int) Math.round(total * LIFESTYLE_RATIO));
        int nutrition = Math.max(1, (int) Math.round(total * NUTRITION_RATIO));
        int exercise = Math.max(1, (int) Math.round(total * EXERCISE_RATIO));
        int symptom = Math.max(1, (int) Math.round(total * SYMPTOM_RATIO));
        int medication = (int) Math.round(total * MEDICATION_RATIO);
        int mentalHealth = (int) Math.round(total * MENTAL_HEALTH_RATIO);
        int sleep = (int) Math.round(total * SLEEP_RATIO);
        int chronicDisease = total - lifestyle - nutrition - exercise - symptom - medication - mentalHealth - sleep;
        
        // 确保至少有1个
        chronicDisease = Math.max(0, chronicDisease);
        
        return new QuestionDistribution(lifestyle, nutrition, exercise, symptom, medication, mentalHealth, sleep, chronicDisease);
    }
    
    private record QuestionDistribution(
        int lifestyle,
        int nutrition,
        int exercise,
        int symptom,
        int medication,
        int mentalHealth,
        int sleep,
        int chronicDisease
    ) {}
    
    /**
     * 转换DTO为业务对象
     */
    private List<ConsultationQuestionDTO> convertToQuestions(QuestionListDTO dto) {
        List<ConsultationQuestionDTO> questions = new ArrayList<>();
        int index = 0;

        if (dto == null || dto.questions() == null) {
            return questions;
        }

        for (QuestionDTO q : dto.questions()) {
            if (q == null || q.question() == null || q.question().isBlank()) {
                continue;
            }
            QuestionType type = parseQuestionType(q.type());
            int mainQuestionIndex = index;
            questions.add(ConsultationQuestionDTO.create(index++, q.question(), type, q.category(), false, null));

            List<String> followUps = sanitizeFollowUps(q.followUps());
            for (int i = 0; i < followUps.size(); i++) {
                questions.add(ConsultationQuestionDTO.create(
                    index++,
                    followUps.get(i),
                    type,
                    buildFollowUpCategory(q.category(), i + 1),
                    true,
                    mainQuestionIndex
                ));
            }
        }
        
        return questions;
    }
    
    private QuestionType parseQuestionType(String typeStr) {
        try {
            return QuestionType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            return QuestionType.SYMPTOM;
        }
    }
    
    /**
     * 生成默认问题（备用）
     */
    private List<ConsultationQuestionDTO> generateDefaultQuestions(int count) {
        List<ConsultationQuestionDTO> questions = new ArrayList<>();
        
        String[][] defaultQuestions = {
            {"请描述一下您平时的作息规律，通常几点睡觉、几点起床？", "LIFESTYLE", "生活方式 - 作息规律"},
            {"您平时的饮食习惯如何？是否以清淡为主，每天蔬菜水果的摄入量大概有多少？", "NUTRITION", "营养饮食 - 饮食结构"},
            {"您目前有规律锻炼的习惯吗？通常做什么运动，频率和时长是多少？", "EXERCISE", "运动锻炼 - 运动习惯"},
            {"最近一段时间有没有感到明显的身体不适，比如头晕、胸闷、乏力等症状？", "SYMPTOM", "症状描述 - 主观不适"},
            {"您目前是否在服用任何药物或保健品？如果有，请描述一下。", "MEDICATION", "用药情况 - 当前用药"},
            {"您最近的情绪状态和心理压力如何？工作或生活压力是否较大？", "MENTAL_HEALTH", "心理健康 - 情绪状态"},
            {"您的睡眠质量如何？是否容易入睡，晚上是否会有多次醒来的情况？", "SLEEP", "睡眠质量 - 睡眠状况"},
            {"您或您的直系亲属是否有高血压、糖尿病、心脏病等慢性疾病的家族史？", "CHRONIC_DISEASE", "慢性病管理 - 家族遗传史"},
            {"您抽烟吗？如果有，每天的吸烟量大概是多少？", "LIFESTYLE", "生活方式 - 不良习惯"},
            {"您每天的饮水量大概有多少？通常喝什么类型的饮料？", "NUTRITION", "营养饮食 - 水分摄入"},
        };
        
        int index = 0;
        for (int i = 0; i < Math.min(count, defaultQuestions.length); i++) {
            String mainQuestion = defaultQuestions[i][0];
            QuestionType type = QuestionType.valueOf(defaultQuestions[i][1]);
            String category = defaultQuestions[i][2];
            questions.add(ConsultationQuestionDTO.create(
                index++,
                mainQuestion,
                type,
                category,
                false,
                null
            ));

            int mainQuestionIndex = index - 1;
            for (int j = 0; j < followUpCount; j++) {
                questions.add(ConsultationQuestionDTO.create(
                    index++,
                    buildDefaultFollowUp(mainQuestion, j + 1),
                    type,
                    buildFollowUpCategory(category, j + 1),
                    true,
                    mainQuestionIndex
                ));
            }
        }
        
        return questions;
    }

    private List<String> sanitizeFollowUps(List<String> followUps) {
        if (followUpCount == 0 || followUps == null || followUps.isEmpty()) {
            return List.of();
        }
        return followUps.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .limit(followUpCount)
            .collect(Collectors.toList());
    }

    private String buildFollowUpCategory(String category, int order) {
        String baseCategory = (category == null || category.isBlank()) ? "追问" : category;
        return baseCategory + "（追问" + order + "）";
    }

    private String buildDefaultFollowUp(String mainQuestion, int order) {
        if (order == 1) {
            return "针对\"" + mainQuestion + "\"这个问题，能否具体描述一下您近期的实际情况？";
        }
        return "针对\"" + mainQuestion + "\"，如果需要做出改变，您觉得哪方面最难坚持，为什么？";
    }
}

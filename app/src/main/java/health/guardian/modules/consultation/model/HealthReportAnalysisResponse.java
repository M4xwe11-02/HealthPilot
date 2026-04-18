package health.guardian.modules.consultation.model;

import java.util.List;

/**
 * 健康分析响应DTO
 */
public record HealthReportAnalysisResponse(
    // 总分 (0-100)
    int overallScore,
    
    // 各维度评分
    ScoreDetail scoreDetail,
    
    // 体检报告摘要
    String summary,
    
    // 优点列表
    List<String> strengths,
    
    // 改进建议列表
    List<Suggestion> suggestions,
    
    // 原始体检报告文本
    String originalText
) {
    
    /**
     * 各维度评分详情
     */
    public record ScoreDetail(
        int lifestyleScore,
        int nutritionScore,
        int physicalFitnessScore,
        int mentalHealthScore,
        int preventiveScore
    ) {}
    
    /**
     * 改进建议
     */
    public record Suggestion(
        String category,        // 建议类别：内容、格式、技能、项目等
        String priority,        // 优先级：高、中、低
        String issue,           // 问题描述
        String recommendation   // 具体建议
    ) {}
}

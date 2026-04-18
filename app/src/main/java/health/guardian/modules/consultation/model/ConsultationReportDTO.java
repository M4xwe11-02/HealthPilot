package health.guardian.modules.consultation.model;

import java.util.List;

/**
 * 问诊评估报告
 */
public record ConsultationReportDTO(
    String sessionId,
    int totalQuestions,
    int overallScore,                          // 总分 (0-100)
    List<CategoryScore> categoryScores,        // 各类别得分
    List<QuestionEvaluation> questionDetails,  // 每题详情
    String overallFeedback,                    // 总体评价
    List<String> strengths,                    // 优势
    List<String> improvements,                 // 改进建议
    List<ReferenceAnswer> referenceAnswers     // AI健康建议
) {
    /**
     * 类别得分
     */
    public record CategoryScore(
        String category,
        int score,
        int questionCount
    ) {}
    
    /**
     * 问题评估详情
     */
    public record QuestionEvaluation(
        int questionIndex,
        String question,
        String category,
        String userAnswer,
        int score,
        String feedback
    ) {}
    
    /**
     * AI健康建议
     */
    public record ReferenceAnswer(
        int questionIndex,
        String question,
        String referenceAnswer,
        List<String> keyPoints
    ) {}
}

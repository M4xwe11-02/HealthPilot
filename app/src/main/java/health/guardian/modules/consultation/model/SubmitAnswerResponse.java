package health.guardian.modules.consultation.model;

/**
 * 提交答案响应
 */
public record SubmitAnswerResponse(
    boolean hasNextQuestion,
    ConsultationQuestionDTO nextQuestion,
    int currentIndex,
    int totalQuestions
) {}

package health.guardian.modules.consultation.model;

/**
 * 健康问诊问题DTO
 */
public record ConsultationQuestionDTO(
    int questionIndex,
    String question,
    QuestionType type,
    String category,      // 问题类别：生活方式、症状、用药、心理、营养、运动、睡眠、慢病
    String userAnswer,    // 用户回答
    Integer score,        // 单题得分 (0-100)
    String feedback,      // 单题反馈
    boolean isFollowUp,   // 是否为追问
    Integer parentQuestionIndex // 追问关联的主问题索引
) {
    public enum QuestionType {
        LIFESTYLE,        // 生活方式
        SYMPTOM,          // 症状描述
        MEDICATION,       // 用药情况
        MENTAL_HEALTH,    // 心理健康
        NUTRITION,        // 营养饮食
        EXERCISE,         // 运动锻炼
        SLEEP,            // 睡眠质量
        CHRONIC_DISEASE   // 慢性病管理
    }
    
    /**
     * 创建新问诊问题（未回答状态）
     */
    public static ConsultationQuestionDTO create(int index, String question, QuestionType type, String category) {
        return new ConsultationQuestionDTO(index, question, type, category, null, null, null, false, null);
    }

    /**
     * 创建新问诊问题（支持追问标记）
     */
    public static ConsultationQuestionDTO create(
            int index,
            String question,
            QuestionType type,
            String category,
            boolean isFollowUp,
            Integer parentQuestionIndex) {
        return new ConsultationQuestionDTO(index, question, type, category, null, null, null, isFollowUp, parentQuestionIndex);
    }
    
    /**
     * 添加用户回答
     */
    public ConsultationQuestionDTO withAnswer(String answer) {
        return new ConsultationQuestionDTO(
            questionIndex, question, type, category, answer, score, feedback, isFollowUp, parentQuestionIndex);
    }
    
    /**
     * 添加评分和反馈
     */
    public ConsultationQuestionDTO withEvaluation(int score, String feedback) {
        return new ConsultationQuestionDTO(
            questionIndex, question, type, category, userAnswer, score, feedback, isFollowUp, parentQuestionIndex);
    }
}

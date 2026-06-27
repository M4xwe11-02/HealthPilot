package health.guardian.modules.consultation.model;

import health.guardian.common.model.AsyncTaskStatus;
import health.guardian.modules.auth.model.UserEntity;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 问诊会话实体
 */
@Entity
@Table(name = "consultation_sessions", indexes = {
    @Index(name = "idx_consultation_session_owner_created", columnList = "owner_id,created_at"),
    @Index(name = "idx_consultation_session_health_report_created", columnList = "health_report_id,created_at"),
    @Index(name = "idx_consultation_session_health_report_status_created", columnList = "health_report_id,status,created_at")
})
public class ConsultationSessionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 会话ID (UUID)
    @Column(nullable = false, unique = true, length = 36)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;
    
    // 关联的体检报告
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_report_id", nullable = false)
    private HealthReportEntity healthReport;
    
    // 问题总数
    private Integer totalQuestions;
    
    // 当前问题索引
    private Integer currentQuestionIndex = 0;
    
    // 会话状态
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SessionStatus status = SessionStatus.CREATED;
    
    // 问题列表 (JSON格式)
    @Column(columnDefinition = "TEXT")
    private String questionsJson;
    
    // 总分 (0-100)
    private Integer overallScore;
    
    // 总体评价
    @Column(columnDefinition = "TEXT")
    private String overallFeedback;
    
    // 优势 (JSON)
    @Column(columnDefinition = "TEXT")
    private String strengthsJson;
    
    // 改进建议 (JSON)
    @Column(columnDefinition = "TEXT")
    private String improvementsJson;
    
    // AI健康建议 (JSON)
    @Column(columnDefinition = "TEXT")
    private String referenceAnswersJson;
    
    // 问诊答案记录
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsultationAnswerEntity> answers = new ArrayList<>();
    
    // 创建时间
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    // 完成时间
    private LocalDateTime completedAt;

    // 评估状态（异步评估）
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AsyncTaskStatus evaluateStatus;

    // 评估错误信息
    @Column(length = 500)
    private String evaluateError;
    
    public enum SessionStatus {
        CREATED,      // 会话已创建
        IN_PROGRESS,  // 问诊进行中
        COMPLETED,    // 问诊已完成
        EVALUATED     // 已生成评估报告
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }
    
    public HealthReportEntity getHealthReport() {
        return healthReport;
    }
    
    public void setHealthReport(HealthReportEntity healthReport) {
        this.healthReport = healthReport;
    }
    
    public Integer getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
    
    public Integer getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }
    
    public void setCurrentQuestionIndex(Integer currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }
    
    public SessionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SessionStatus status) {
        this.status = status;
    }
    
    public String getQuestionsJson() {
        return questionsJson;
    }
    
    public void setQuestionsJson(String questionsJson) {
        this.questionsJson = questionsJson;
    }
    
    public Integer getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }
    
    public String getOverallFeedback() {
        return overallFeedback;
    }
    
    public void setOverallFeedback(String overallFeedback) {
        this.overallFeedback = overallFeedback;
    }
    
    public String getStrengthsJson() {
        return strengthsJson;
    }
    
    public void setStrengthsJson(String strengthsJson) {
        this.strengthsJson = strengthsJson;
    }
    
    public String getImprovementsJson() {
        return improvementsJson;
    }
    
    public void setImprovementsJson(String improvementsJson) {
        this.improvementsJson = improvementsJson;
    }
    
    public String getReferenceAnswersJson() {
        return referenceAnswersJson;
    }
    
    public void setReferenceAnswersJson(String referenceAnswersJson) {
        this.referenceAnswersJson = referenceAnswersJson;
    }
    
    public List<ConsultationAnswerEntity> getAnswers() {
        return answers;
    }
    
    public void setAnswers(List<ConsultationAnswerEntity> answers) {
        this.answers = answers;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public AsyncTaskStatus getEvaluateStatus() {
        return evaluateStatus;
    }

    public void setEvaluateStatus(AsyncTaskStatus evaluateStatus) {
        this.evaluateStatus = evaluateStatus;
    }

    public String getEvaluateError() {
        return evaluateError;
    }

    public void setEvaluateError(String evaluateError) {
        this.evaluateError = evaluateError;
    }

    public void addAnswer(ConsultationAnswerEntity answer) {
        answers.add(answer);
        answer.setSession(this);
    }
}

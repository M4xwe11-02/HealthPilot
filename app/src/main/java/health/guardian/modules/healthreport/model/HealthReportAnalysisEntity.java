package health.guardian.modules.healthreport.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 健康分析结果实体
 * Health Report Analysis Entity
 */
@Entity
@Table(name = "health_report_analyses")
public class HealthReportAnalysisEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 关联的体检报告
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_report_id", nullable = false)
    private HealthReportEntity healthReport;
    
    // 总分 (0-100)
    private Integer overallScore;
    
    // 各维度评分
    private Integer lifestyleScore;
    private Integer nutritionScore;
    private Integer physicalFitnessScore;
    private Integer mentalHealthScore;
    private Integer preventiveScore;
    
    // 体检报告摘要
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    // 优点列表 (JSON格式)
    @Column(columnDefinition = "TEXT")
    private String strengthsJson;
    
    // 改进建议列表 (JSON格式)
    @Column(columnDefinition = "TEXT")
    private String suggestionsJson;
    
    // 评测时间
    @Column(nullable = false)
    private LocalDateTime analyzedAt;
    
    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public HealthReportEntity getHealthReport() {
        return healthReport;
    }
    
    public void setHealthReport(HealthReportEntity healthReport) {
        this.healthReport = healthReport;
    }
    
    public Integer getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }
    
    public Integer getLifestyleScore() {
        return lifestyleScore;
    }
    
    public void setLifestyleScore(Integer lifestyleScore) {
        this.lifestyleScore = lifestyleScore;
    }
    
    public Integer getNutritionScore() {
        return nutritionScore;
    }
    
    public void setNutritionScore(Integer nutritionScore) {
        this.nutritionScore = nutritionScore;
    }
    
    public Integer getPhysicalFitnessScore() {
        return physicalFitnessScore;
    }
    
    public void setPhysicalFitnessScore(Integer physicalFitnessScore) {
        this.physicalFitnessScore = physicalFitnessScore;
    }
    
    public Integer getMentalHealthScore() {
        return mentalHealthScore;
    }
    
    public void setMentalHealthScore(Integer mentalHealthScore) {
        this.mentalHealthScore = mentalHealthScore;
    }
    
    public Integer getPreventiveScore() {
        return preventiveScore;
    }
    
    public void setPreventiveScore(Integer preventiveScore) {
        this.preventiveScore = preventiveScore;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getStrengthsJson() {
        return strengthsJson;
    }
    
    public void setStrengthsJson(String strengthsJson) {
        this.strengthsJson = strengthsJson;
    }
    
    public String getSuggestionsJson() {
        return suggestionsJson;
    }
    
    public void setSuggestionsJson(String suggestionsJson) {
        this.suggestionsJson = suggestionsJson;
    }
    
    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }
    
    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}

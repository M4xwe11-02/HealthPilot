package health.guardian.modules.publicdoc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "public_doc_analyses", indexes = {
    @Index(name = "idx_public_doc_analysis_doc_id", columnList = "publicDocId")
})
public class PublicDocAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long publicDocId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String keyPointsJson;

    @Column(length = 500)
    private String applicablePopulation;

    @Column(columnDefinition = "TEXT")
    private String mainRecommendationsJson;

    @Column(nullable = false, updatable = false)
    private Instant analyzedAt;

    @PrePersist
    protected void onCreate() {
        analyzedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPublicDocId() { return publicDocId; }
    public void setPublicDocId(Long publicDocId) { this.publicDocId = publicDocId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getKeyPointsJson() { return keyPointsJson; }
    public void setKeyPointsJson(String keyPointsJson) { this.keyPointsJson = keyPointsJson; }

    public String getApplicablePopulation() { return applicablePopulation; }
    public void setApplicablePopulation(String applicablePopulation) { this.applicablePopulation = applicablePopulation; }

    public String getMainRecommendationsJson() { return mainRecommendationsJson; }
    public void setMainRecommendationsJson(String mainRecommendationsJson) { this.mainRecommendationsJson = mainRecommendationsJson; }

    public Instant getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(Instant analyzedAt) { this.analyzedAt = analyzedAt; }
}

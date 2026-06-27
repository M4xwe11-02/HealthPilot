package health.guardian.modules.auth.service;

import health.guardian.modules.auth.model.UserEntity;
import health.guardian.modules.consultation.repository.ConsultationSessionRepository;
import health.guardian.modules.knowledgebase.repository.KnowledgeBaseRepository;
import health.guardian.modules.knowledgebase.repository.RagChatSessionRepository;
import health.guardian.modules.healthreport.repository.HealthReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnershipMigrationService {

    private final HealthReportRepository healthReportRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ConsultationSessionRepository consultationSessionRepository;
    private final RagChatSessionRepository ragChatSessionRepository;

    @Transactional
    public void claimUnownedData(UserEntity owner) {
        var healthReports = healthReportRepository.findByOwnerIsNull();
        healthReports.forEach(healthReport -> healthReport.setOwner(owner));
        healthReportRepository.saveAll(healthReports);

        var knowledgeBases = knowledgeBaseRepository.findByOwnerIsNull();
        knowledgeBases.forEach(kb -> kb.setOwner(owner));
        knowledgeBaseRepository.saveAll(knowledgeBases);

        var consultationSessions = consultationSessionRepository.findByOwnerIsNull();
        consultationSessions.forEach(session -> session.setOwner(owner));
        consultationSessionRepository.saveAll(consultationSessions);

        var ragSessions = ragChatSessionRepository.findByOwnerIsNull();
        ragSessions.forEach(session -> session.setOwner(owner));
        ragChatSessionRepository.saveAll(ragSessions);

        log.info("首个用户已接管旧数据: userId={}, healthReports={}, knowledgeBases={}, consultations={}, ragSessions={}",
            owner.getId(), healthReports.size(), knowledgeBases.size(), consultationSessions.size(), ragSessions.size());
    }
}

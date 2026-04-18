package health.guardian.modules.consultation.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.common.model.AsyncTaskStatus;
import health.guardian.modules.auth.service.CurrentUserService;
import health.guardian.modules.consultation.model.ConsultationAnswerEntity;
import health.guardian.modules.consultation.model.ConsultationQuestionDTO;
import health.guardian.modules.consultation.model.ConsultationReportDTO;
import health.guardian.modules.consultation.model.ConsultationSessionEntity;
import health.guardian.modules.consultation.repository.ConsultationAnswerRepository;
import health.guardian.modules.consultation.repository.ConsultationSessionRepository;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import health.guardian.modules.healthreport.repository.HealthReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 健康问诊持久化服务
 * 问诊会话和答案的持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationPersistenceService {
    
    private final ConsultationSessionRepository sessionRepository;
    private final ConsultationAnswerRepository answerRepository;
    private final HealthReportRepository healthReportRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    
    /**
     * 保存新的问诊会话
     */
    @Transactional(rollbackFor = Exception.class)
    public ConsultationSessionEntity saveSession(String sessionId, Long healthReportId, 
                                              int totalQuestions, 
                                              List<ConsultationQuestionDTO> questions) {
        try {
            Long ownerId = currentUserService.requireCurrentUserId();
            Optional<HealthReportEntity> healthReportOpt = healthReportRepository.findByIdAndOwner_Id(healthReportId, ownerId);
            if (healthReportOpt.isEmpty()) {
                throw new BusinessException(ErrorCode.HEALTH_REPORT_NOT_FOUND);
            }
            
            ConsultationSessionEntity session = new ConsultationSessionEntity();
            session.setSessionId(sessionId);
            session.setHealthReport(healthReportOpt.get());
            session.setOwner(currentUserService.requireCurrentUserReference());
            session.setTotalQuestions(totalQuestions);
            session.setCurrentQuestionIndex(0);
            session.setStatus(ConsultationSessionEntity.SessionStatus.CREATED);
            session.setQuestionsJson(objectMapper.writeValueAsString(questions));
            
            ConsultationSessionEntity saved = sessionRepository.save(session);
            log.info("问诊会话已保存: sessionId={}, healthReportId={}", sessionId, healthReportId);
            
            return saved;
        } catch (JacksonException e) {
            log.error("序列化问题列表失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存会话失败");
        }
    }
    
    /**
     * 更新会话状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSessionStatus(String sessionId, ConsultationSessionEntity.SessionStatus status) {
        Optional<ConsultationSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            ConsultationSessionEntity session = sessionOpt.get();
            session.setStatus(status);
            if (status == ConsultationSessionEntity.SessionStatus.COMPLETED ||
                status == ConsultationSessionEntity.SessionStatus.EVALUATED) {
                session.setCompletedAt(LocalDateTime.now());
            }
            sessionRepository.save(session);
        }
    }

    /**
     * 更新评估状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateEvaluateStatus(String sessionId, AsyncTaskStatus status, String error) {
        Optional<ConsultationSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            ConsultationSessionEntity session = sessionOpt.get();
            session.setEvaluateStatus(status);
            if (error != null) {
                session.setEvaluateError(error.length() > 500 ? error.substring(0, 500) : error);
            } else {
                session.setEvaluateError(null);
            }
            sessionRepository.save(session);
            log.debug("评估状态已更新: sessionId={}, status={}", sessionId, status);
        }
    }
    
    /**
     * 更新当前问题索引
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentQuestionIndex(String sessionId, int index) {
        Optional<ConsultationSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            ConsultationSessionEntity session = sessionOpt.get();
            session.setCurrentQuestionIndex(index);
            session.setStatus(ConsultationSessionEntity.SessionStatus.IN_PROGRESS);
            sessionRepository.save(session);
        }
    }
    
    /**
     * 保存问诊答案
     */
    @Transactional(rollbackFor = Exception.class)
    public ConsultationAnswerEntity saveAnswer(String sessionId, int questionIndex,
                                            String question, String category,
                                            String userAnswer, int score, String feedback) {
        Optional<ConsultationSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.CONSULTATION_SESSION_NOT_FOUND);
        }

        ConsultationAnswerEntity answer = answerRepository
            .findBySession_SessionIdAndQuestionIndex(sessionId, questionIndex)
            .orElseGet(() -> {
                ConsultationAnswerEntity created = new ConsultationAnswerEntity();
                created.setSession(sessionOpt.get());
                created.setQuestionIndex(questionIndex);
                return created;
            });

        answer.setQuestion(question);
        answer.setCategory(category);
        answer.setUserAnswer(userAnswer);
        answer.setScore(score);
        answer.setFeedback(feedback);

        ConsultationAnswerEntity saved = answerRepository.save(answer);
        log.info("问诊答案已保存: sessionId={}, questionIndex={}, score={}",
                sessionId, questionIndex, score);
        
        return saved;
    }
    
    /**
     * 保存问诊报告
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveReport(String sessionId, ConsultationReportDTO report) {
        try {
            Optional<ConsultationSessionEntity> sessionOpt = sessionRepository.findBySessionId(sessionId);
            if (sessionOpt.isEmpty()) {
                log.warn("会话不存在: {}", sessionId);
                return;
            }

            ConsultationSessionEntity session = sessionOpt.get();
            session.setOverallScore(report.overallScore());
            session.setOverallFeedback(report.overallFeedback());
            session.setStrengthsJson(objectMapper.writeValueAsString(report.strengths()));
            session.setImprovementsJson(objectMapper.writeValueAsString(report.improvements()));
            session.setReferenceAnswersJson(objectMapper.writeValueAsString(report.referenceAnswers()));
            session.setStatus(ConsultationSessionEntity.SessionStatus.EVALUATED);
            session.setCompletedAt(LocalDateTime.now());

            sessionRepository.save(session);

            // 查询已存在的答案，建立索引
            List<ConsultationAnswerEntity> existingAnswers = answerRepository.findBySession_SessionIdOrderByQuestionIndex(sessionId);
            java.util.Map<Integer, ConsultationAnswerEntity> answerMap = existingAnswers.stream()
                .collect(java.util.stream.Collectors.toMap(
                    ConsultationAnswerEntity::getQuestionIndex,
                    a -> a,
                    (a1, a2) -> a1
                ));

            // 建立AI健康建议索引
            java.util.Map<Integer, ConsultationReportDTO.ReferenceAnswer> refAnswerMap = report.referenceAnswers().stream()
                .collect(java.util.stream.Collectors.toMap(
                    ConsultationReportDTO.ReferenceAnswer::questionIndex,
                    r -> r,
                    (r1, r2) -> r1
                ));

            List<ConsultationAnswerEntity> answersToSave = new java.util.ArrayList<>();

            // 遍历所有评估结果，更新或创建AI健康建议记录
            for (ConsultationReportDTO.QuestionEvaluation eval : report.questionDetails()) {
                ConsultationAnswerEntity answer = answerMap.get(eval.questionIndex());

                if (answer == null) {
                    // 未回答的题目，创建新记录
                    answer = new ConsultationAnswerEntity();
                    answer.setSession(session);
                    answer.setQuestionIndex(eval.questionIndex());
                    answer.setQuestion(eval.question());
                    answer.setCategory(eval.category());
                    answer.setUserAnswer(null);  // 未回答
                    log.debug("为未回答的题目 {} 创建答案记录", eval.questionIndex());
                }

                // 更新评分和反馈
                answer.setScore(eval.score());
                answer.setFeedback(eval.feedback());

                // 设置AI健康建议和关键点
                ConsultationReportDTO.ReferenceAnswer refAns = refAnswerMap.get(eval.questionIndex());
                if (refAns != null) {
                    answer.setReferenceAnswer(refAns.referenceAnswer());
                    if (refAns.keyPoints() != null && !refAns.keyPoints().isEmpty()) {
                        answer.setKeyPointsJson(objectMapper.writeValueAsString(refAns.keyPoints()));
                    }
                }

                answersToSave.add(answer);
            }

            answerRepository.saveAll(answersToSave);
            log.info("问诊报告已保存: sessionId={}, score={}, 答案数={}",
                sessionId, report.overallScore(), answersToSave.size());

        } catch (JacksonException e) {
            log.error("序列化报告失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 根据会话ID获取会话
     */
    public Optional<ConsultationSessionEntity> findBySessionId(String sessionId) {
        return sessionRepository.findBySessionIdAndOwner_Id(sessionId, currentUserService.requireCurrentUserId());
    }

    public void assertCurrentUserOwnsSession(String sessionId) {
        if (!sessionRepository.existsBySessionIdAndOwner_Id(sessionId, currentUserService.requireCurrentUserId())) {
            throw new BusinessException(ErrorCode.CONSULTATION_SESSION_NOT_FOUND);
        }
    }

    public void assertCurrentUserOwnsHealthReport(Long healthReportId) {
        if (!healthReportRepository.existsByIdAndOwner_Id(healthReportId, currentUserService.requireCurrentUserId())) {
            throw new BusinessException(ErrorCode.HEALTH_REPORT_NOT_FOUND);
        }
    }
    
    /**
     * 获取体检报告的所有问诊记录
     */
    public List<ConsultationSessionEntity> findByHealthReportId(Long healthReportId) {
        return sessionRepository.findByHealthReportIdAndOwner_IdOrderByCreatedAtDesc(healthReportId, currentUserService.requireCurrentUserId());
    }
    
    /**
     * 删除体检报告的所有问诊会话
     * 由于ConsultationSessionEntity设置了cascade = CascadeType.ALL, orphanRemoval = true
     * 删除会话会自动删除关联的答案
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSessionsByHealthReportId(Long healthReportId) {
        List<ConsultationSessionEntity> sessions = sessionRepository.findByHealthReportIdAndOwner_IdOrderByCreatedAtDesc(healthReportId, currentUserService.requireCurrentUserId());
        if (!sessions.isEmpty()) {
            sessionRepository.deleteAll(sessions);
            log.info("已删除 {} 个问诊会话（包含所有答案）", sessions.size());
        }
    }
    
    /**
     * 删除单个问诊会话
     * 由于ConsultationSessionEntity设置了cascade = CascadeType.ALL, orphanRemoval = true
     * 删除会话会自动删除关联的答案
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSessionBySessionId(String sessionId) {
        Optional<ConsultationSessionEntity> sessionOpt = sessionRepository.findBySessionIdAndOwner_Id(sessionId, currentUserService.requireCurrentUserId());
        if (sessionOpt.isPresent()) {
            sessionRepository.delete(sessionOpt.get());
            log.info("已删除问诊会话: sessionId={}", sessionId);
        } else {
            throw new BusinessException(ErrorCode.CONSULTATION_SESSION_NOT_FOUND);
        }
    }
    
    /**
     * 查找未完成的问诊会话（CREATED或IN_PROGRESS状态）
     */
    public Optional<ConsultationSessionEntity> findUnfinishedSession(Long healthReportId) {
        List<ConsultationSessionEntity.SessionStatus> unfinishedStatuses = List.of(
            ConsultationSessionEntity.SessionStatus.CREATED,
            ConsultationSessionEntity.SessionStatus.IN_PROGRESS
        );
        return sessionRepository.findFirstByHealthReportIdAndOwner_IdAndStatusInOrderByCreatedAtDesc(
            healthReportId,
            currentUserService.requireCurrentUserId(),
            unfinishedStatuses
        );
    }
    
    /**
     * 根据会话ID查找所有答案
     */
    public List<ConsultationAnswerEntity> findAnswersBySessionId(String sessionId) {
        return answerRepository.findBySession_SessionIdOrderByQuestionIndex(sessionId);
    }

    /**
     * 获取体检报告的历史提问列表（限制最近的 N 条）
     */
    public List<String> getHistoricalQuestionsByHealthReportId(Long healthReportId) {
        // 只查询最近的 10 个会话，避免加载过多历史数据
        List<ConsultationSessionEntity> sessions = sessionRepository.findTop10ByHealthReportIdAndOwner_IdOrderByCreatedAtDesc(
            healthReportId,
            currentUserService.requireCurrentUserId()
        );
        
        return sessions.stream()
            .map(ConsultationSessionEntity::getQuestionsJson)
            .filter(json -> json != null && !json.isEmpty())
            .flatMap(json -> {
                try {
                    List<ConsultationQuestionDTO> questions = objectMapper.readValue(json, 
                        new TypeReference<List<ConsultationQuestionDTO>>() {});
                    // 过滤掉追问，只保留主问题作为历史参考
                    return questions.stream()
                        .filter(q -> !q.isFollowUp())
                        .map(ConsultationQuestionDTO::question);
                } catch (Exception e) {
                    log.error("解析历史问题JSON失败", e);
                    return java.util.stream.Stream.empty();
                }
            })
            .distinct()
            .limit(30) // 核心改动：只保留最近的 30 道题
            .toList();
    }
}

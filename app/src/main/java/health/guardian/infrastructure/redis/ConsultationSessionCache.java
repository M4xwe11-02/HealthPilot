package health.guardian.infrastructure.redis;

import health.guardian.modules.consultation.model.ConsultationQuestionDTO;
import health.guardian.modules.consultation.model.ConsultationSessionDTO.SessionStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 问诊会话 Redis 缓存服务
 * 管理问诊会话在 Redis 中的存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationSessionCache {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    /**
     * 缓存键前缀
     */
    private static final String SESSION_KEY_PREFIX = "consultation:session:";

    /**
     * 体检报告ID到会话ID的映射前缀（用于查找未完成会话）
     */
    private static final String HEALTH_REPORT_SESSION_KEY_PREFIX = "health:healthreport:";

    /**
     * 会话默认过期时间（24小时）
     */
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    /**
     * 缓存的会话数据
     */
    @Data
    public static class CachedSession implements Serializable {
        private String sessionId;
        private String reportText;
        private Long healthReportId;
        private String questionsJson;  // 序列化的问题列表
        private int currentIndex;
        private SessionStatus status;

        public CachedSession() {
        }

        public CachedSession(String sessionId, String reportText, Long healthReportId,
                            List<ConsultationQuestionDTO> questions, int currentIndex,
                            SessionStatus status, ObjectMapper objectMapper) {
            this.sessionId = sessionId;
            this.reportText = reportText;
            this.healthReportId = healthReportId;
            this.currentIndex = currentIndex;
            this.status = status;
            try {
                this.questionsJson = objectMapper.writeValueAsString(questions);
            } catch (JacksonException e) {
                throw new RuntimeException("序列化问题列表失败", e);
            }
        }

        public List<ConsultationQuestionDTO> getQuestions(ObjectMapper objectMapper) {
            try {
                return objectMapper.readValue(questionsJson, new TypeReference<>() {});
            } catch (JacksonException e) {
                throw new RuntimeException("反序列化问题列表失败", e);
            }
        }
    }

    /**
     * 保存会话到缓存
     */
    public void saveSession(String sessionId, String reportText, Long healthReportId,
                           List<ConsultationQuestionDTO> questions, int currentIndex,
                           SessionStatus status) {
        String key = buildSessionKey(sessionId);
        CachedSession cachedSession = new CachedSession(
            sessionId, reportText, healthReportId, questions, currentIndex, status, objectMapper
        );

        redisService.set(key, cachedSession, SESSION_TTL);

        // 如果有 healthReportId，建立映射关系（用于查找未完成会话）
        if (healthReportId != null && isUnfinishedStatus(status)) {
            saveHealthReportSessionMapping(healthReportId, sessionId);
        }

        log.debug("会话已缓存: sessionId={}, healthReportId={}, status={}", sessionId, healthReportId, status);
    }

    /**
     * 获取缓存的会话
     */
    public Optional<CachedSession> getSession(String sessionId) {
        String key = buildSessionKey(sessionId);
        CachedSession session = redisService.get(key);
        if (session != null) {
            log.debug("从缓存获取会话: sessionId={}", sessionId);
            return Optional.of(session);
        }
        return Optional.empty();
    }

    /**
     * 更新会话状态
     */
    public void updateSessionStatus(String sessionId, SessionStatus status) {
        getSession(sessionId).ifPresent(session -> {
            session.setStatus(status);
            String key = buildSessionKey(sessionId);
            redisService.set(key, session, SESSION_TTL);

            // 如果会话已完成，移除映射
            if (!isUnfinishedStatus(status) && session.getHealthReportId() != null) {
                removeHealthReportSessionMapping(session.getHealthReportId(), sessionId);
            }

            log.debug("更新会话状态: sessionId={}, status={}", sessionId, status);
        });
    }

    /**
     * 更新当前问题索引
     */
    public void updateCurrentIndex(String sessionId, int currentIndex) {
        getSession(sessionId).ifPresent(session -> {
            session.setCurrentIndex(currentIndex);
            String key = buildSessionKey(sessionId);
            redisService.set(key, session, SESSION_TTL);
            log.debug("更新会话进度: sessionId={}, currentIndex={}", sessionId, currentIndex);
        });
    }

    /**
     * 更新问题列表（用于保存答案）
     */
    public void updateQuestions(String sessionId, List<ConsultationQuestionDTO> questions) {
        getSession(sessionId).ifPresent(session -> {
            try {
                session.setQuestionsJson(objectMapper.writeValueAsString(questions));
                String key = buildSessionKey(sessionId);
                redisService.set(key, session, SESSION_TTL);
                log.debug("更新会话问题: sessionId={}", sessionId);
            } catch (JacksonException e) {
                log.error("序列化问题列表失败", e);
            }
        });
    }

    /**
     * 删除会话缓存
     */
    public void deleteSession(String sessionId) {
        getSession(sessionId).ifPresent(session -> {
            if (session.getHealthReportId() != null) {
                removeHealthReportSessionMapping(session.getHealthReportId(), sessionId);
            }
        });

        String key = buildSessionKey(sessionId);
        redisService.delete(key);
        log.debug("删除会话缓存: sessionId={}", sessionId);
    }

    /**
     * 根据体检报告ID查找未完成的会话ID
     */
    public Optional<String> findUnfinishedSessionId(Long healthReportId) {
        String key = buildHealthReportSessionKey(healthReportId);
        String sessionId = redisService.get(key);
        if (sessionId != null) {
            // 验证会话是否仍然存在且未完成
            Optional<CachedSession> sessionOpt = getSession(sessionId);
            if (sessionOpt.isPresent() && isUnfinishedStatus(sessionOpt.get().getStatus())) {
                return Optional.of(sessionId);
            } else {
                // 会话已不存在或已完成，清理映射
                redisService.delete(key);
            }
        }
        return Optional.empty();
    }

    /**
     * 刷新会话过期时间
     */
    public void refreshSessionTTL(String sessionId) {
        String key = buildSessionKey(sessionId);
        redisService.expire(key, SESSION_TTL);
    }

    /**
     * 检查会话是否在缓存中
     */
    public boolean exists(String sessionId) {
        String key = buildSessionKey(sessionId);
        return redisService.exists(key);
    }

    // ==================== 私有方法 ====================

    private String buildSessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String buildHealthReportSessionKey(Long healthReportId) {
        return HEALTH_REPORT_SESSION_KEY_PREFIX + healthReportId;
    }

    private void saveHealthReportSessionMapping(Long healthReportId, String sessionId) {
        String key = buildHealthReportSessionKey(healthReportId);
        redisService.set(key, sessionId, SESSION_TTL);
    }

    private void removeHealthReportSessionMapping(Long healthReportId, String sessionId) {
        String key = buildHealthReportSessionKey(healthReportId);
        String currentSessionId = redisService.get(key);
        // 只有当前映射的是这个 sessionId 时才删除
        if (sessionId.equals(currentSessionId)) {
            redisService.delete(key);
        }
    }

    private boolean isUnfinishedStatus(SessionStatus status) {
        return status == SessionStatus.CREATED || status == SessionStatus.IN_PROGRESS;
    }
}

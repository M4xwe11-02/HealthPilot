package health.guardian.modules.consultation;

import health.guardian.common.annotation.RateLimit;
import health.guardian.common.result.Result;
import health.guardian.modules.consultation.model.*;
import health.guardian.modules.consultation.service.ConsultationHistoryService;
import health.guardian.modules.consultation.service.ConsultationPersistenceService;
import health.guardian.modules.consultation.service.ConsultationSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 健康问诊控制器
 * 提供 AI 健康问诊相关的API接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ConsultationController {
    
    private final ConsultationSessionService sessionService;
    private final ConsultationHistoryService historyService;
    private final ConsultationPersistenceService persistenceService;
    
    /**
     * 创建问诊会话
     */
    @PostMapping("/api/consultation/sessions")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Result<ConsultationSessionDTO> createSession(@RequestBody CreateConsultationRequest request) {
        log.info("创建问诊会话，题目数量: {}", request.questionCount());
        ConsultationSessionDTO session = sessionService.createSession(request);
        return Result.success(session);
    }
    
    /**
     * 获取会话信息
     */
    @GetMapping("/api/consultation/sessions/{sessionId}")
    public Result<ConsultationSessionDTO> getSession(@PathVariable String sessionId) {
        ConsultationSessionDTO session = sessionService.getSession(sessionId);
        return Result.success(session);
    }
    
    /**
     * 获取当前问题
     */
    @GetMapping("/api/consultation/sessions/{sessionId}/question")
    public Result<Map<String, Object>> getCurrentQuestion(@PathVariable String sessionId) {
        return Result.success(sessionService.getCurrentQuestionResponse(sessionId));
    }
    
    /**
     * 提交答案
     */
    @PostMapping("/api/consultation/sessions/{sessionId}/answers")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL}, count = 10)
    public Result<SubmitAnswerResponse> submitAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        log.info("提交答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer);
        SubmitAnswerResponse response = sessionService.submitAnswer(request);
        return Result.success(response);
    }
    
    /**
     * 生成问诊报告
     */
    @GetMapping("/api/consultation/sessions/{sessionId}/report")
    public Result<ConsultationReportDTO> getReport(@PathVariable String sessionId) {
        log.info("生成问诊报告: {}", sessionId);
        ConsultationReportDTO report = sessionService.generateReport(sessionId);
        return Result.success(report);
    }
    
    /**
     * 查找未完成的问诊会话
     * GET /api/consultation/sessions/unfinished/{healthReportId}
     */
    @GetMapping("/api/consultation/sessions/unfinished/{healthReportId}")
    public Result<ConsultationSessionDTO> findUnfinishedSession(@PathVariable Long healthReportId) {
        return Result.success(sessionService.findUnfinishedSessionOrThrow(healthReportId));
    }
    
    /**
     * 暂存答案（不进入下一题）
     */
    @PutMapping("/api/consultation/sessions/{sessionId}/answers")
    public Result<Void> saveAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        log.info("暂存答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer);
        sessionService.saveAnswer(request);
        return Result.success(null);
    }
    
    /**
     * 提前交卷
     */
    @PostMapping("/api/consultation/sessions/{sessionId}/complete")
    public Result<Void> completeConsultation(@PathVariable String sessionId) {
        log.info("提前交卷: {}", sessionId);
        sessionService.completeConsultation(sessionId);
        return Result.success(null);
    }

    /**
     * 获取问诊会话详情
     * GET /api/consultation/sessions/{sessionId}/details
     */
    @GetMapping("/api/consultation/sessions/{sessionId}/details")
    public Result<ConsultationDetailDTO> getConsultationDetail(@PathVariable String sessionId) {
        ConsultationDetailDTO detail = historyService.getConsultationDetail(sessionId);
        return Result.success(detail);
    }

    /**
     * 导出问诊报告为PDF
     */
    @GetMapping("/api/consultation/sessions/{sessionId}/export")
    public ResponseEntity<byte[]> exportConsultationPdf(@PathVariable String sessionId) {
        try {
            byte[] pdfBytes = historyService.exportConsultationPdf(sessionId);
            String filename = URLEncoder.encode("健康问诊报告_" + sessionId + ".pdf",
                StandardCharsets.UTF_8);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 删除问诊会话
     */
    @DeleteMapping("/api/consultation/sessions/{sessionId}")
    public Result<Void> deleteConsultation(@PathVariable String sessionId) {
        log.info("删除问诊会话: {}", sessionId);
        persistenceService.deleteSessionBySessionId(sessionId);
        return Result.success(null);
    }
}

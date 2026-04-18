package health.guardian.modules.healthreport;

import health.guardian.common.annotation.RateLimit;
import health.guardian.common.result.Result;
import health.guardian.modules.healthreport.model.HealthReportDetailDTO;
import health.guardian.modules.healthreport.model.HealthReportListItemDTO;
import health.guardian.modules.healthreport.service.HealthReportDeleteService;
import health.guardian.modules.healthreport.service.HealthReportHistoryService;
import health.guardian.modules.healthreport.service.HealthReportUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 体检报告控制器
 * Health report controller for upload and analysis
 */
@Slf4j
@RestController
@RequiredArgsConstructor // 构造器注入
public class HealthReportController {

    private final HealthReportUploadService uploadService;
    private final HealthReportDeleteService deleteService;
    private final HealthReportHistoryService historyService;

    /**
     * 上传体检报告并获取健康分析结果
     *
     * @param file 体检报告文件（支持PDF、DOCX、DOC、TXT、MD等）
     * @return 健康分析结果，包含评分和建议
     */
    @PostMapping(value = "/api/healthreports/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Result<Map<String, Object>> uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = uploadService.uploadAndAnalyze(file);
        // 包含三个字段：healthReport, storage, duplicate 表示是否是重复的内容
        boolean isDuplicate = (Boolean) result.get("duplicate");
        if (isDuplicate) {
            return Result.success("检测到相同体检报告，已返回历史分析结果", result);
        }
        return Result.success(result);
    }

    /**
     * 获取所有体检报告列表
     */
    @GetMapping("/api/healthreports")
    public Result<List<HealthReportListItemDTO>> getAllHealthReports() {
        List<HealthReportListItemDTO> healthReports = historyService.getAllHealthReports();
        return Result.success(healthReports);
    }

    /**
     * 获取体检报告详情（包含分析历史）
     */
    @GetMapping("/api/healthreports/{id}/detail")
    public Result<HealthReportDetailDTO> getHealthReportDetail(@PathVariable Long id) {
        HealthReportDetailDTO detail = historyService.getHealthReportDetail(id);
        return Result.success(detail);
    }

    /**
     * 导出健康分析报告为PDF
     */
    @GetMapping("/api/healthreports/{id}/export")
    public ResponseEntity<byte[]> exportAnalysisPdf(@PathVariable Long id) {
        try {
            var result = historyService.exportAnalysisPdf(id);
            String filename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.pdfBytes());
        } catch (Exception e) {
            log.error("导出PDF失败: healthReportId={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除体检报告
     *
     * @param id 体检报告ID
     * @return 删除结果
     */
    @DeleteMapping("/api/healthreports/{id}")
    public Result<Void> deleteHealthReport(@PathVariable Long id) {
        deleteService.deleteHealthReport(id);
        return Result.success(null);
    }

    /**
     * 重新分析体检报告（手动重试）
     * 用于分析失败后的重试
     *
     * @param id 体检报告ID
     * @return 结果
     */
    @PostMapping("/api/healthreports/{id}/reanalyze")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 2)
    public Result<Void> reanalyze(@PathVariable Long id) {
        uploadService.reanalyze(id);
        return Result.success(null);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/api/healthreports/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of(
            "status", "UP",
            "service", "AI Health Guardian - Report Service"
        ));
    }

}

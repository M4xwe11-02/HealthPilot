package health.guardian.modules.consultation.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建问诊会话请求
 */
public record CreateConsultationRequest(
    @NotBlank(message = "体检报告文本不能为空")
    String reportText,      // 体检报告文本内容
    
    @Min(value = 3, message = "题目数量最少3题")
    @Max(value = 20, message = "题目数量最多20题")
    int questionCount,      // 问诊题目数量 (3-20)
    
    @NotNull(message = "体检报告ID不能为空")
    Long healthReportId,          // 体检报告ID（用于持久化关联）
    
    Boolean forceCreate     // 是否强制创建新会话（忽略未完成的会话），默认为 false
) {}

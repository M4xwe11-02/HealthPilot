package health.guardian.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // ========== 通用错误 1xxx ==========
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    // ========== 体检报告模块错误 2xxx ==========
    HEALTH_REPORT_NOT_FOUND(2001, "体检报告不存在"),
    HEALTH_REPORT_PARSE_FAILED(2002, "体检报告解析失败"),
    HEALTH_REPORT_UPLOAD_FAILED(2003, "体检报告上传失败"),
    HEALTH_REPORT_DUPLICATE(2004, "体检报告已存在"),
    HEALTH_REPORT_FILE_EMPTY(2005, "体检报告文件为空"),
    HEALTH_REPORT_FILE_TYPE_NOT_SUPPORTED(2006, "不支持的文件类型"),
    HEALTH_REPORT_ANALYSIS_FAILED(2007, "体检报告分析失败"),
    HEALTH_REPORT_ANALYSIS_NOT_FOUND(2008, "体检报告分析结果不存在"),
    HEALTH_REPORT_NOT_VALID(2009, "上传的文件不是体检报告，请确认后重新上传"),

    // ========== 问诊模块错误 3xxx ==========
    CONSULTATION_SESSION_NOT_FOUND(3001, "问诊会话不存在"),
    CONSULTATION_SESSION_EXPIRED(3002, "问诊会话已过期"),
    CONSULTATION_QUESTION_NOT_FOUND(3003, "问诊问题不存在"),
    CONSULTATION_ALREADY_COMPLETED(3004, "问诊已完成"),
    CONSULTATION_EVALUATION_FAILED(3005, "问诊评估失败"),
    CONSULTATION_QUESTION_GENERATION_FAILED(3006, "问诊问题生成失败"),
    CONSULTATION_NOT_COMPLETED(3007, "问诊尚未完成"),
    
    // ========== 存储模块错误 4xxx ==========
    STORAGE_UPLOAD_FAILED(4001, "文件上传失败"),
    STORAGE_DOWNLOAD_FAILED(4002, "文件下载失败"),
    STORAGE_DELETE_FAILED(4003, "文件删除失败"),
    
    // ========== 导出模块错误 5xxx ==========
    EXPORT_PDF_FAILED(5001, "PDF导出失败"),
    
    // ========== 知识库模块错误 6xxx ==========
    KNOWLEDGE_BASE_NOT_FOUND(6001, "知识库不存在"),
    KNOWLEDGE_BASE_PARSE_FAILED(6002, "知识库文件解析失败"),
    KNOWLEDGE_BASE_UPLOAD_FAILED(6003, "知识库上传失败"),
    KNOWLEDGE_BASE_QUERY_FAILED(6004, "知识库查询失败"),
    KNOWLEDGE_BASE_DELETE_FAILED(6005, "知识库删除失败"),
    KNOWLEDGE_BASE_VECTORIZATION_FAILED(6006, "知识库向量化失败"),
    
    // ========== AI服务错误 7xxx ==========
    AI_SERVICE_UNAVAILABLE(7001, "AI服务暂时不可用，请稍后重试"),
    AI_SERVICE_TIMEOUT(7002, "AI服务响应超时"),
    AI_SERVICE_ERROR(7003, "AI服务调用失败"),
    AI_API_KEY_INVALID(7004, "AI服务密钥无效"),
    AI_RATE_LIMIT_EXCEEDED(7005, "AI服务调用频率超限"),

    // ========== 限流模块错误 8xxx ==========
    RATE_LIMIT_EXCEEDED(8001, "请求过于频繁，请稍后再试"),

    // ========== 公共文档模块错误 9xxx ==========
    PUBLIC_DOC_NOT_FOUND(9001, "公共文档不存在"),
    PUBLIC_DOC_UPLOAD_FAILED(9002, "公共文档上传失败"),
    PUBLIC_DOC_PARSE_FAILED(9003, "公共文档解析失败"),
    PUBLIC_DOC_ANALYSIS_FAILED(9004, "公共文档分析失败"),
    PUBLIC_DOC_DELETE_FAILED(9005, "公共文档下架失败");
    
    private final Integer code;
    private final String message;
}

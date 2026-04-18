package health.guardian.modules.healthreport.service;

import health.guardian.infrastructure.file.ContentTypeDetectionService;
import health.guardian.infrastructure.file.DocumentParseService;
import health.guardian.infrastructure.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 体检报告解析服务
 * 委托给通用的 DocumentParseService 处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthReportParseService {

    private final DocumentParseService documentParseService;
    private final ContentTypeDetectionService contentTypeDetectionService;
    private final FileStorageService storageService;

    /**
     * 解析上传的体检报告文件，提取文本内容
     *
     * @param file 上传的文件（支持PDF、DOCX、DOC、TXT、MD等）
     * @return 提取的文本内容
     */
    public String parseHealthReport(MultipartFile file) {
        log.info("开始解析体检报告文件: {}", file.getOriginalFilename());
        return documentParseService.parseContent(file);
    }

    /**
     * 解析字节数组形式的体检报告文件
     *
     * @param fileBytes 文件字节数组
     * @param fileName  原始文件名（用于日志）
     * @return 提取的文本内容
     */
    public String parseHealthReport(byte[] fileBytes, String fileName) {
        log.info("开始解析体检报告文件（从字节数组）: {}", fileName);
        return documentParseService.parseContent(fileBytes, fileName);
    }

    /**
     * 从存储下载文件并解析内容
     *
     * @param storageKey       存储键
     * @param originalFilename 原始文件名
     * @return 提取的文本内容
     */
    public String downloadAndParseContent(String storageKey, String originalFilename) {
        log.info("从存储下载并解析体检报告文件: {}", originalFilename);
        return documentParseService.downloadAndParseContent(storageService, storageKey, originalFilename);
    }

    /**
     * 检测文件的MIME类型
     */
    public String detectContentType(MultipartFile file) {
        return contentTypeDetectionService.detectContentType(file);
    }
}

package health.guardian.modules.publicdoc.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.infrastructure.file.DocumentParseService;
import health.guardian.infrastructure.file.FileStorageService;
import health.guardian.infrastructure.file.FileValidationService;
import health.guardian.infrastructure.redis.PublicDocCache;
import health.guardian.modules.publicdoc.listener.PublicDocAnalyzeStreamProducer;
import health.guardian.modules.publicdoc.model.PublicDocEntity;
import health.guardian.modules.publicdoc.model.PublicDocListItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDocUploadService {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024L; // 50MB

    private final FileValidationService fileValidationService;
    private final DocumentParseService documentParseService;
    private final FileStorageService fileStorageService;
    private final PublicDocPersistenceService persistenceService;
    private final PublicDocAnalyzeStreamProducer analyzeProducer;
    private final PublicDocCache cache;

    public PublicDocListItemDTO upload(MultipartFile file, String title, String category,
                                      String source, String description) {
        fileValidationService.validateFile(file, MAX_FILE_SIZE, "公共文档");

        String contentType = file.getContentType();
        if (!fileValidationService.isKnowledgeBaseMimeType(contentType)
            && !fileValidationService.isMarkdownExtension(file.getOriginalFilename())) {
            throw new BusinessException(ErrorCode.PUBLIC_DOC_UPLOAD_FAILED, "不支持的文件类型: " + contentType);
        }

        String docText;
        try {
            docText = documentParseService.parseContent(file);
        } catch (Exception e) {
            log.error("公共文档解析失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.PUBLIC_DOC_PARSE_FAILED, "文档解析失败: " + e.getMessage());
        }

        String storageKey;
        try {
            storageKey = fileStorageService.uploadPublicDoc(file);
        } catch (Exception e) {
            log.error("公共文档上传存储失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.PUBLIC_DOC_UPLOAD_FAILED, "文件存储失败: " + e.getMessage());
        }

        PublicDocEntity entity = new PublicDocEntity();
        entity.setTitle(title);
        entity.setCategory(category);
        entity.setSource(source);
        entity.setDescription(description);
        entity.setOriginalFilename(file.getOriginalFilename());
        entity.setFileSize(file.getSize());
        entity.setContentType(contentType != null ? contentType : "application/octet-stream");
        entity.setStorageKey(storageKey);
        entity.setStorageUrl(storageKey);
        entity.setDocText(docText);

        PublicDocEntity saved = persistenceService.saveDoc(entity);

        cache.invalidateList();

        analyzeProducer.sendAnalyzeTask(saved.getId(), docText);

        log.info("公共文档已上传并触发AI分析: id={}, title={}", saved.getId(), title);
        return PublicDocListItemDTO.from(saved);
    }
}

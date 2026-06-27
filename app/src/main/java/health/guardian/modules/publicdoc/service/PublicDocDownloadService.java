package health.guardian.modules.publicdoc.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.infrastructure.file.FileStorageService;
import health.guardian.modules.publicdoc.model.PublicDocEntity;
import health.guardian.modules.publicdoc.repository.PublicDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDocDownloadService {

    private final PublicDocRepository docRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public DownloadResult download(Long id) {
        PublicDocEntity doc = docRepository.findByIdAndIsActiveTrue(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PUBLIC_DOC_NOT_FOUND));

        byte[] data = fileStorageService.downloadFile(doc.getStorageKey());

        doc.incrementDownloadCount();
        docRepository.save(doc);

        log.info("公共文档已下载: id={}, filename={}", id, doc.getOriginalFilename());
        return new DownloadResult(data, doc.getOriginalFilename(), doc.getContentType());
    }

    public record DownloadResult(byte[] data, String filename, String contentType) {}
}

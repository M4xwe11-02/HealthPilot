package health.guardian.modules.publicdoc.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.infrastructure.redis.PublicDocCache;
import health.guardian.modules.publicdoc.repository.PublicDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDocDeleteService {

    private final PublicDocRepository docRepository;
    private final PublicDocPersistenceService persistenceService;
    private final PublicDocCache cache;

    public void delete(Long id) {
        if (!docRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PUBLIC_DOC_NOT_FOUND);
        }
        persistenceService.softDelete(id);
        cache.invalidateDetail(id);
        cache.invalidateList();
        log.info("公共文档已下架并清除缓存: id={}", id);
    }
}

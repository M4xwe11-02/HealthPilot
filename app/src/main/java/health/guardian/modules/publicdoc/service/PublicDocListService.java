package health.guardian.modules.publicdoc.service;

import health.guardian.infrastructure.redis.PublicDocCache;
import health.guardian.modules.publicdoc.model.PublicDocListItemDTO;
import health.guardian.modules.publicdoc.repository.PublicDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDocListService {

    private final PublicDocRepository docRepository;
    private final PublicDocCache cache;

    public List<PublicDocListItemDTO> listDocs() {
        return cache.getList().orElseGet(() -> {
            log.debug("公共文档列表缓存未命中，查询数据库");
            List<PublicDocListItemDTO> list = docRepository.findAllByIsActiveTrueOrderByUploadedAtDesc()
                .stream()
                .map(PublicDocListItemDTO::from)
                .toList();
            cache.saveList(list);
            return list;
        });
    }
}

package health.guardian.infrastructure.redis;

import health.guardian.modules.publicdoc.model.PublicDocDetailDTO;
import health.guardian.modules.publicdoc.model.PublicDocListItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDocCache {

    private final RedisService redisService;

    private static final String LIST_KEY = "public-doc:list";
    private static final String DETAIL_KEY_PREFIX = "public-doc:detail:";
    private static final Duration LIST_TTL = Duration.ofMinutes(5);
    private static final Duration DETAIL_TTL = Duration.ofMinutes(10);

    public void saveList(List<PublicDocListItemDTO> list) {
        redisService.set(LIST_KEY, list, LIST_TTL);
        log.debug("公共文档列表已缓存, size={}", list.size());
    }

    @SuppressWarnings("unchecked")
    public Optional<List<PublicDocListItemDTO>> getList() {
        List<PublicDocListItemDTO> cached = redisService.get(LIST_KEY);
        if (cached != null) {
            log.debug("公共文档列表命中缓存");
            return Optional.of(cached);
        }
        return Optional.empty();
    }

    public void invalidateList() {
        redisService.delete(LIST_KEY);
        log.debug("公共文档列表缓存已清除");
    }

    public void saveDetail(Long id, PublicDocDetailDTO detail) {
        redisService.set(DETAIL_KEY_PREFIX + id, detail, DETAIL_TTL);
        log.debug("公共文档详情已缓存: id={}", id);
    }

    public Optional<PublicDocDetailDTO> getDetail(Long id) {
        PublicDocDetailDTO cached = redisService.get(DETAIL_KEY_PREFIX + id);
        if (cached != null) {
            log.debug("公共文档详情命中缓存: id={}", id);
            return Optional.of(cached);
        }
        return Optional.empty();
    }

    public void invalidateDetail(Long id) {
        redisService.delete(DETAIL_KEY_PREFIX + id);
        log.debug("公共文档详情缓存已清除: id={}", id);
    }
}

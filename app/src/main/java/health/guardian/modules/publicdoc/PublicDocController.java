package health.guardian.modules.publicdoc;

import health.guardian.common.annotation.RateLimit;
import health.guardian.common.result.Result;
import health.guardian.modules.publicdoc.model.PublicDocDetailDTO;
import health.guardian.modules.publicdoc.model.PublicDocListItemDTO;
import health.guardian.modules.publicdoc.service.PublicDocDetailService;
import health.guardian.modules.publicdoc.service.PublicDocDownloadService;
import health.guardian.modules.publicdoc.service.PublicDocListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/public-docs")
@RequiredArgsConstructor
public class PublicDocController {

    private final PublicDocListService listService;
    private final PublicDocDetailService detailService;
    private final PublicDocDownloadService downloadService;

    @GetMapping
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 100)
    public Result<List<PublicDocListItemDTO>> list() {
        return Result.success(listService.listDocs());
    }

    @GetMapping("/{id}/preview")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 50)
    public Result<PublicDocDetailDTO> preview(@PathVariable Long id) {
        return Result.success(detailService.getDetail(id));
    }

    @GetMapping("/{id}/download")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 20)
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        PublicDocDownloadService.DownloadResult result = downloadService.download(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename(result.filename(), StandardCharsets.UTF_8)
            .build());
        headers.setContentType(MediaType.parseMediaType(result.contentType()));
        headers.setContentLength(result.data().length);

        return ResponseEntity.ok().headers(headers).body(result.data());
    }
}

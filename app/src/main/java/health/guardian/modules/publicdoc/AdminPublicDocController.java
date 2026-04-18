package health.guardian.modules.publicdoc;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.common.result.Result;
import health.guardian.modules.auth.CurrentUserContext;
import health.guardian.modules.auth.model.CurrentUserDTO;
import health.guardian.modules.publicdoc.model.PublicDocListItemDTO;
import health.guardian.modules.publicdoc.service.PublicDocDeleteService;
import health.guardian.modules.publicdoc.service.PublicDocUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/admin/public-docs")
@RequiredArgsConstructor
public class AdminPublicDocController {

    private final PublicDocUploadService uploadService;
    private final PublicDocDeleteService deleteService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<PublicDocListItemDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "description", required = false) String description) {
        requireAdmin();
        PublicDocListItemDTO result = uploadService.upload(file, title, category, source, description);
        return Result.success("文档上传成功，AI分析已触发", result);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireAdmin();
        deleteService.delete(id);
        return Result.success(null);
    }

    private void requireAdmin() {
        CurrentUserDTO user = CurrentUserContext.requireCurrentUser();
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无管理员权限");
        }
    }
}

package health.guardian.infrastructure.mapper;

import health.guardian.modules.consultation.model.HealthReportAnalysisResponse;
import health.guardian.modules.healthreport.model.HealthReportAnalysisEntity;
import health.guardian.modules.healthreport.model.HealthReportDetailDTO;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import health.guardian.modules.healthreport.model.HealthReportListItemDTO;
import org.mapstruct.*;

import java.util.List;

/**
 * 体检报告相关的对象映射器
 * 使用MapStruct自动生成转换代码
 * <p>
 * 注意：JSON字段(strengthsJson, suggestionsJson)需要在Service层手动处理
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HealthReportMapper {

    // ========== ScoreDetail 映射 ==========

    /**
     * 将实体基础字段映射到DTO的ScoreDetail
     */
    @Mapping(target = "lifestyleScore", source = "lifestyleScore", qualifiedByName = "nullToZero")
    @Mapping(target = "nutritionScore", source = "nutritionScore", qualifiedByName = "nullToZero")
    @Mapping(target = "physicalFitnessScore", source = "physicalFitnessScore", qualifiedByName = "nullToZero")
    @Mapping(target = "mentalHealthScore", source = "mentalHealthScore", qualifiedByName = "nullToZero")
    @Mapping(target = "preventiveScore", source = "preventiveScore", qualifiedByName = "nullToZero")
    HealthReportAnalysisResponse.ScoreDetail toScoreDetail(HealthReportAnalysisEntity entity);

    // ========== HealthReportListItemDTO 映射 ==========

    /**
     * HealthReportEntity 转换为 HealthReportListItemDTO
     * 需要额外传入 latestScore, lastAnalyzedAt, consultationCount
     */
    default HealthReportListItemDTO toListItemDTO(
        HealthReportEntity healthReport,
        Integer latestScore,
        java.time.LocalDateTime lastAnalyzedAt,
        Integer consultationCount
    ) {
        return new HealthReportListItemDTO(
            healthReport.getId(),
            healthReport.getOriginalFilename(),
            healthReport.getFileSize(),
            healthReport.getUploadedAt(),
            healthReport.getAccessCount(),
            latestScore,
            lastAnalyzedAt,
            consultationCount
        );
    }

    /**
     * 简化版：从 HealthReportEntity 直接映射（其他字段为 null）
     */
    @Mapping(target = "filename", source = "originalFilename")
    @Mapping(target = "latestScore", ignore = true)
    @Mapping(target = "lastAnalyzedAt", ignore = true)
    @Mapping(target = "consultationCount", ignore = true)
    HealthReportListItemDTO toListItemDTOBasic(HealthReportEntity entity);

    // ========== HealthReportDetailDTO 映射 ==========

    /**
     * HealthReportEntity 转换为 HealthReportDetailDTO（不含 analyses 和 consultations）
     */
    @Mapping(target = "filename", source = "originalFilename")
    @Mapping(target = "analyses", ignore = true)
    @Mapping(target = "consultations", ignore = true)
    HealthReportDetailDTO toDetailDTOBasic(HealthReportEntity entity);

    // ========== AnalysisHistoryDTO 映射 ==========

    /**
     * HealthReportAnalysisEntity 转换为 AnalysisHistoryDTO
     * 注意：strengths 和 suggestions 需要在 Service 层从 JSON 解析后传入
     */
    @Mapping(target = "strengths", source = "strengths")
    @Mapping(target = "suggestions", source = "suggestions")
    HealthReportDetailDTO.AnalysisHistoryDTO toAnalysisHistoryDTO(
        HealthReportAnalysisEntity entity,
        List<String> strengths,
        List<Object> suggestions
    );

    /**
     * 批量转换（需要在 Service 层处理 JSON）
     */
    default List<HealthReportDetailDTO.AnalysisHistoryDTO> toAnalysisHistoryDTOList(
        List<HealthReportAnalysisEntity> entities,
        java.util.function.Function<HealthReportAnalysisEntity, List<String>> strengthsExtractor,
        java.util.function.Function<HealthReportAnalysisEntity, List<Object>> suggestionsExtractor
    ) {
        return entities.stream()
            .map(e -> toAnalysisHistoryDTO(e, strengthsExtractor.apply(e), suggestionsExtractor.apply(e)))
            .toList();
    }

    // ========== HealthReportAnalysisEntity 创建映射 ==========

    /**
     * 从 HealthReportAnalysisResponse 创建 HealthReportAnalysisEntity
     * 注意：JSON 字段和 Resume 关联需要在 Service 层设置
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "healthReport", ignore = true)
    @Mapping(target = "strengthsJson", ignore = true)
    @Mapping(target = "suggestionsJson", ignore = true)
    @Mapping(target = "analyzedAt", ignore = true)
    @Mapping(target = "lifestyleScore", source = "scoreDetail.lifestyleScore")
    @Mapping(target = "nutritionScore", source = "scoreDetail.nutritionScore")
    @Mapping(target = "physicalFitnessScore", source = "scoreDetail.physicalFitnessScore")
    @Mapping(target = "mentalHealthScore", source = "scoreDetail.mentalHealthScore")
    @Mapping(target = "preventiveScore", source = "scoreDetail.preventiveScore")
    HealthReportAnalysisEntity toAnalysisEntity(HealthReportAnalysisResponse response);

    /**
     * 更新已有的 HealthReportAnalysisEntity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "healthReport", ignore = true)
    @Mapping(target = "strengthsJson", ignore = true)
    @Mapping(target = "suggestionsJson", ignore = true)
    @Mapping(target = "analyzedAt", ignore = true)
    @Mapping(target = "lifestyleScore", source = "scoreDetail.lifestyleScore")
    @Mapping(target = "nutritionScore", source = "scoreDetail.nutritionScore")
    @Mapping(target = "physicalFitnessScore", source = "scoreDetail.physicalFitnessScore")
    @Mapping(target = "mentalHealthScore", source = "scoreDetail.mentalHealthScore")
    @Mapping(target = "preventiveScore", source = "scoreDetail.preventiveScore")
    void updateAnalysisEntity(HealthReportAnalysisResponse response, @MappingTarget HealthReportAnalysisEntity entity);

    // ========== 工具方法 ==========

    @Named("nullToZero")
    default int nullToZero(Integer value) {
        return value != null ? value : 0;
    }
}

package health.guardian.modules.knowledgebase.model;

import health.guardian.modules.auth.model.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 知识库实体
 */
@Entity
@Table(name = "knowledge_bases", indexes = {
    @Index(name = "idx_kb_owner_hash", columnList = "owner_id,fileHash", unique = true),
    @Index(name = "idx_kb_owner_uploaded", columnList = "owner_id,uploadedAt"),
    @Index(name = "idx_kb_category", columnList = "category")
})
public class KnowledgeBaseEntity {

    @Id // 这个注解声明这个字段是作为这个表的主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 这个枚举就是代表这个是 主键自增的
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;

    // 文件内容的SHA-256哈希值，用于去重
    @Column(nullable = false, length = 64) // 这个表示是一个普通的列，非空，类型是varchar(64)，可变长字符串
    private String fileHash;

    // 知识库名称（用户自定义或从文件名提取）
    @Column(nullable = false)
    private String name;

    // 分类/分组（如"慢病管理"、"药品说明书"、"健康指南"等）
    @Column(length = 100) // varchar(100)
    private String category;

    // 原始文件名
    @Column(nullable = false)
    private String originalFilename;
    
    // 文件大小（字节）
    private Long fileSize;
    
    // 文件类型
    private String contentType;
    
    // RustFS存储的文件Key
    @Column(length = 500)
    private String storageKey;
    
    // RustFS存储的文件URL
    @Column(length = 1000)
    private String storageUrl;
    
    // 上传时间
    @Column(nullable = false)
    private LocalDateTime uploadedAt;
    
    // 最后访问时间
    private LocalDateTime lastAccessedAt;
    
    // 访问次数
    private Integer accessCount = 0;
    
    // 问题数量（用户针对此知识库提问的次数）
    private Integer questionCount = 0;

    // 向量化状态（新上传时为 PENDING，异步处理完成后变为 COMPLETED）
    @Enumerated(EnumType.STRING) // 这个表示限定jpa不要存数字索引，而是存枚举的字符串
    @Column(length = 20)
    private VectorStatus vectorStatus = VectorStatus.PENDING; // 这个表示新建对象的时候，如果不手动设置的话，它默认就是PENDING

    // 向量化错误信息（失败时记录）
    @Column(length = 500)
    private String vectorError;

    @Column(length = 100)
    private String lightRagTrackId;

    @Column(length = 30)
    private String lightRagStatus = "NOT_SUBMITTED";

    @Column(length = 500)
    private String lightRagError;

    // 向量分块数量
    private Integer chunkCount = 0;
    
    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
        accessCount = 1;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }
    
    public String getFileHash() {
        return fileHash;
    }
    
    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getStorageKey() {
        return storageKey;
    }
    
    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }
    
    public String getStorageUrl() {
        return storageUrl;
    }
    
    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public Integer getAccessCount() {
        return accessCount;
    }
    
    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }
    
    public Integer getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
    
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    public void incrementQuestionCount() {
        this.questionCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public VectorStatus getVectorStatus() {
        return vectorStatus;
    }

    public void setVectorStatus(VectorStatus vectorStatus) {
        this.vectorStatus = vectorStatus;
    }

    public String getVectorError() {
        return vectorError;
    }

    public void setVectorError(String vectorError) {
        this.vectorError = vectorError;
    }

    public String getLightRagTrackId() {
        return lightRagTrackId;
    }

    public void setLightRagTrackId(String lightRagTrackId) {
        this.lightRagTrackId = lightRagTrackId;
    }

    public String getLightRagStatus() {
        return lightRagStatus;
    }

    public void setLightRagStatus(String lightRagStatus) {
        this.lightRagStatus = lightRagStatus;
    }

    public String getLightRagError() {
        return lightRagError;
    }

    public void setLightRagError(String lightRagError) {
        this.lightRagError = lightRagError;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }
}


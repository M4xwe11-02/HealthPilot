package health.guardian.common.constant;

/**
 * 异步任务 Redis Stream 通用常量
 * 包含知识库向量化和体检报告分析两个异步任务的配置
 */
public final class AsyncTaskStreamConstants {

    private AsyncTaskStreamConstants() {
        // 私有构造函数，防止实例化
    }

    // ========== 通用消息字段 ==========

    /**
     * 重试次数字段
     */
    public static final String FIELD_RETRY_COUNT = "retryCount";

    /**
     * 文档内容字段
     */
    public static final String FIELD_CONTENT = "content";

    // ========== 通用消费者配置 ==========

    /**
     * 最大重试次数
     */
    public static final int MAX_RETRY_COUNT = 3;

    /**
     * 每次拉取的消息批次大小
     */
    public static final int BATCH_SIZE = 10;

    /**
     * 消费者轮询间隔（毫秒）
     */
    public static final long POLL_INTERVAL_MS = 1000;

    /**
     * Stream 最大长度（自动裁剪旧消息，防止无限增长）
     */
    public static final int STREAM_MAX_LEN = 1000;

    // ========== 知识库向量化 Stream 配置 ==========

    /**
     * 知识库向量化 Stream Key
     */
    public static final String KB_VECTORIZE_STREAM_KEY = "knowledgebase:vectorize:stream";

    /**
     * 知识库向量化 Consumer Group 名称
     */
    public static final String KB_VECTORIZE_GROUP_NAME = "vectorize-group";

    /**
     * 知识库向量化 Consumer 名称前缀
     */
    public static final String KB_VECTORIZE_CONSUMER_PREFIX = "vectorize-consumer-";

    /**
     * 知识库ID字段
     */
    public static final String FIELD_KB_ID = "kbId";

    // ========== 体检报告分析 Stream 配置 ==========

    /**
     * 体检报告分析 Stream Key
     */
    public static final String HEALTH_REPORT_ANALYZE_STREAM_KEY = "healthreport:analyze:stream";

    /**
     * 体检报告分析 Consumer Group 名称
     */
    public static final String HEALTH_REPORT_ANALYZE_GROUP_NAME = "analyze-group";

    /**
     * 体检报告分析 Consumer 名称前缀
     */
    public static final String HEALTH_REPORT_ANALYZE_CONSUMER_PREFIX = "analyze-consumer-";

    /**
     * 体检报告ID字段
     */
    public static final String FIELD_HEALTH_REPORT_ID = "healthReportId";

    // ========== 问诊评估 Stream 配置 ==========

    /**
     * 问诊评估 Stream Key
     */
    public static final String CONSULTATION_EVALUATE_STREAM_KEY = "consultation:evaluate:stream";

    /**
     * 问诊评估 Consumer Group 名称
     */
    public static final String CONSULTATION_EVALUATE_GROUP_NAME = "evaluate-group";

    /**
     * 问诊评估 Consumer 名称前缀
     */
    public static final String CONSULTATION_EVALUATE_CONSUMER_PREFIX = "evaluate-consumer-";

    /**
     * 问诊会话ID字段
     */
    public static final String FIELD_SESSION_ID = "sessionId";

    // ========== 公共文档分析 Stream 配置 ==========

    public static final String PUBLIC_DOC_ANALYZE_STREAM_KEY = "publicdoc:analyze:stream";
    public static final String PUBLIC_DOC_ANALYZE_GROUP_NAME = "publicdoc-analyze-group";
    public static final String PUBLIC_DOC_ANALYZE_CONSUMER_PREFIX = "publicdoc-analyze-consumer-";
    public static final String FIELD_PUBLIC_DOC_ID = "publicDocId";
}

package health.guardian;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring 上下文 smoke test
 *
 * <p>验证应用上下文能否正常加载，覆盖以下场景：
 * <ul>
 *   <li>所有 @Bean 定义能正常实例化</li>
 *   <li>配置属性绑定无误（@ConfigurationProperties）</li>
 *   <li>组件扫描与依赖注入无循环依赖或缺失 bean</li>
 *   <li>编译通过但 Spring wiring 失败的问题（纯单元测试无法发现）</li>
 * </ul>
 *
 * <p>外部服务处理策略：
 * <ul>
 *   <li>PostgreSQL → H2 内存库（application-test.yml）</li>
 *   <li>Redis/Redisson → @MockitoBean，触发 @ConditionalOnMissingBean 跳过真实连接</li>
 *   <li>pgvector VectorStore → @MockitoBean，阻止 pgvector schema 初始化</li>
 *   <li>S3 / Spring AI → 懒加载，填假值不影响上下文启动</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SmokeTest {

    /**
     * 阻止 Redisson 自动配置尝试连接真实 Redis。
     * @MockitoBean 在上下文刷新前注册，使 @ConditionalOnMissingBean 检测到已有 bean 并跳过自动配置。
     */
    @MockitoBean
    RedissonClient redissonClient;

    /**
     * 满足业务代码中对 VectorStore 的 @Autowired 注入。
     * PgVectorStoreAutoConfiguration 已在 application-test.yml 中排除，
     * 避免其 @PostConstruct DDL（CREATE EXTENSION vector）在 H2 上失败。
     */
    @MockitoBean
    VectorStore vectorStore;

    @Test
    void contextLoads() {
        // 能执行到此处即表示 Spring 上下文加载成功。
        // 此测试无需断言——上下文加载失败会直接抛出异常并使测试失败。
    }
}

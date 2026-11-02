package lytheriia.bot.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "discord.token=test-token",
        "discord.guild-id=1",
        "discord.maintainer-id=99",
        "discord.roles.verified=2",
        "discord.roles.booster=3",
        "discord.roles.boosty=11,22,33",
        "boosty.weekly-tokens=0,1,3",
        "api.key=" + IntegrationTest.API_KEY,
        "api.allowed-ips=",
        "ss14.server-ip=127.0.0.1:1",
        "stats.retention-days=90"
})
public abstract class IntegrationTest {

    protected static final String API_KEY = "test-api-key";

    // Started once and shared by all test classes; Testcontainers stops them when the JVM exits.
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected StringRedisTemplate redis;

    @LocalServerPort
    protected int port;

    @BeforeEach
    void cleanStorage() {
        jdbc.execute("TRUNCATE antag_tokens, boosty_subscription, discord_linked_accounts, discord_account_info, "
                + "round_snapshots");
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }
}

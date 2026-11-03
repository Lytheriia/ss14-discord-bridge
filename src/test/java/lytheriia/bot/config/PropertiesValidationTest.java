package lytheriia.bot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PropertiesValidationTest {

    private static final String[] VALID_DISCORD = {
            "discord.token=secret-token",
            "discord.guild-id=1",
            "discord.maintainer-id=2",
            "discord.roles.verified=3",
            "discord.roles.booster=4" };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class)
            .withPropertyValues("api.key=secret-key", "ss14.server-ip=127.0.0.1:1");

    @EnableConfigurationProperties({ DiscordProperties.class, ApiProperties.class, StatsProperties.class,
            BoostyProperties.class })
    static class Config {
    }

    @Test
    void validSettingsStartAndSecretsStayOutOfToString() {
        runner.withPropertyValues(VALID_DISCORD).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(DiscordProperties.class).toString()).doesNotContain("secret-token");
            assertThat(context.getBean(ApiProperties.class).toString()).doesNotContain("secret-key");
            assertThat(context.getBean(StatsProperties.class).retentionDays()).isNull();
        });
    }

    @Test
    void emptyBoostySettingsUseDefaults() {
        runner.withPropertyValues(VALID_DISCORD)
                .withPropertyValues("boosty.weekly-tokens=", "boosty.weekly-token-cap=", "boosty.purchase-ttl-minutes=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    BoostyProperties boosty = context.getBean(BoostyProperties.class);
                    assertThat(boosty.weeklyTokens()).isEmpty();
                    assertThat(boosty.weeklyTokenCap()).isEqualTo(4);
                    assertThat(boosty.purchaseTtlMinutes()).isEqualTo(60);
                });
    }

    @Test
    void emptyBoosterRoleFailsAtStartup() {
        runner.withPropertyValues(VALID_DISCORD).withPropertyValues("discord.roles.booster=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("discord.roles.booster");
        });
    }

    @Test
    void emptyMaintainerFailsAtStartup() {
        runner.withPropertyValues(VALID_DISCORD).withPropertyValues("discord.maintainer-id=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("maintainerId");
        });
    }

    @Test
    void blankApiKeyFailsAtStartup() {
        runner.withPropertyValues(VALID_DISCORD).withPropertyValues("api.key=").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void retentionShorterThanStatsWindowFailsAtStartup() {
        runner.withPropertyValues(VALID_DISCORD).withPropertyValues("stats.retention-days=30")
                .run(context -> assertThat(context).hasFailed());
    }
}

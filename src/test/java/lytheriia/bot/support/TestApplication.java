package lytheriia.bot.support;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import lytheriia.bot.BotApplication;
import lytheriia.bot.config.JdaConfig;
import net.dv8tion.jda.api.JDA;

/**
 * The whole application except the real Discord connection and scheduling: jobs are called directly by the tests.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@AutoConfigurationPackage(basePackages = "lytheriia.bot")
@ConfigurationPropertiesScan("lytheriia.bot")
@ComponentScan(basePackages = "lytheriia.bot", excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = { JdaConfig.class, BotApplication.class }))
public class TestApplication {

    @Bean
    JDA jda() {
        return mock(JDA.class, RETURNS_DEEP_STUBS);
    }
}

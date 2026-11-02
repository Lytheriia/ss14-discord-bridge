package lytheriia.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * @param retentionDays how long round snapshots are kept, null keeps them forever; /stats shows up to 90 days
 */
@Validated
@ConfigurationProperties(prefix = "stats")
public record StatsProperties(@Min(90) Integer retentionDays) {
}

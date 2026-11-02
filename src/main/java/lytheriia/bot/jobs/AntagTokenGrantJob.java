package lytheriia.bot.jobs;

import java.util.concurrent.TimeUnit;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lytheriia.bot.config.BoostyProperties;
import lytheriia.bot.service.AntagTokenService;
import lytheriia.bot.service.BoostySubscriptionService;

@Component
public class AntagTokenGrantJob {
    private static final Logger log = LoggerFactory.getLogger(AntagTokenGrantJob.class);

    private final BoostyProperties properties;
    private final BoostySubscriptionService boostyService;
    private final AntagTokenService tokenService;

    public AntagTokenGrantJob(BoostyProperties properties, BoostySubscriptionService boostyService,
            AntagTokenService tokenService) {
        this.properties = properties;
        this.boostyService = boostyService;
        this.tokenService = tokenService;
    }

    // Starts after the first Boosty reconciliation, so grants use up-to-date subscriptions.
    @Scheduled(initialDelay = 10, fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
    public void grantWeeklyTokens() {
        if (properties.weeklyTokens().isEmpty()) {
            log.warn("Weekly token grant skipped: BOOSTY_WEEKLY_TOKENS is empty");
            return;
        }

        Map<Long, Integer> highestRanks = boostyService.findHighestActiveRanks();
        int granted = 0;

        for (Map.Entry<Long, Integer> subscriber : highestRanks.entrySet()) {
            int amount = properties.tokensForRank(subscriber.getValue());
            if (amount == 0) {
                continue;
            }

            try {
                if (tokenService.grantWeekly(subscriber.getKey(), amount)) {
                    granted++;
                }
            } catch (Exception e) {
                log.error("Failed to grant weekly tokens for discordId={}", subscriber.getKey(), e);
            }
        }

        log.info("Weekly token grant finished: subscribers={}, granted={}", highestRanks.size(), granted);
    }
}

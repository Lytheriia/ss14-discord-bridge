package lytheriia.bot.discord.listeners;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lytheriia.bot.db.service.AntagTokenService;
import lytheriia.bot.db.service.BoostySubscriberService;
import lytheriia.bot.properties.SponsorshipProperties;

@Component
public class AntagTokenGrantJob {
    private static final Logger log = LoggerFactory.getLogger(AntagTokenGrantJob.class);

    private final SponsorshipProperties properties;
    private final BoostySubscriberService boostyService;
    private final AntagTokenService tokenService;

    public AntagTokenGrantJob(SponsorshipProperties properties, BoostySubscriberService boostyService,
            AntagTokenService tokenService) {
        this.properties = properties;
        this.boostyService = boostyService;
        this.tokenService = tokenService;
    }

    // Starts after the first Boosty reconciliation, so grants use up-to-date subscriptions.
    @Scheduled(initialDelay = 10, fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
    public void grantWeeklyTokens() {
        if (properties.weeklyTokens().isEmpty()) {
            log.warn("Weekly token grant skipped: SPONSOR_WEEKLY_TOKENS is empty");
            return;
        }

        Map<Long, Integer> highestRanks = boostyService.findHighestActiveRanks();
        int granted = 0;

        for (Map.Entry<Long, Integer> sponsor : highestRanks.entrySet()) {
            int amount = properties.tokensForTier(sponsor.getValue());
            if (amount == 0) {
                continue;
            }

            try {
                if (tokenService.grantWeekly(sponsor.getKey(), amount)) {
                    granted++;
                }
            } catch (Exception e) {
                log.error("Failed to grant weekly tokens for discordId={}", sponsor.getKey(), e);
            }
        }

        log.info("Weekly token grant finished: sponsors={}, granted={}", highestRanks.size(), granted);
    }
}

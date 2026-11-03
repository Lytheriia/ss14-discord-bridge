package lytheriia.bot.jobs;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lytheriia.bot.config.BoostyProperties;
import lytheriia.bot.service.AntagTokenService;

@Component
public class AntagTokenPurchaseExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(AntagTokenPurchaseExpiryJob.class);

    private final BoostyProperties properties;
    private final AntagTokenService tokenService;

    public AntagTokenPurchaseExpiryJob(BoostyProperties properties, AntagTokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    @Scheduled(initialDelay = 60, fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
    public void refundExpiredPurchases() {
        int refunded = tokenService.refundExpired(Duration.ofMinutes(properties.purchaseTtlMinutes()));

        if (refunded > 0) {
            log.warn("Refunded {} antag token purchases that were never finished", refunded);
        }
    }
}

package lytheriia.bot.jobs;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lytheriia.bot.config.StatsProperties;
import lytheriia.bot.service.RoundSnapshotService;

@Component
public class RoundSnapshotCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(RoundSnapshotCleanupJob.class);

    private final StatsProperties properties;
    private final RoundSnapshotService snapshotService;

    public RoundSnapshotCleanupJob(StatsProperties properties, RoundSnapshotService snapshotService) {
        this.properties = properties;
        this.snapshotService = snapshotService;
    }

    @Scheduled(initialDelay = 1, fixedDelay = 24, timeUnit = TimeUnit.HOURS)
    public void deleteOldSnapshots() {
        if (properties.retentionDays() == null) {
            return;
        }

        int deleted = snapshotService.deleteOlderThan(properties.retentionDays());
        log.info("Round snapshot cleanup finished: deleted={}", deleted);
    }
}

package lytheriia.bot.db.repository;

import java.time.Instant;

public interface RoundSnapshotBucket {
    Instant getBucket();

    double getPlayers();
}

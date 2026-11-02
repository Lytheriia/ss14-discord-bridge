package lytheriia.bot.db.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lytheriia.bot.db.entity.RoundSnapshotEntity;

public interface RoundSnapshotRepository extends JpaRepository<RoundSnapshotEntity, Long> {

    @Query(value = """
            SELECT date_bin(make_interval(secs => CAST(:bucketSeconds AS double precision)),
                            time_stamp, TIMESTAMPTZ 'epoch') AS bucket,
                   CAST(avg(players) AS double precision) AS players
            FROM round_snapshots
            WHERE time_stamp > :from
            GROUP BY bucket
            ORDER BY bucket
            """, nativeQuery = true)
    List<RoundSnapshotBucket> findAverages(@Param("from") Instant from, @Param("bucketSeconds") long bucketSeconds);

    @Modifying
    @Query(value = "DELETE FROM round_snapshots WHERE time_stamp < :before", nativeQuery = true)
    int deleteOlderThan(@Param("before") Instant before);

    default RoundSnapshotEntity saveSnapshot(int roundId, int players) {
        RoundSnapshotEntity entity = new RoundSnapshotEntity();
        entity.setRoundId(roundId);
        entity.setPlayers(players);
        return save(entity);
    }
}

package lytheriia.bot.db.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import lytheriia.bot.db.entity.RoundSnapshotEntity;

public interface RoundSnapshotRepository extends JpaRepository<RoundSnapshotEntity, Long> {

    List<RoundSnapshotEntity> findByTimeStampAfterOrderByTimeStampAsc(LocalDateTime from);

    List<RoundSnapshotEntity> findByTimeStampBetweenOrderByTimeStampAsc(LocalDateTime from, LocalDateTime to);

    default List<RoundSnapshotEntity> findLastDays(int days) {
        return findByTimeStampAfterOrderByTimeStampAsc(LocalDateTime.now().minusDays(days));
    }

    default RoundSnapshotEntity saveSnapshot(int roundId, int players) {
        RoundSnapshotEntity entity = new RoundSnapshotEntity();
        entity.setRoundId(roundId);
        entity.setPlayers(players);
        return save(entity);
    }
}
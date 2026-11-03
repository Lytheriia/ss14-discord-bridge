package lytheriia.bot.db.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lytheriia.bot.db.entity.AntagTokenPurchaseEntity;

public interface AntagTokenPurchaseRepository extends JpaRepository<AntagTokenPurchaseEntity, UUID> {

    @Query(value = "SELECT * FROM antag_token_purchase WHERE discord_id = :discordId AND status = 'PENDING'",
            nativeQuery = true)
    Optional<AntagTokenPurchaseEntity> findPendingByDiscordId(@Param("discordId") long discordId);

    @Query(value = "SELECT id FROM antag_token_purchase WHERE server_id = :serverId AND status = 'PENDING'",
            nativeQuery = true)
    List<UUID> findPendingIds(@Param("serverId") String serverId);

    @Query(value = "SELECT id FROM antag_token_purchase WHERE status = 'PENDING' AND created_at <= :before",
            nativeQuery = true)
    List<UUID> findPendingIdsCreatedBefore(@Param("before") Instant before);

    /**
     * @return 1 if this call finished the purchase, 0 if it was not waiting anymore
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE antag_token_purchase
            SET status = :status, finished_at = now()
            WHERE id = :id AND status = 'PENDING'
            """, nativeQuery = true)
    int finishIfPending(@Param("id") UUID id, @Param("status") String status);
}

package lytheriia.bot.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lytheriia.bot.db.entity.AntagTokensEntity;

public interface AntagTokensRepository extends JpaRepository<AntagTokensEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO antag_tokens (discord_id, balance, last_weekly_grant_at)
            VALUES (:discordId, :amount, now())
            ON CONFLICT (discord_id) DO UPDATE
            SET balance = antag_tokens.balance + EXCLUDED.balance,
                last_weekly_grant_at = now()
            WHERE antag_tokens.last_weekly_grant_at IS NULL
               OR antag_tokens.last_weekly_grant_at <= now() - INTERVAL '7 days'
            """, nativeQuery = true)
    int addWeekly(@Param("discordId") long discordId, @Param("amount") int amount);

    @Modifying
    @Query(value = """
            INSERT INTO antag_tokens (discord_id, balance)
            VALUES (:discordId, :amount)
            ON CONFLICT (discord_id) DO UPDATE
            SET balance = antag_tokens.balance + EXCLUDED.balance
            """, nativeQuery = true)
    int addManual(@Param("discordId") long discordId, @Param("amount") int amount);
}

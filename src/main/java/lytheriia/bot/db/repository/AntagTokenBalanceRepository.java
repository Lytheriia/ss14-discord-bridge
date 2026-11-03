package lytheriia.bot.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lytheriia.bot.db.entity.AntagTokenBalanceEntity;

public interface AntagTokenBalanceRepository extends JpaRepository<AntagTokenBalanceEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO antag_tokens (discord_id, balance, last_weekly_grant_at)
            VALUES (:discordId, LEAST(:amount, :cap), now())
            ON CONFLICT (discord_id) DO UPDATE
            SET balance = LEAST(antag_tokens.balance + :amount, :cap),
                last_weekly_grant_at = now()
            WHERE antag_tokens.balance < :cap
              AND (antag_tokens.last_weekly_grant_at IS NULL
                   OR antag_tokens.last_weekly_grant_at <= now() - make_interval(days => :intervalDays))
            """, nativeQuery = true)
    int addWeekly(@Param("discordId") long discordId, @Param("amount") int amount, @Param("cap") int cap,
            @Param("intervalDays") int intervalDays);

    @Modifying
    @Query(value = """
            INSERT INTO antag_tokens (discord_id, balance)
            VALUES (:discordId, :amount)
            ON CONFLICT (discord_id) DO UPDATE
            SET balance = antag_tokens.balance + EXCLUDED.balance
            """, nativeQuery = true)
    int addManual(@Param("discordId") long discordId, @Param("amount") int amount);

    /**
     * @return 1 if a token was taken, 0 if the balance is empty
     */
    @Modifying
    @Query(value = "UPDATE antag_tokens SET balance = balance - 1 WHERE discord_id = :discordId AND balance >= 1",
            nativeQuery = true)
    int spendOne(@Param("discordId") long discordId);
}

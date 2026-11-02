package lytheriia.bot.db.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "antag_tokens")
public class AntagTokenBalanceEntity {

    @Id
    private long discordId;

    @Column(nullable = false)
    private int balance;

    @Column
    private Instant lastWeeklyGrantAt;

    protected AntagTokenBalanceEntity() {
    }

    public long getDiscordId() {
        return discordId;
    }

    public int getBalance() {
        return balance;
    }

    public Instant getLastWeeklyGrantAt() {
        return lastWeeklyGrantAt;
    }
}

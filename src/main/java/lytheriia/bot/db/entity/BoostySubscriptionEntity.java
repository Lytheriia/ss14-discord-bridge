package lytheriia.bot.db.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "boosty_subscription")
public class BoostySubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long discordId;

    @Column(nullable = false)
    private long promotedById;

    @Column(nullable = false)
    private int rank;

    @CreationTimestamp
    private Instant startedAt;

    @Column
    private Instant endedAt;

    protected BoostySubscriptionEntity() {
    }

    public BoostySubscriptionEntity(long discordId, long promotedById, int rank) {
        this.discordId = discordId;
        this.promotedById = promotedById;
        this.rank = rank;
    }

    public void end() {
        this.endedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public long getDiscordId() {
        return discordId;
    }

    public long getPromotedById() {
        return promotedById;
    }

    public int getRank() {
        return rank;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }
}

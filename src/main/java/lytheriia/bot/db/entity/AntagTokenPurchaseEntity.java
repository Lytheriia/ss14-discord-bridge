package lytheriia.bot.db.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "antag_token_purchase")
public class AntagTokenPurchaseEntity implements Persistable<UUID> {

    public enum Status {
        PENDING, GRANTED, REFUNDED
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private long discordId;

    @Column(nullable = false, length = 64)
    private String listingId;

    @Column(nullable = false, length = 64)
    private String serverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @CreationTimestamp
    private Instant createdAt;

    @Column
    private Instant finishedAt;

    // The id comes from the game server, so a non-null id does not mean the row exists.
    @Transient
    private boolean persisted;

    protected AntagTokenPurchaseEntity() {
    }

    public AntagTokenPurchaseEntity(UUID id, long discordId, String listingId, String serverId) {
        this.id = id;
        this.discordId = discordId;
        this.listingId = listingId;
        this.serverId = serverId;
        this.status = Status.PENDING;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        persisted = true;
    }

    @Override
    public boolean isNew() {
        return !persisted;
    }

    public boolean isSameWaitingPurchase(long discordId, String listingId, String serverId) {
        return status == Status.PENDING
                && this.discordId == discordId
                && this.listingId.equals(listingId)
                && this.serverId.equals(serverId);
    }

    @Override
    public UUID getId() {
        return id;
    }

    public long getDiscordId() {
        return discordId;
    }

    public String getListingId() {
        return listingId;
    }

    public String getServerId() {
        return serverId;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}

package lytheriia.bot.db.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "discord_linked_accounts")
public class DiscordLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discord_id", nullable = false, unique = true)
    private long discordId;

    @Column(name = "ss14_user_id", nullable = false, unique = true)
    private UUID ss14UserId;

    @CreationTimestamp
    private Instant linkedAt;

    protected DiscordLinkEntity() {
    }

    public DiscordLinkEntity(long discordId, UUID ss14UserId) {
        this.discordId = discordId;
        this.ss14UserId = ss14UserId;
    }

    public UUID getSs14UserId() {
        return ss14UserId;
    }

    public void setSs14UserId(UUID ss14UserId) {
        this.ss14UserId = ss14UserId;
    }

    public long getDiscordId() {
        return discordId;
    }

    public void setDiscordId(long discordId) {
        this.discordId = discordId;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }
}

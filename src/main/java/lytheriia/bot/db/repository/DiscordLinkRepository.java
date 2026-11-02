package lytheriia.bot.db.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import lytheriia.bot.db.entity.DiscordLinkEntity;

public interface DiscordLinkRepository extends JpaRepository<DiscordLinkEntity, Long> {

    boolean existsByDiscordId(long discordId);

    boolean existsBySs14UserId(UUID ss14UserId);

    Optional<DiscordLinkEntity> findByDiscordId(long discordId);

    Optional<DiscordLinkEntity> findBySs14UserId(UUID ss14UserId);

    long deleteByDiscordId(long discordId);
}
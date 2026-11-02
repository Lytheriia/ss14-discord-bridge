package lytheriia.bot.db.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lytheriia.bot.db.entity.BoostySubscriptionEntity;

public interface BoostySubscriptionRepository extends JpaRepository<BoostySubscriptionEntity, Long> {

    Optional<BoostySubscriptionEntity> findByDiscordIdAndRankAndEndedAtIsNull(long discordId, int rank);

    Optional<BoostySubscriptionEntity> findTopByDiscordIdAndEndedAtIsNullOrderByRankDesc(long discordId);

    List<BoostySubscriptionEntity> findByDiscordIdAndEndedAtIsNull(long discordId);

    List<BoostySubscriptionEntity> findByEndedAtIsNull();
}

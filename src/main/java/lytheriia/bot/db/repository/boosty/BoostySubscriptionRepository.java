package lytheriia.bot.db.repository.boosty;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lytheriia.bot.db.entity.boosty.BoostySubscriptionEntity;

public interface BoostySubscriptionRepository extends JpaRepository<BoostySubscriptionEntity, Long> {

    Optional<BoostySubscriptionEntity> findByDiscordIdAndRankAndEndedAtIsNull(long discordId, int rank);

    Optional<BoostySubscriptionEntity> findTopByDiscordIdAndEndedAtIsNullOrderByRankDesc(long discordId);
}

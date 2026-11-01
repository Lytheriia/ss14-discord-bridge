package lytheriia.bot.db.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lytheriia.bot.db.entity.boosty.BoostySubscriptionEntity;
import lytheriia.bot.db.repository.DiscordLinkRepository;
import lytheriia.bot.db.repository.boosty.BoostySubscriptionRepository;

@Service
public class BoostySubscriberService {

    private final BoostySubscriptionRepository subscriptionRepository;
    private final DiscordLinkRepository discordLinkRepository;

    public BoostySubscriberService(BoostySubscriptionRepository subscriptionRepository,
            DiscordLinkRepository discordLinkRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.discordLinkRepository = discordLinkRepository;
    }

    @Transactional(readOnly = true)
    public Optional<BoostySubscriptionEntity> findActiveByUser(UUID ss14UserId) {
        return discordLinkRepository.findBySs14UserId(ss14UserId)
                .flatMap(link -> subscriptionRepository
                        .findTopByDiscordIdAndEndedAtIsNullOrderByRankDesc(link.getDiscordId()));
    }

    @Transactional
    public void subscribe(long discordId, long promotedBy, int rank) {
        subscriptionRepository.findByDiscordIdAndRankAndEndedAtIsNull(discordId, rank)
                .ifPresent(old -> {
                    archive(old);
                    subscriptionRepository.flush();
                });

        subscriptionRepository.save(new BoostySubscriptionEntity(discordId, promotedBy, rank));
    }

    @Transactional
    public void unsubscribe(long discordId, int rank) {
        subscriptionRepository.findByDiscordIdAndRankAndEndedAtIsNull(discordId, rank)
                .ifPresent(this::archive);
    }

    @Transactional
    public void unsubscribeAll(long discordId) {
        List<BoostySubscriptionEntity> subscriptions = subscriptionRepository
                .findByDiscordIdAndEndedAtIsNull(discordId);

        for (BoostySubscriptionEntity subscriptionEntity : subscriptions) {
            subscriptionEntity.end();
        }
    }

    @Transactional
    public void ensureUserSubscription(long discordId, List<Integer> currentRanks, long promotedBy) {

        List<BoostySubscriptionEntity> currentSubscriptions = subscriptionRepository
                .findByDiscordIdAndEndedAtIsNull(discordId);

        Set<Integer> activeRanks = currentSubscriptions.stream()
                .map(BoostySubscriptionEntity::getRank)
                .collect(Collectors.toSet());

        for (Integer rank : currentRanks) {
            if (!activeRanks.contains(rank)) {
                subscriptionRepository.save(new BoostySubscriptionEntity(discordId, promotedBy, rank));
            }
        }

        for (BoostySubscriptionEntity subscription : currentSubscriptions) {
            if (!currentRanks.contains(subscription.getRank())) {
                subscription.end();
            }
        }
    }

    @Transactional(readOnly = true)
    public Set<Long> findActiveDiscordIds() {
        return subscriptionRepository.findByEndedAtIsNull().stream()
                .map(BoostySubscriptionEntity::getDiscordId)
                .collect(Collectors.toSet());
    }

    private void archive(BoostySubscriptionEntity active) {
        active.end();
    }
}

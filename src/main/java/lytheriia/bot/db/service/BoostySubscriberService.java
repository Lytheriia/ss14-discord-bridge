package lytheriia.bot.db.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private void archive(BoostySubscriptionEntity active) {
        active.end();
    }
}

package lytheriia.bot.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lytheriia.bot.db.entity.DiscordAccountInfoEntity;
import lytheriia.bot.db.repository.DiscordAccountInfoRepository;

@Service
public class DiscordAccountInfoService {

    public record AccountStatus(boolean verified, boolean booster) {
    }

    private final DiscordAccountInfoRepository infoRepository;

    public DiscordAccountInfoService(DiscordAccountInfoRepository infoRepository) {
        this.infoRepository = infoRepository;
    }

    @Transactional
    public void createIfMissing(long discordId) {
        getOrCreate(discordId);
    }

    @Transactional
    public void setVerified(long discordId, boolean verified) {
        getOrCreate(discordId).setVerified(verified);
    }

    @Transactional
    public void setBooster(long discordId, boolean booster) {
        getOrCreate(discordId).setBooster(booster);
    }

    @Transactional
    public void clearVerified(long discordId) {
        infoRepository.findByDiscordId(discordId).ifPresent(info -> info.setVerified(false));
    }

    @Transactional
    public void clearBooster(long discordId) {
        infoRepository.findByDiscordId(discordId).ifPresent(info -> info.setBooster(false));
    }

    private DiscordAccountInfoEntity getOrCreate(long discordId) {
        return infoRepository.findByDiscordId(discordId).orElseGet(() -> {
            return infoRepository.save(new DiscordAccountInfoEntity(discordId));
        });
    }

    @Transactional(readOnly = true)
    public Map<Long, AccountStatus> getAllStatuses() {
        return infoRepository.findAll().stream()
                .collect(Collectors.toMap(
                        DiscordAccountInfoEntity::getDiscordId,
                        i -> new AccountStatus(i.isVerified(), i.isBooster())));
    }

    @Transactional
    public void setStatus(long discordId, boolean verified, boolean booster) {
        DiscordAccountInfoEntity info = getOrCreate(discordId);
        info.setVerified(verified);
        info.setBooster(booster);
    }
}
package lytheriia.bot.jobs;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lytheriia.bot.config.DiscordProperties;
import lytheriia.bot.service.BoostySubscriptionService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.JDA;

@Component
public class BoostyReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(BoostyReconciliationJob.class);

    private final Map<Integer, Long> boostyRoleIds;
    private final BoostySubscriptionService boostyService;
    private final JDA jda;
    private final long guildId;
    private final long botId;

    public BoostyReconciliationJob(DiscordProperties props, BoostySubscriptionService boostyService, JDA jda) {
        this.boostyRoleIds = props.getRoles().boostyRolesByRank();
        this.guildId = props.getGuildId();
        this.boostyService = boostyService;
        this.jda = jda;
        this.botId = jda.getSelfUser().getIdLong();
    }

    @Scheduled(initialDelay = 5, fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
    public void reconcileSubscriptions() {
        if (boostyRoleIds.isEmpty()) {
            log.warn("Boosty reconciliation skipped: no Boosty roles configured");
            return;
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null || !guild.isLoaded()) {
            log.warn("Boosty reconciliation skipped: guild members are not loaded");
            return;
        }

        Map<Long, List<Integer>> ranksByMember = new HashMap<>();

        for (Member member : guild.getMembers()) {
            List<Integer> ranks = findRanks(member);

            if (!ranks.isEmpty()) {
                ranksByMember.put(member.getIdLong(), ranks);
            }
        }

        Set<Long> idsToCheck = new HashSet<>(boostyService.findActiveDiscordIds());
        idsToCheck.addAll(ranksByMember.keySet());

        for (Long id : idsToCheck) {
            try {
                boostyService.ensureUserSubscription(id, ranksByMember.getOrDefault(id, List.of()), botId);
            } catch (Exception e) {
                log.error("Failed to reconcile Boosty subscriptions for discordId={}", id, e);
            }
        }

        log.info("Boosty reconciliation finished: checked={}", idsToCheck.size());
    }

    private List<Integer> findRanks(Member member) {
        Set<Long> memberRoleIds = member.getRoles().stream()
                .map(Role::getIdLong)
                .collect(Collectors.toSet());

        List<Integer> ranks = new ArrayList<>();

        for (Map.Entry<Integer, Long> entry : boostyRoleIds.entrySet()) {
            if (memberRoleIds.contains(entry.getValue())) {
                ranks.add(entry.getKey());
            }
        }

        return ranks;
    }
}

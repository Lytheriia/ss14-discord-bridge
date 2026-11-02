package lytheriia.bot.discord.listener;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lytheriia.bot.config.DiscordProperties;
import lytheriia.bot.service.BoostySubscriptionService;
import lytheriia.bot.service.DiscordAccountInfoService;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.pagination.AuditLogPaginationAction;

@Component
public class BoostyRoleListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(BoostyRoleListener.class);

    private static final long AUDIT_DELAY_MS = 1500;
    private static final long AUDIT_MAX_AGE_SEC = 15;

    private final Map<Integer, Long> boostyRoleIds;
    private final DiscordAccountInfoService accountService;
    private final BoostySubscriptionService boostyService;

    public BoostyRoleListener(DiscordAccountInfoService accountService,
            BoostySubscriptionService boostyService,
            DiscordProperties props) {
        this.accountService = accountService;
        this.boostyService = boostyService;
        this.boostyRoleIds = props.getRoles().boostyRolesByRank();

        if (boostyRoleIds.isEmpty()) {
            log.warn("DISCORD_BOOSTY_ROLES is empty: Boosty roles will not be tracked");
        }
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        List<Integer> ranks = findBoostyRanks(event.getRoles());
        if (ranks.isEmpty()) {
            return;
        }

        long discordId = event.getUser().getIdLong();
        Guild guild = event.getGuild();

        resolveResponsibleId(
                guild.retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE),
                discordId,
                promotedBy -> ranks.forEach(rank -> {
                    Member member = guild.getMemberById(discordId);

                    if (member == null)
                        return;

                    if (!hasRole(member, boostyRoleIds.get(rank)))
                        return;

                    try {
                        boostyService.subscribe(discordId, promotedBy, rank);
                    } catch (Exception e) {
                        log.error("Failed to subscribe discordId={} rank={}", discordId, rank, e);
                    }
                }));
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        long discordId = event.getUser().getIdLong();

        try {
            boostyService.unsubscribeAll(discordId);
        } catch (Exception e) {
            log.error("Failed to unsubscribe discordId={} on leave", discordId, e);
        }
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        List<Integer> ranks = findBoostyRanks(event.getRoles());
        if (ranks.isEmpty()) {
            return;
        }

        long discordId = event.getUser().getIdLong();

        boolean hasBoostyRole = event.getMember().getRoles().stream()
                .anyMatch(role -> boostyRoleIds.containsValue(role.getIdLong()));
        if (!hasBoostyRole) {
            accountService.setBooster(discordId, false);
        }

        ranks.forEach(rank -> {
            try {
                boostyService.unsubscribe(discordId, rank);
            } catch (Exception e) {
                log.error("Failed to unsubscribe discordId={} rank={}", discordId, rank, e);
            }
        });
    }

    private List<Integer> findBoostyRanks(List<Role> roles) {
        Set<Long> roleIds = roles.stream().map(Role::getIdLong).collect(Collectors.toSet());
        return boostyRoleIds.entrySet().stream()
                .filter(e -> roleIds.contains(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private void resolveResponsibleId(AuditLogPaginationAction action, long targetDiscordId,
            LongConsumer callback) {
        long fallbackId = action.getGuild().getJDA().getSelfUser().getIdLong();

        action.limit(25).queueAfter(AUDIT_DELAY_MS, TimeUnit.MILLISECONDS, entries -> {
            OffsetDateTime threshold = OffsetDateTime.now().minusSeconds(AUDIT_MAX_AGE_SEC);

            long responsibleId = entries.stream()
                    .filter(e -> e.getTargetIdLong() == targetDiscordId)
                    .filter(e -> e.getTimeCreated().isAfter(threshold))
                    .map(AuditLogEntry::getUserIdLong)
                    .findFirst()
                    .orElse(fallbackId);

            callback.accept(responsibleId);
        }, error -> {
            log.warn("Could not read audit log (missing VIEW_AUDIT_LOG?): {}", error.getMessage());
            callback.accept(fallbackId);
        });
    }

    private static boolean hasRole(Member member, long roleId) {
        return member.getRoles().stream().anyMatch(r -> r.getIdLong() == roleId);
    }
}

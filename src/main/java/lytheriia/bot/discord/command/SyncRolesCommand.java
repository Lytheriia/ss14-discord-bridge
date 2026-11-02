package lytheriia.bot.discord.command;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import lytheriia.bot.config.DiscordProperties;
import lytheriia.bot.service.DiscordAccountInfoService;
import lytheriia.bot.service.DiscordAccountInfoService.AccountStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
public class SyncRolesCommand extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SyncRolesCommand.class);

    public static final String COMMAND = "syncdbroles";

    private final long maintainerId;
    private final long verifiedRoleId;
    private final long boosterRoleId;
    private final DiscordAccountInfoService accountService;
    private final TaskExecutor executor;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public SyncRolesCommand(
            DiscordProperties props,
            DiscordAccountInfoService accountService,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor) {

        DiscordProperties.Roles roles = props.getRoles();

        this.maintainerId = props.getMaintainerId();
        this.verifiedRoleId = roles.getVerified();
        this.boosterRoleId = roles.getBooster();
        this.accountService = accountService;
        this.executor = executor;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals(COMMAND))
            return;

        event.deferReply(true).queue();

        if (event.getUser().getIdLong() != maintainerId) {
            event.getHook().sendMessage("Команда доступна только главному разработчику.").queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            event.getHook().sendMessage("Команда работает только на сервере.").queue();
            return;
        }

        if (!running.compareAndSet(false, true)) {
            event.getHook().sendMessage("Синхронизация уже выполняется.").queue();
            return;
        }

        executor.execute(() -> {
            try {
                sync(event, guild);
            } finally {
                running.set(false);
            }
        });
    }

    private void sync(SlashCommandInteractionEvent event, Guild guild) {
        try {
            List<Member> members = guild.loadMembers().get();
            Map<Long, AccountStatus> db = accountService.getAllStatuses();

            Set<Long> memberIds = new HashSet<>();
            int added = 0;
            int updated = 0;
            int left = 0;

            for (Member member : members) {
                if (member.getUser().isBot())
                    continue;

                long id = member.getIdLong();
                memberIds.add(id);

                boolean verified = hasRole(member, verifiedRoleId);
                boolean booster = hasRole(member, boosterRoleId);
                AccountStatus row = db.get(id);

                if (row == null) {
                    if (!verified && !booster)
                        continue;

                    accountService.setStatus(id, verified, booster);
                    added++;
                } else if (row.verified() != verified || row.booster() != booster) {
                    accountService.setStatus(id, verified, booster);
                    updated++;
                }
            }

            for (Map.Entry<Long, AccountStatus> entry : db.entrySet()) {
                AccountStatus row = entry.getValue();
                if (!memberIds.contains(entry.getKey()) && (row.verified() || row.booster())) {
                    accountService.setStatus(entry.getKey(), false, false);
                    left++;
                }
            }

            event.getHook()
                    .editOriginal("Готово. Добавлено: " + added + ", обновлено: " + updated
                            + ", сброшено у вышедших: " + left + ".")
                    .queue();

            log.info("syncdbroles: members={}, added={}, updated={}, left={}",
                    memberIds.size(), added, updated, left);

        } catch (Exception e) {
            log.error("syncdbroles failed", e);
            event.getHook().editOriginal("Ошибка: " + e.getClass().getSimpleName()).queue();
        }
    }

    private static boolean hasRole(Member member, long roleId) {
        return member.getRoles().stream().anyMatch(r -> r.getIdLong() == roleId);
    }
}
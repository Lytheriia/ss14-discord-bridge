package lytheriia.bot.discord.command;

import org.springframework.stereotype.Component;

import lytheriia.bot.service.CommandPermissionService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Component
public class CommandGuard {

    private final CommandPermissionService permissions;

    public CommandGuard(
            CommandPermissionService permissions) {
        this.permissions = permissions;
    }

    public boolean isAllow(SlashCommandInteractionEvent event) {
        if (permissions.hasAccess(event.getMember(), event.getName())) {
            return true;
        }

        event.reply("У вас нет доступа к этой команде.")
                .setEphemeral(true)
                .queue();

        return false;
    }
}
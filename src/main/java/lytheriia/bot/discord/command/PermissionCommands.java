package lytheriia.bot.discord.command;

import org.springframework.stereotype.Component;

import lytheriia.bot.service.CommandPermissionService;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.Permission;

@Component
public class PermissionCommands extends ListenerAdapter {

    private final CommandPermissionService permissions;

    public PermissionCommands(CommandPermissionService permissions) {
        this.permissions = permissions;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName();
        if (!name.equals("addpermission") && !name.equals("removepermission") && !name.equals("listpermissions"))
            return;

        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("Данная команда доступна лищб кураторскому составу.").setEphemeral(true).queue();
            return;
        }

        String command = event.getOption("command", OptionMapping::getAsString);

        switch (name) {
            case "addpermission" -> {
                Role role = event.getOption("role", OptionMapping::getAsRole);
                boolean added = permissions.add(command, role.getIdLong());
                event.reply(added
                        ? "Роль " + role.getAsMention() + " теперь может использовать `/" + command + "`."
                        : "У роли " + role.getAsMention() + " уже есть доступ к `/" + command + "`.")
                        .setEphemeral(true).queue();
            }

            case "removepermission" -> {
                Role role = event.getOption("role", OptionMapping::getAsRole);
                boolean removed = permissions.remove(command, role.getIdLong());
                event.reply(removed
                        ? "Доступ роли " + role.getAsMention() + " к `/" + command + "` убран."
                        : "У роли " + role.getAsMention() + " не было доступа к `/" + command + "`.")
                        .setEphemeral(true).queue();
            }

            case "listpermissions" -> {
                var roles = permissions.list(command).stream()
                        .map(p -> "<@&" + p.getRoleId() + ">").toList();
                event.reply(roles.isEmpty()
                        ? "Для `/" + command + "` роли не назначены (только администраторы)."
                        : "Доступ к `/" + command + "`: " + String.join(", ", roles))
                        .setEphemeral(true).queue();
            }
        }
    }
}
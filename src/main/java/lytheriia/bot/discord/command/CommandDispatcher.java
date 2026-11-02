package lytheriia.bot.discord.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

@Component
public class CommandDispatcher extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

    private final Map<String, SlashCommand> byName;
    private final List<SlashCommandData> data;
    private final CommandGuard guard;

    public CommandDispatcher(List<SlashCommand> commands, CommandCatalog catalog,
            CommandGuard guard) {
        this.guard = guard;

        Map<String, SlashCommand> map = new LinkedHashMap<>();
        List<SlashCommandData> list = new ArrayList<>();
        for (SlashCommand command : commands) {
            SlashCommandData d = command.data();
            if (map.putIfAbsent(d.getName(), command) != null) {
                throw new IllegalStateException("Команда /" + d.getName() + " объявлена дважды");
            }
            list.add(d);
        }
        this.byName = Map.copyOf(map);
        this.data = List.copyOf(list);

        catalog.load(data);
    }

    public void register(Guild guild) {
        guild.updateCommands()
                .addCommands(data)
                .queue(
                        ok -> log.info("Registered {} guild commands", ok.size()),
                        err -> log.error("Failed to register commands", err));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        SlashCommand command = byName.get(event.getName());
        if (command == null) {
            return;
        }
        try {
            if (command.guarded() && !guard.isAllow(event)) {
                return;
            }
            command.execute(event);
        } catch (Exception ex) {
            log.error("Command {} executed with error ", event.getFullCommandName(), ex);
            if (!event.isAcknowledged()) {
                event.reply("❌ Internal error").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        SlashCommand command = byName.get(event.getName());
        if (command == null) {
            return;
        }
        try {
            command.autocomplete(event);
        } catch (Exception ex) {
            log.warn("Автодополнение {} не удалось", event.getFullCommandName(), ex);
            event.replyChoiceStrings(List.of()).queue();
        }
    }
}
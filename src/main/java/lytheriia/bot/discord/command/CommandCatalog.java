package lytheriia.bot.discord.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

@Component
public class CommandCatalog {

    private final ObjectProvider<? extends SlashCommand> commands;
    private volatile List<String> names;

    public CommandCatalog(ObjectProvider<? extends SlashCommand> commands) {
        this.commands = commands;
    }

    public boolean contains(String name) {
        return names().contains(name);
    }

    public List<String> search(String typed) {
        String q = typed.toLowerCase();
        return names().stream()
                .filter(n -> n.toLowerCase().contains(q))
                .limit(25)
                .toList();
    }

    public void load(Collection<? extends SlashCommandData> commands) {
        List<String> result = new ArrayList<>();
        for (SlashCommandData c : commands) {
            if (c.getSubcommands().isEmpty() && c.getSubcommandGroups().isEmpty()) {
                result.add(c.getName());
                continue;
            }
            for (SubcommandData s : c.getSubcommands()) {
                result.add(c.getName() + " " + s.getName());
            }
            for (SubcommandGroupData g : c.getSubcommandGroups()) {
                for (SubcommandData s : g.getSubcommands()) {
                    result.add(c.getName() + " " + g.getName() + " " + s.getName());
                }
            }
        }
        names = result.stream().sorted().toList();
    }

    private List<String> names() {
        List<String> local = names;
        if (local == null) {
            synchronized (this) {
                local = names;
                if (local == null) {
                    local = build(commands.stream().map(SlashCommand::data).toList());
                    names = local;
                }
            }
        }
        return local;
    }

    private static List<String> build(Collection<? extends SlashCommandData> data) {
        List<String> result = new ArrayList<>();
        for (SlashCommandData c : data) {
            if (c.getSubcommands().isEmpty() && c.getSubcommandGroups().isEmpty()) {
                result.add(c.getName());
                continue;
            }
            for (SubcommandData s : c.getSubcommands()) {
                result.add(c.getName() + " " + s.getName());
            }
            for (SubcommandGroupData g : c.getSubcommandGroups()) {
                for (SubcommandData s : g.getSubcommands()) {
                    result.add(c.getName() + " " + g.getName() + " " + s.getName());
                }
            }
        }
        return result.stream().sorted().toList();
    }
}
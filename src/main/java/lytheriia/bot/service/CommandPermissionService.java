package lytheriia.bot.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lytheriia.bot.db.entity.CommandPermissionEntity;
import lytheriia.bot.db.repository.CommandPermissionRepository;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.Permission;

@Service
public class CommandPermissionService {

    public static final List<String> MANAGED = List.of(
            "stats",
            "tokens"
        );

    private final CommandPermissionRepository repo;

    public CommandPermissionService(CommandPermissionRepository repo) {
        this.repo = repo;
    }

    public boolean hasAccess(Member member, String command) {
        if (member == null)
            return false;

        if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR))
            return true;

        List<Long> roleIds = member.getRoles().stream().map(Role::getIdLong).toList();
        return !roleIds.isEmpty() && repo.existsByCommandNameAndRoleIdIn(command, roleIds);
    }

    @Transactional
    public boolean add(String command, long roleId) {
        if (repo.existsByCommandNameAndRoleId(command, roleId))
            return false;

        repo.save(new CommandPermissionEntity(command, roleId));
        return true;
    }

    @Transactional
    public boolean remove(String command, long roleId) {
        return repo.deleteByCommandNameAndRoleId(command, roleId) > 0;
    }

    @Transactional(readOnly = true)
    public List<CommandPermissionEntity> list(String command) {
        return repo.findByCommandName(command);
    }
}
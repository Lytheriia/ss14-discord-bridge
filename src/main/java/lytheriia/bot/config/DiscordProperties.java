package lytheriia.bot.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "discord")
public class DiscordProperties {
    private String token;
    private Long guildId;
    private Roles roles = new Roles();
    private Long maintainerId;

    public Roles getRoles() {
        return roles;
    }

    public void setRoles(Roles roles) {
        this.roles = roles;
    }

    public Long getGuildId() {
        return guildId;
    }

    public void setGuildId(Long guildId) {
        this.guildId = guildId;
    }

    public Long getMaintainerId() {
        return maintainerId;
    }

    public void setMaintainerId(Long maintainerId) {
        this.maintainerId = maintainerId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public static class Roles {
        private Long verified;
        private Long booster;
        private List<Long> boosty = new ArrayList<>();

        public Long getVerified() {
            return verified;
        }

        public void setVerified(Long verified) {
            this.verified = verified;
        }

        public Long getBooster() {
            return booster;
        }

        public void setBooster(Long booster) {
            this.booster = booster;
        }

        public List<Long> getBoosty() {
            return boosty;
        }

        public void setBoosty(List<Long> boosty) {
            this.boosty = boosty;
        }

        public Map<Integer, Long> boostyRolesByRank() {
            Map<Integer, Long> rolesByRank = new HashMap<>();

            for (int i = 0; i < boosty.size(); i++) {
                rolesByRank.put(i + 1, boosty.get(i));
            }

            return Map.copyOf(rolesByRank);
        }

    }
}

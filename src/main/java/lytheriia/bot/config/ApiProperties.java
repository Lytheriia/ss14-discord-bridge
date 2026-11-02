package lytheriia.bot.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "api")
public record ApiProperties(@NotBlank String key, List<String> allowedIps) {

    public ApiProperties {
        allowedIps = allowedIps == null ? List.of() : List.copyOf(allowedIps);
    }

    @Override
    public String toString() {
        return "ApiProperties[allowedIps=" + allowedIps + "]";
    }
}

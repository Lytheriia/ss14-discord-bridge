package lytheriia.bot.discord.presence;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class ServerStatusClient {

    private static final Logger log = LoggerFactory.getLogger(ServerStatusClient.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper;
    private final String ip;

    public ServerStatusClient(ObjectMapper mapper, @Value("${ss14.server-ip}") String ip) {
        this.mapper = mapper;
        this.ip = ip;
    }

    public Optional<ServerStatus> fetch() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + ip + "/status"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("HTTP error", response.statusCode());
                return Optional.empty();
            }
            return Optional.of(mapper.readValue(response.body(), ServerStatus.class));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (HttpTimeoutException e) {
            log.error("Timeout while getting server status");
        } catch (IOException e) {
            log.error("Server status unhandled error: {}", e.getMessage());
        }
        
        return Optional.empty();
    }
}
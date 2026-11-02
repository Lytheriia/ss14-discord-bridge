package lytheriia.bot.api.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lytheriia.bot.api.dto.LinkStatus;
import lytheriia.bot.api.dto.RegisterCodeRequest;
import lytheriia.bot.service.DiscordLinkingService;

@RestController
@RequestMapping("/api/link")
public class DiscordLinkingController {

    private final DiscordLinkingService linkingService;

    public DiscordLinkingController(DiscordLinkingService linkingService) {
        this.linkingService = linkingService;
    }

    @PostMapping("/codes")
    public ResponseEntity<?> registerCode(@Valid @RequestBody RegisterCodeRequest req) {
        if (linkingService.isLinked(req.userId())) {
            return error(HttpStatus.CONFLICT, "ALREADY_LINKED");
        }
        try {
            linkingService.register(req.code(), req.userId());
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, "CODE_COLLISION");
        } catch (DataAccessException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE");
        }
    }

    @GetMapping("/{userId}")
    public LinkStatus status(@PathVariable UUID userId) {
        return linkingService.findLink(userId)
                .map(l -> new LinkStatus(true, String.valueOf(l.getDiscordId())))
                .orElse(new LinkStatus(false, null));
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(Map.of("error", code));
    }
}
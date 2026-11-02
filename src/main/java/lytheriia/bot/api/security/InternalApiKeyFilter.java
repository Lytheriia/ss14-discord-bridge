package lytheriia.bot.api.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lytheriia.bot.config.ApiProperties;
import jakarta.servlet.ServletException;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Internal-Api-Key";

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness");

    private final byte[] expectedKey;
    private final Set<String> allowedIps;

    public InternalApiKeyFilter(ApiProperties properties) {
        this.expectedKey = properties.key().getBytes(StandardCharsets.UTF_8);
        this.allowedIps = Set.copyOf(
                properties.allowedIps().stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXCLUDED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!allowedIps.isEmpty() && !allowedIps.contains(request.getRemoteAddr())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String provided = request.getHeader(HEADER_NAME);

        if (provided == null
                || !MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Invalid internal API key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
package br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.rest;

import br.com.github.gtvnv.omnishift.infrastructure.security.ApiKeyValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Exige o header X-Api-Key em toda rota, exceto /actuator/health (senão o HEALTHCHECK do
 * Dockerfile e sondas de liveness/readiness de orquestrador quebram). Roda antes do
 * DispatcherServlet, então o GlobalExceptionHandler não pega erro daqui — o corpo de erro
 * 401 é escrito diretamente, no mesmo formato.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String HEALTH_PATH = "/actuator/health";

    private final ApiKeyValidator apiKeyValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKeyAuthFilter(ApiKeyValidator apiKeyValidator) {
        this.apiKeyValidator = apiKeyValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (HEALTH_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> clientName = apiKeyValidator.resolveClientName(request.getHeader(API_KEY_HEADER));
        if (clientName.isEmpty()) {
            log.warn("Requisicao rejeitada: X-Api-Key ausente ou invalida ({})", request.getRequestURI());
            writeUnauthorized(response);
            return;
        }

        log.info("Requisicao autenticada: cliente={}", clientName.get());
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Não Autorizado");
        body.put("message", "Header X-Api-Key ausente ou inválido.");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

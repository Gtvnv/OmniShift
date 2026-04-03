package br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.rest;

import br.com.github.gtvnv.omnishift.application.dto.ShiftRequest;
import br.com.github.gtvnv.omnishift.application.usecase.ShiftDataUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shift")
public class ShiftController {

    private static final Logger log = LoggerFactory.getLogger(ShiftController.class);
    private final ShiftDataUseCase shiftDataUseCase;

    public ShiftController(ShiftDataUseCase shiftDataUseCase) {
        this.shiftDataUseCase = shiftDataUseCase;
    }

    @PostMapping
    public ResponseEntity<String> shift(
            @RequestHeader("X-Source-Format") String sourceFormat,
            @RequestHeader("X-Target-Format") String targetFormat,
            @RequestBody String rawPayload) {

        // 1. Sanitização para evitar Log Forging (remove quebras de linha e limita tamanho)
        String safeSource = sanitizeHeaderInput(sourceFormat);
        String safeTarget = sanitizeHeaderInput(targetFormat);

        log.info("Recebendo requisicao de conversao: {} -> {}", safeSource, safeTarget);

        ShiftRequest request = new ShiftRequest(rawPayload, safeSource, safeTarget);
        String convertedData = shiftDataUseCase.execute(request);

        log.info("Conversao concluida com sucesso para o formato: {}", safeTarget);

        // 2. Hardening dos Headers HTTP contra XSS e Sniffing
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, resolveContentType(safeTarget))
                .header("X-Content-Type-Options", "nosniff") // Bloqueia a execução de scripts indesejados
                .header("X-XSS-Protection", "1; mode=block") // Camada extra de defesa em navegadores legados
                .body(convertedData);
    }

    // Método utilitário de segurança
    private String sanitizeHeaderInput(String input) {
        if (input == null || input.isBlank()) {
            return "UNKNOWN";
        }
        // Remove CRLF (\r, \n) e caracteres de controle
        String sanitized = input.replaceAll("[\r\n\t]", "").replaceAll("\\p{C}", "").trim();

        // Evita estouro de buffer no log caso mandem um header de 5MB
        return sanitized.length() > 20 ? sanitized.substring(0, 20) : sanitized;
    }

    // Resolve dinamicamente o tipo HTTP de retorno
    private String resolveContentType(String format) {
        return switch (format.toUpperCase()) {
            case "JSON" -> "application/json";
            case "XML" -> "application/xml";
            case "YAML", "YML" -> "application/x-yaml";
            default -> "text/plain";
        };
    }
}
package br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.rest;

import br.com.github.gtvnv.omnishift.application.usecase.ShiftDataUseCase;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import br.com.github.gtvnv.omnishift.domain.ports.MetricsRecorder;
import br.com.github.gtvnv.omnishift.infrastructure.security.PayloadValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shift")
public class ShiftController {

    private static final Logger log = LoggerFactory.getLogger(ShiftController.class);
    private final ShiftDataUseCase shiftDataUseCase;
    private final PayloadValidator payloadValidator;
    private final MetricsRecorder metricsRecorder;

    public ShiftController(ShiftDataUseCase shiftDataUseCase, PayloadValidator payloadValidator,
                            MetricsRecorder metricsRecorder) {
        this.shiftDataUseCase = shiftDataUseCase;
        this.payloadValidator = payloadValidator;
        this.metricsRecorder = metricsRecorder;
    }

    @PostMapping
    public ResponseEntity<StreamingResponseBody> shift(
            @RequestHeader("X-Source-Format") String sourceFormat,
            @RequestHeader("X-Target-Format") String targetFormat,
            @RequestHeader(value = "X-Mapping-Profile", required = false) String mappingProfile,
            HttpServletRequest request) throws IOException {

        if (request.getContentLengthLong() == 0) {
            throw new IllegalArgumentException("O payload não pode ser nulo ou vazio.");
        }

        // 1. Sanitização para evitar Log Forging (remove quebras de linha e limita tamanho)
        String safeSource = sanitizeHeaderInput(sourceFormat, 20);
        String safeTarget = sanitizeHeaderInput(targetFormat, 20);
        boolean hasMappingProfile = mappingProfile != null && !mappingProfile.isBlank();
        String safeProfile = hasMappingProfile ? sanitizeHeaderInput(mappingProfile, 100) : null;

        log.info("Recebendo requisicao de conversao (streaming): {} -> {} (perfil: {})", safeSource, safeTarget,
                hasMappingProfile ? safeProfile : "nenhum");

        // 0. Sanitização de conteúdo: o limite de tamanho é aplicado durante a leitura do
        // stream (não upfront), então funciona mesmo sem Content-Length confiável.
        InputStream limitedPayload = payloadValidator.limit(request.getInputStream());

        long start = System.nanoTime();
        OmniNode canonicalData;
        DataSerializer serializer;
        try {
            canonicalData = hasMappingProfile
                    ? shiftDataUseCase.parseAndTransformWithProfile(limitedPayload, safeSource, safeProfile)
                    : shiftDataUseCase.parseAndTransform(limitedPayload, safeSource, List.of());

            // Resolve o serializer (e falha cedo se o formato de destino não existir) antes
            // de começar a escrever a resposta — depois disso não dá mais para trocar o status.
            serializer = shiftDataUseCase.resolveSerializer(safeTarget);

            // Métrica cobre parse+transform+resolução do serializer, não a escrita final do
            // stream de saída abaixo (que não pode falhar por causa de input do usuário).
            metricsRecorder.recordConversion(safeSource, safeTarget, System.nanoTime() - start, true);
        } catch (RuntimeException e) {
            metricsRecorder.recordConversion(safeSource, safeTarget, System.nanoTime() - start, false);
            throw e;
        }

        log.info("Conversao concluida com sucesso para o formato: {}", safeTarget);

        StreamingResponseBody body = outputStream -> serializer.serialize(canonicalData, outputStream);

        // 2. Hardening dos Headers HTTP contra XSS e Sniffing
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, resolveContentType(safeTarget))
                .header("X-Content-Type-Options", "nosniff") // Bloqueia a execução de scripts indesejados
                .header("X-XSS-Protection", "1; mode=block") // Camada extra de defesa em navegadores legados
                .body(body);
    }

    // Método utilitário de segurança
    private String sanitizeHeaderInput(String input, int maxLength) {
        if (input == null || input.isBlank()) {
            return "UNKNOWN";
        }
        // Remove CRLF (\r, \n) e caracteres de controle
        String sanitized = input.replaceAll("[\r\n\t]", "").replaceAll("\\p{C}", "").trim();

        // Evita estouro de buffer no log caso mandem um header de 5MB
        return sanitized.length() > maxLength ? sanitized.substring(0, maxLength) : sanitized;
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
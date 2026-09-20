package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.json;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.jackson.JacksonOmniNodeConverter;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class JacksonJsonParser implements DataParser {

    private final ObjectMapper objectMapper;

    // Construtor sem argumentos exigido pelo java.util.ServiceLoader (descoberta via SPI)
    public JacksonJsonParser() {
        this.objectMapper = new ObjectMapper();

        // Limite explícito de profundidade de aninhamento, como defesa em profundidade
        // contra payloads profundamente aninhados (evita depender do default implícito).
        this.objectMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder().maxNestingDepth(500).build());
    }

    @Override
    public OmniNode parse(String payload) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            return JacksonOmniNodeConverter.toOmniNode(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o payload JSON bruto", e);
        }
    }

    @Override
    public OmniNode parse(InputStream inputStream) {
        try {
            JsonNode rootNode = objectMapper.readTree(inputStream);
            return JacksonOmniNodeConverter.toOmniNode(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o InputStream JSON", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "JSON"; // Este é o gatilho que a ParserFactory vai ler!
    }
}
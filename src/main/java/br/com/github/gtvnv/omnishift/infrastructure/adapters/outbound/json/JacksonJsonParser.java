package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.json;

import br.com.github.gtvnv.omnishift.domain.model.*;
import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

@Component
public class JacksonJsonParser implements DataParser {

    private final ObjectMapper objectMapper;

    // O Spring Boot fornece o ObjectMapper configurado automaticamente
    public JacksonJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public OmniNode parse(String payload) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            return mapToOmniNode(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o payload JSON bruto", e);
        }
    }

    @Override
    public OmniNode parse(InputStream inputStream) {
        try {
            JsonNode rootNode = objectMapper.readTree(inputStream);
            return mapToOmniNode(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o InputStream JSON", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "JSON"; // Este é o gatilho que a ParserFactory vai ler!
    }

    /**
     * Motor de conversão recursiva.
     * Isola o nosso domínio (OmniNode) da biblioteca externa (Jackson).
     */
    private OmniNode mapToOmniNode(JsonNode jsonNode) {
        if (jsonNode.isNull()) {
            return OmniNull.getInstance();

        } else if (jsonNode.isObject()) {
            OmniObject omniObject = new OmniObject();
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                omniObject.put(field.getKey(), mapToOmniNode(field.getValue())); // Recursão
            }
            return omniObject;

        } else if (jsonNode.isArray()) {
            OmniArray omniArray = new OmniArray();
            for (JsonNode element : jsonNode) {
                omniArray.add(mapToOmniNode(element)); // Recursão
            }
            return omniArray;

        } else {
            // Valores primitivos finais
            if (jsonNode.isNumber()) {
                return new OmniValue(jsonNode.numberValue());
            } else if (jsonNode.isBoolean()) {
                return new OmniValue(jsonNode.booleanValue());
            } else {
                return new OmniValue(jsonNode.asText());
            }
        }
    }
}
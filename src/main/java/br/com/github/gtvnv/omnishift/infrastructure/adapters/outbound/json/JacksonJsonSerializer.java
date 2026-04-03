package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.json;

import br.com.github.gtvnv.omnishift.domain.model.*;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JacksonJsonSerializer implements DataSerializer {

    private final ObjectMapper objectMapper;

    // Injeção do ObjectMapper configurado pelo Spring
    public JacksonJsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(OmniNode node) {
        try {
            // Converte nosso OmniNode para estruturas padrão do Java (Map, List, Primitivos)
            Object javaObject = mapToJavaObject(node);
            // O Jackson pega as estruturas padrão e cospe a String JSON
            return objectMapper.writeValueAsString(javaObject);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar OmniNode para JSON String", e);
        }
    }

    @Override
    public void serialize(OmniNode node, OutputStream outputStream) {
        try {
            Object javaObject = mapToJavaObject(node);
            // Grava direto no fluxo de saída (excelente para streaming/arquivos grandes)
            objectMapper.writeValue(outputStream, javaObject);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar OmniNode para JSON OutputStream", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "JSON"; // Este é o gatilho que a SerializerFactory vai ler!
    }

    /**
     * Motor de conversão recursiva utilizando Pattern Matching do Java 21!
     * Mapeia os nossos objetos de domínio para estruturas que o Jackson entende nativamente.
     */
    private Object mapToJavaObject(OmniNode node) {
        return switch (node) {
            case OmniObject obj -> {
                // Mantém a ordem original das chaves
                Map<String, Object> map = new LinkedHashMap<>();
                obj.getProperties().forEach((key, value) -> map.put(key, mapToJavaObject(value)));
                yield map;
            }
            case OmniArray arr ->
                    arr.getElements().stream().map(this::mapToJavaObject).toList();

            case OmniValue val ->
                    val.getValue(); // Retorna a String, Number ou Boolean original

            case OmniNull n ->
                    null;

            default ->
                    throw new IllegalStateException("Tipo de nó não suportado pelo motor: " + node.getClass());
        };
    }
}
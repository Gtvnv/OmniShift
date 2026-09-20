package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.yaml;

import br.com.github.gtvnv.omnishift.domain.model.*;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class JacksonYamlSerializer implements DataSerializer {

    private final YAMLMapper yamlMapper;

    public JacksonYamlSerializer() {
        this.yamlMapper = new YAMLMapper();
    }

    @Override
    public String serialize(OmniNode node) {
        try {
            Object javaObject = mapToJavaObject(node);
            return yamlMapper.writeValueAsString(javaObject);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar OmniNode para YAML String", e);
        }
    }

    @Override
    public void serialize(OmniNode node, OutputStream outputStream) {
        try {
            Object javaObject = mapToJavaObject(node);
            yamlMapper.writeValue(outputStream, javaObject);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar OmniNode para YAML OutputStream", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "YAML"; // Gatilho lido pela SerializerFactory
    }

    /**
     * Motor de conversão usando o Pattern Matching do Java 21.
     * Diferente do XML, YAML representa 'null' nativamente, então OmniNull
     * vira null Java de verdade (mesma regra do serializer JSON).
     */
    private Object mapToJavaObject(OmniNode node) {
        return switch (node) {
            case OmniObject obj -> {
                Map<String, Object> map = new LinkedHashMap<>();
                obj.getProperties().forEach((key, value) -> map.put(key, mapToJavaObject(value)));
                yield map;
            }
            case OmniArray arr ->
                    arr.getElements().stream().map(this::mapToJavaObject).toList();
            case OmniValue val ->
                    val.getValue();
            case OmniNull n ->
                    null;
            default ->
                    throw new IllegalStateException("Tipo de nó não suportado: " + node.getClass());
        };
    }
}

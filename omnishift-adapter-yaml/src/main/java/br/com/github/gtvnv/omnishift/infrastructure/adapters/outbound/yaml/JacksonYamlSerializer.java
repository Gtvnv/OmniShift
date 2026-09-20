package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.yaml;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.jackson.JacksonOmniNodeConverter;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.OutputStream;

public class JacksonYamlSerializer implements DataSerializer {

    private final YAMLMapper yamlMapper;

    public JacksonYamlSerializer() {
        this.yamlMapper = new YAMLMapper();
    }

    @Override
    public String serialize(OmniNode node) {
        try {
            // Diferente do XML, YAML representa 'null' nativamente, então OmniNull
            // vira null Java de verdade (mesma regra do serializer JSON).
            Object javaObject = JacksonOmniNodeConverter.toJavaObject(node, null);
            return yamlMapper.writeValueAsString(javaObject);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar OmniNode para YAML String", e);
        }
    }

    @Override
    public void serialize(OmniNode node, OutputStream outputStream) {
        try {
            Object javaObject = JacksonOmniNodeConverter.toJavaObject(node, null);
            yamlMapper.writeValue(outputStream, javaObject);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar OmniNode para YAML OutputStream", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "YAML"; // Gatilho lido pela SerializerFactory
    }
}

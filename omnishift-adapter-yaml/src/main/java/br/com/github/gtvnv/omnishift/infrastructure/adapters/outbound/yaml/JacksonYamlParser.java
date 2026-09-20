package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.yaml;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.jackson.JacksonOmniNodeConverter;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.InputStream;

public class JacksonYamlParser implements DataParser {

    private final YAMLMapper yamlMapper;

    public JacksonYamlParser() {
        this.yamlMapper = new YAMLMapper();

        // Limite explícito de profundidade de aninhamento, como defesa em profundidade
        // contra payloads profundamente aninhados (evita depender do default implícito).
        this.yamlMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder().maxNestingDepth(500).build());
    }

    @Override
    public OmniNode parse(String payload) {
        try {
            JsonNode rootNode = yamlMapper.readTree(payload);
            return JacksonOmniNodeConverter.toOmniNode(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o payload YAML bruto", e);
        }
    }

    @Override
    public OmniNode parse(InputStream inputStream) {
        try {
            JsonNode rootNode = yamlMapper.readTree(inputStream);
            return JacksonOmniNodeConverter.toOmniNode(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o InputStream YAML", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "YAML"; // O gatilho que a ParserFactory vai ler!
    }
}

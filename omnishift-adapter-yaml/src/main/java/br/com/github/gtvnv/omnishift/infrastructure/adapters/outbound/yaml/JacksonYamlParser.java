package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.yaml;

import br.com.github.gtvnv.omnishift.domain.model.*;
import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public class JacksonYamlParser implements DataParser {

    private final YAMLMapper yamlMapper;

    public JacksonYamlParser() {
        this.yamlMapper = new YAMLMapper();
    }

    @Override
    public OmniNode parse(String payload) {
        try {
            JsonNode rootNode = yamlMapper.readTree(payload);
            return mapToOmniNode(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o payload YAML bruto", e);
        }
    }

    @Override
    public OmniNode parse(InputStream inputStream) {
        try {
            JsonNode rootNode = yamlMapper.readTree(inputStream);
            return mapToOmniNode(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o InputStream YAML", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "YAML"; // O gatilho que a ParserFactory vai ler!
    }

    /**
     * Motor de conversão recursiva.
     * Como o Jackson abstrai o YAML num JsonNode, a lógica é idêntica à do JSON/XML!
     */
    private OmniNode mapToOmniNode(JsonNode node) {
        if (node.isNull()) {
            return OmniNull.getInstance();

        } else if (node.isObject()) {
            OmniObject omniObject = new OmniObject();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                omniObject.put(field.getKey(), mapToOmniNode(field.getValue()));
            }
            return omniObject;

        } else if (node.isArray()) {
            OmniArray omniArray = new OmniArray();
            for (JsonNode element : node) {
                omniArray.add(mapToOmniNode(element));
            }
            return omniArray;

        } else {
            if (node.isNumber()) {
                return new OmniValue(node.numberValue());
            } else if (node.isBoolean()) {
                return new OmniValue(node.booleanValue());
            } else {
                return new OmniValue(node.asText());
            }
        }
    }
}

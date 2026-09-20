package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.jackson;

import br.com.github.gtvnv.omnishift.domain.model.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conversão recursiva entre {@link OmniNode} e os tipos que o Jackson entende
 * nativamente ({@link JsonNode} na leitura, {@code Map}/{@code List}/primitivos na escrita).
 * Compartilhada pelos adapters JSON, XML e YAML, que usam Jackson por baixo e por isso
 * tinham essa mesma lógica de conversão triplicada.
 */
public final class JacksonOmniNodeConverter {

    private JacksonOmniNodeConverter() {
    }

    public static OmniNode toOmniNode(JsonNode jsonNode) {
        if (jsonNode.isNull()) {
            return OmniNull.getInstance();

        } else if (jsonNode.isObject()) {
            OmniObject omniObject = new OmniObject();
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                omniObject.put(field.getKey(), toOmniNode(field.getValue()));
            }
            return omniObject;

        } else if (jsonNode.isArray()) {
            OmniArray omniArray = new OmniArray();
            for (JsonNode element : jsonNode) {
                omniArray.add(toOmniNode(element));
            }
            return omniArray;

        } else if (jsonNode.isNumber()) {
            return new OmniValue(jsonNode.numberValue());
        } else if (jsonNode.isBoolean()) {
            return new OmniValue(jsonNode.booleanValue());
        } else {
            return new OmniValue(jsonNode.asText());
        }
    }

    /**
     * @param nullRepresentation valor usado para {@link OmniNull} — cada formato decide o que
     *                           faz mais sentido (ex: {@code null} para JSON/YAML, {@code ""} para XML).
     */
    public static Object toJavaObject(OmniNode node, Object nullRepresentation) {
        return switch (node) {
            case OmniObject obj -> {
                Map<String, Object> map = new LinkedHashMap<>();
                obj.getProperties().forEach((key, value) -> map.put(key, toJavaObject(value, nullRepresentation)));
                yield map;
            }
            case OmniArray arr ->
                    arr.getElements().stream().map(element -> toJavaObject(element, nullRepresentation)).toList();

            case OmniValue val -> val.getValue();

            case OmniNull n -> nullRepresentation;

            default -> throw new IllegalStateException("Tipo de nó não suportado: " + node.getClass());
        };
    }
}

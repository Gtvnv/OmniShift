package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.xml;

import br.com.github.gtvnv.omnishift.domain.model.*;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JacksonXmlSerializer implements DataSerializer {

    private final XmlMapper xmlMapper;

    public JacksonXmlSerializer() {
        this.xmlMapper = new XmlMapper();
    }

    @Override
    public String serialize(OmniNode node) {
        try {
            Object javaObject = mapToJavaObject(node);

            // Uma particularidade do XML: ele precisa de uma "Tag Raiz".
            // O Jackson usa "ObjectNode" por padrão, mas podemos customizar no futuro.
            return xmlMapper.writer()
                    .withRootName("OmniShiftDocument")
                    .writeValueAsString(javaObject);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar OmniNode para XML String", e);
        }
    }

    @Override
    public void serialize(OmniNode node, OutputStream outputStream) {
        try {
            Object javaObject = mapToJavaObject(node);
            xmlMapper.writer().withRootName("OmniShiftDocument").writeValue(outputStream, javaObject);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar OmniNode para XML OutputStream", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "XML"; // Gatilho lido pela SerializerFactory
    }

    /**
     * Motor de conversão usando o Pattern Matching do Java 21.
     * Independente do parser, a saída segue nossa regra de negócio canônica.
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
                    ""; // XML lida melhor com strings vazias do que com 'null' literal
            default ->
                    throw new IllegalStateException("Tipo de nó não suportado: " + node.getClass());
        };
    }
}
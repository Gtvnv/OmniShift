package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.xml;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.jackson.JacksonOmniNodeConverter;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.OutputStream;

public class JacksonXmlSerializer implements DataSerializer {

    private final XmlMapper xmlMapper;

    public JacksonXmlSerializer() {
        this.xmlMapper = new XmlMapper();
    }

    @Override
    public String serialize(OmniNode node) {
        try {
            // XML lida melhor com strings vazias do que com 'null' literal.
            Object javaObject = JacksonOmniNodeConverter.toJavaObject(node, "");

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
            Object javaObject = JacksonOmniNodeConverter.toJavaObject(node, "");
            xmlMapper.writer().withRootName("OmniShiftDocument").writeValue(outputStream, javaObject);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar OmniNode para XML OutputStream", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "XML"; // Gatilho lido pela SerializerFactory
    }
}
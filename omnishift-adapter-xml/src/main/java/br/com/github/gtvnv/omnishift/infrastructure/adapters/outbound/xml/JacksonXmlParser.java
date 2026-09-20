package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.xml;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.jackson.JacksonOmniNodeConverter;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import java.io.InputStream;

public class JacksonXmlParser implements DataParser {

    private final XmlMapper xmlMapper;

    public JacksonXmlParser() {
        // Previne XXE (XML External Entity): desliga DTD por completo, o que bloqueia
        // tanto entidades externas quanto expansão de entidade interna ("billion laughs").
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        XmlFactory xmlFactory = new XmlFactory(xmlInputFactory, XMLOutputFactory.newInstance());
        this.xmlMapper = new XmlMapper(xmlFactory);

        // Limite explícito de profundidade de aninhamento, como defesa em profundidade
        // contra payloads profundamente aninhados (evita depender do default implícito).
        this.xmlMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder().maxNestingDepth(500).build());
    }

    @Override
    public OmniNode parse(String payload) {
        try {
            // O Jackson pega as tags XML (<nome>Tavera</nome>) e converte num Node
            JsonNode rootNode = xmlMapper.readTree(payload);
            return JacksonOmniNodeConverter.toOmniNode(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o payload XML bruto", e);
        }
    }

    @Override
    public OmniNode parse(InputStream inputStream) {
        try {
            JsonNode rootNode = xmlMapper.readTree(inputStream);
            return JacksonOmniNodeConverter.toOmniNode(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o InputStream XML", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "XML"; // O gatilho mágico que a nossa ParserFactory vai capturar!
    }
}
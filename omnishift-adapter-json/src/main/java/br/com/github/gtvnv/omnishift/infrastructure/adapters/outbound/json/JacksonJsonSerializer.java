package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.json;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.jackson.JacksonOmniNodeConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;

public class JacksonJsonSerializer implements DataSerializer {

    private final ObjectMapper objectMapper;

    // Construtor sem argumentos exigido pelo java.util.ServiceLoader (descoberta via SPI)
    public JacksonJsonSerializer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String serialize(OmniNode node) {
        try {
            // Converte nosso OmniNode para estruturas padrão do Java (Map, List, Primitivos)
            Object javaObject = JacksonOmniNodeConverter.toJavaObject(node, null);
            // O Jackson pega as estruturas padrão e cospe a String JSON
            return objectMapper.writeValueAsString(javaObject);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar OmniNode para JSON String", e);
        }
    }

    @Override
    public void serialize(OmniNode node, OutputStream outputStream) {
        try {
            Object javaObject = JacksonOmniNodeConverter.toJavaObject(node, null);
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
}
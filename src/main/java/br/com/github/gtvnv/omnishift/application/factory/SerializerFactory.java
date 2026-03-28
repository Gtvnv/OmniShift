package br.com.github.gtvnv.omnishift.application.factory;

import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fábrica responsável por fornecer a implementação correta de DataSerializer 
 * com base no formato de saída solicitado.
 */
public class SerializerFactory {

    private final Map<String, DataSerializer> serializers;

    /**
     * O Spring injeta automaticamente todas as classes que implementam DataSerializer.
     */
    public SerializerFactory(List<DataSerializer> availableSerializers) {
        this.serializers = availableSerializers.stream()
                .collect(Collectors.toMap(
                        serializer -> serializer.getSupportedFormat().toUpperCase(),
                        serializer -> serializer
                ));
    }

    /**
     * Obtém o serializer adequado para o formato especificado.
     * * @param format O formato desejado (ex: "XML", "JSON").
     * @return A implementação de DataSerializer correspondente.
     */
    public DataSerializer getSerializer(String format) {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("O formato de saída não pode ser nulo ou vazio.");
        }

        DataSerializer serializer = serializers.get(format.toUpperCase());

        if (serializer == null) {
            throw new IllegalArgumentException("Formato de saída não suportado pelo OmniShift: " + format);
        }

        return serializer;
    }
}
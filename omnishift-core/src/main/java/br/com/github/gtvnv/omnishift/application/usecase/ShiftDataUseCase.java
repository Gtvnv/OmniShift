package br.com.github.gtvnv.omnishift.application.usecase;

import br.com.github.gtvnv.omnishift.application.dto.ShiftRequest;
import br.com.github.gtvnv.omnishift.application.factory.ParserFactory;
import br.com.github.gtvnv.omnishift.application.factory.SerializerFactory;
import br.com.github.gtvnv.omnishift.domain.mapping.FieldMapping;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import br.com.github.gtvnv.omnishift.domain.ports.MappingProfileRepository;
import br.com.github.gtvnv.omnishift.domain.service.TransformationEngine;

import java.util.List;

/**
 * Caso de uso principal responsável por orquestrar a transformação de dados.
 */
public class ShiftDataUseCase {

    private final ParserFactory parserFactory;
    private final SerializerFactory serializerFactory;
    private final TransformationEngine transformationEngine;
    private final MappingProfileRepository mappingProfileRepository;

    public ShiftDataUseCase(ParserFactory parserFactory, SerializerFactory serializerFactory) {
        this(parserFactory, serializerFactory, new TransformationEngine(), MappingProfileRepository.none());
    }

    public ShiftDataUseCase(ParserFactory parserFactory, SerializerFactory serializerFactory,
                             TransformationEngine transformationEngine,
                             MappingProfileRepository mappingProfileRepository) {
        this.parserFactory = parserFactory;
        this.serializerFactory = serializerFactory;
        this.transformationEngine = transformationEngine;
        this.mappingProfileRepository = mappingProfileRepository;
    }

    /**
     * Executa a conversão baseada nas instruções do ShiftRequest, sem reformatar campos.
     */
    public String execute(ShiftRequest request) {
        return execute(request, List.of());
    }

    /**
     * Executa a conversão aplicando os FieldMapping de um perfil nomeado (ver
     * MappingProfileRepository). Lança IllegalArgumentException se o perfil não existir.
     */
    public String executeWithProfile(ShiftRequest request, String profileName) {
        List<FieldMapping> mappings = mappingProfileRepository.findByName(profileName);
        return execute(request, mappings);
    }

    /**
     * Executa a conversão aplicando, além da troca de formato, um conjunto de
     * FieldMapping para reformatar a estrutura dos dados (ver TransformationEngine).
     */
    public String execute(ShiftRequest request, List<FieldMapping> mappings) {

        // 1. Identificar e recuperar o Parser correto (ex: JSON)
        // Note o uso dos métodos de acesso do 'record' (sem o prefixo 'get')
        DataParser parser = parserFactory.getParser(request.sourceFormat());

        // 2. Converter o dado bruto para o nosso "Esperanto" interno (Modelo Canônico)
        OmniNode canonicalData = parser.parse(request.rawPayload());

        // 3. Reformatar a estrutura de acordo com os FieldMapping fornecidos
        OmniNode transformedData = transformationEngine.transform(canonicalData, mappings);

        // 4. Identificar e recuperar o Serializer correto (ex: XML)
        DataSerializer serializer = serializerFactory.getSerializer(request.targetFormat());

        // 5. Converter o Modelo Canônico para o formato final desejado e retornar
        return serializer.serialize(transformedData);
    }
}
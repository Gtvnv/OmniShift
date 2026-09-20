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

import java.io.InputStream;
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

    /**
     * Variante em streaming de parse+transformação: lê de um InputStream em vez de
     * uma String já inteira em memória. Separada da serialização de propósito — ver
     * {@link #resolveSerializer(String)} — para que erros de entrada (formato inválido,
     * payload malformado, perfil inexistente) aconteçam antes de qualquer escrita de saída.
     */
    public OmniNode parseAndTransform(InputStream rawPayload, String sourceFormat, List<FieldMapping> mappings) {
        DataParser parser = parserFactory.getParser(sourceFormat);
        OmniNode canonicalData = parser.parse(rawPayload);
        return transformationEngine.transform(canonicalData, mappings);
    }

    /**
     * Como {@link #parseAndTransform(InputStream, String, List)}, mas resolvendo os
     * FieldMapping a partir de um perfil nomeado (ver MappingProfileRepository).
     */
    public OmniNode parseAndTransformWithProfile(InputStream rawPayload, String sourceFormat, String profileName) {
        List<FieldMapping> mappings = mappingProfileRepository.findByName(profileName);
        return parseAndTransform(rawPayload, sourceFormat, mappings);
    }

    /**
     * Resolve o serializer do formato de destino sem ainda escrever nada — permite ao
     * chamador validar o formato (e falhar cedo) antes de iniciar uma resposta em streaming.
     */
    public DataSerializer resolveSerializer(String targetFormat) {
        return serializerFactory.getSerializer(targetFormat);
    }
}
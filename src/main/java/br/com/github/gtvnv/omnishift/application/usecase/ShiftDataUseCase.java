package br.com.github.gtvnv.omnishift.application.usecase;

import br.com.github.gtvnv.omnishift.application.dto.ShiftRequest;
import br.com.github.gtvnv.omnishift.application.factory.ParserFactory;
import br.com.github.gtvnv.omnishift.application.factory.SerializerFactory;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import org.springframework.stereotype.Service;

/**
 * Caso de uso principal responsável por orquestrar a transformação de dados.
 */
@Service // Avisa o Spring para gerenciar essa classe e injetá-la no Controller
public class ShiftDataUseCase {

    private final ParserFactory parserFactory;
    private final SerializerFactory serializerFactory;

    // O Spring injeta as fábricas automaticamente aqui
    public ShiftDataUseCase(ParserFactory parserFactory, SerializerFactory serializerFactory) {
        this.parserFactory = parserFactory;
        this.serializerFactory = serializerFactory;
    }

    /**
     * Executa a conversão baseada nas instruções do ShiftRequest.
     */
    public String execute(ShiftRequest request) {

        // 1. Identificar e recuperar o Parser correto (ex: JSON)
        // Note o uso dos métodos de acesso do 'record' (sem o prefixo 'get')
        DataParser parser = parserFactory.getParser(request.sourceFormat());

        // 2. Converter o dado bruto para o nosso "Esperanto" interno (Modelo Canônico)
        OmniNode canonicalData = parser.parse(request.rawPayload());

        // 3. (Futuro) Interceptadores de Transformação entrariam aqui

        // 4. Identificar e recuperar o Serializer correto (ex: XML)
        DataSerializer serializer = serializerFactory.getSerializer(request.targetFormat());

        // 5. Converter o Modelo Canônico para o formato final desejado e retornar
        return serializer.serialize(canonicalData);
    }
}
package br.com.github.gtvnv.omnishift.infrastructure.config;

import br.com.github.gtvnv.omnishift.application.factory.ParserFactory;
import br.com.github.gtvnv.omnishift.application.factory.SerializerFactory;
import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Classe de configuração central do OmniShift.
 * Ensina o Spring como instanciar as fábricas do nosso domínio puro.
 */
@Configuration
public class OmniShiftConfig {

    /**
     * O Spring vai procurar por TODAS as classes no projeto que implementem DataParser
     * (como o nosso JacksonJsonParser) e injetar automaticamente nessa lista.
     */
    @Bean
    public ParserFactory parserFactory(List<DataParser> availableParsers) {
        return new ParserFactory(availableParsers);
    }

    /**
     * O Spring vai procurar por TODAS as classes no projeto que implementem DataSerializer
     * e injetar aqui.
     */
    @Bean
    public SerializerFactory serializerFactory(List<DataSerializer> availableSerializers) {
        return new SerializerFactory(availableSerializers);
    }
}
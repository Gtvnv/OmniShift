package br.com.github.gtvnv.omnishift.infrastructure.config;

import br.com.github.gtvnv.omnishift.application.factory.ParserFactory;
import br.com.github.gtvnv.omnishift.application.factory.SerializerFactory;
import br.com.github.gtvnv.omnishift.application.usecase.ShiftDataUseCase;
import br.com.github.gtvnv.omnishift.domain.mapping.FieldMapping;
import br.com.github.gtvnv.omnishift.domain.ports.MappingProfileRepository;
import br.com.github.gtvnv.omnishift.domain.service.TransformationEngine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Ponte entre a descoberta de plugins via Java SPI (feita no omnishift-core)
 * e o container de injeção de dependência do Spring. Nenhum adapter de formato
 * é conhecido por nome aqui: novos módulos "omnishift-adapter-*" na classpath
 * são descobertos automaticamente via META-INF/services.
 */
@Configuration
@EnableConfigurationProperties({MappingProfilesProperties.class, ApiKeyProperties.class})
public class OmniShiftConfig {

    @Bean
    public ParserFactory parserFactory() {
        return ParserFactory.discover();
    }

    @Bean
    public SerializerFactory serializerFactory() {
        return SerializerFactory.discover();
    }

    @Bean
    public MappingProfileRepository mappingProfileRepository(MappingProfilesProperties properties) {
        return profileName -> {
            List<FieldMapping> mappings = properties.getMappingProfiles().get(profileName);
            if (mappings == null || mappings.isEmpty()) {
                throw new IllegalArgumentException("Perfil de mapeamento não encontrado: " + profileName);
            }
            return mappings;
        };
    }

    @Bean
    public ShiftDataUseCase shiftDataUseCase(ParserFactory parserFactory, SerializerFactory serializerFactory,
                                              MappingProfileRepository mappingProfileRepository) {
        return new ShiftDataUseCase(parserFactory, serializerFactory, new TransformationEngine(), mappingProfileRepository);
    }
}
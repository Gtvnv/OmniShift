package br.com.github.gtvnv.omnishift.application.usecase;

import br.com.github.gtvnv.omnishift.application.dto.ShiftRequest;
import br.com.github.gtvnv.omnishift.application.factory.ParserFactory;
import br.com.github.gtvnv.omnishift.application.factory.SerializerFactory;
import br.com.github.gtvnv.omnishift.domain.mapping.FieldMapping;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;
import br.com.github.gtvnv.omnishift.domain.model.OmniValue;
import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import br.com.github.gtvnv.omnishift.domain.ports.MappingProfileRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShiftDataUseCaseTest {

    private static final String FORMAT = "FAKE";

    // Parser/serializer de teste: evitam depender de um adapter real de formato
    // (JSON/XML vivem em outros módulos, indisponíveis para omnishift-core).
    // Implementam tanto o caminho String quanto o de InputStream/OutputStream, para
    // exercitar de verdade a variante em streaming do ShiftDataUseCase.
    private static final DataParser FAKE_PARSER = new DataParser() {
        @Override
        public OmniNode parse(String payload) {
            OmniObject node = new OmniObject();
            node.put("nome", new OmniValue("Tavera"));
            return node;
        }

        @Override
        public OmniNode parse(InputStream inputStream) {
            try {
                inputStream.readAllBytes(); // consome o stream, prova que a integração é real
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return parse("ignorado");
        }

        @Override
        public String getSupportedFormat() {
            return FORMAT;
        }
    };

    private static final DataSerializer FAKE_SERIALIZER = new DataSerializer() {
        @Override
        public String serialize(OmniNode node) {
            return describe(node);
        }

        @Override
        public void serialize(OmniNode node, OutputStream outputStream) {
            try {
                outputStream.write(describe(node).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public String getSupportedFormat() {
            return FORMAT;
        }
    };

    private static String describe(OmniNode node) {
        if (node.isObject()) {
            Map<String, OmniNode> properties = node.asObject().getProperties();
            return properties.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + describe(entry.getValue()))
                    .collect(Collectors.joining(","));
        }
        if (node.isValue()) {
            return String.valueOf(((OmniValue) node).getValue());
        }
        return "null";
    }

    private ShiftDataUseCase newUseCase(MappingProfileRepository mappingProfileRepository) {
        ParserFactory parserFactory = new ParserFactory(List.of(FAKE_PARSER));
        SerializerFactory serializerFactory = new SerializerFactory(List.of(FAKE_SERIALIZER));
        return new ShiftDataUseCase(parserFactory, serializerFactory,
                new br.com.github.gtvnv.omnishift.domain.service.TransformationEngine(), mappingProfileRepository);
    }

    @Test
    void executeSemPerfilFazPassthrough() {
        ShiftDataUseCase useCase = newUseCase(MappingProfileRepository.none());
        ShiftRequest request = new ShiftRequest("qualquerPayload", FORMAT, FORMAT);

        assertEquals("nome=Tavera", useCase.execute(request));
    }

    @Test
    void executeWithProfileAplicaOsMappingsDoPerfil() {
        MappingProfileRepository repository = profileName ->
                "perfilTeste".equals(profileName)
                        ? List.of(new FieldMapping("nome", "fullName"))
                        : MappingProfileRepository.none().findByName(profileName);

        ShiftDataUseCase useCase = newUseCase(repository);
        ShiftRequest request = new ShiftRequest("qualquerPayload", FORMAT, FORMAT);

        assertEquals("fullName=Tavera", useCase.executeWithProfile(request, "perfilTeste"));
    }

    @Test
    void executeWithProfilePerfilInexistenteLancaIllegalArgumentException() {
        ShiftDataUseCase useCase = newUseCase(MappingProfileRepository.none());
        ShiftRequest request = new ShiftRequest("qualquerPayload", FORMAT, FORMAT);

        assertThrows(IllegalArgumentException.class,
                () -> useCase.executeWithProfile(request, "naoExiste"));
    }

    @Test
    void parseAndTransformAplicaMappingsAPartirDeUmInputStream() {
        ShiftDataUseCase useCase = newUseCase(MappingProfileRepository.none());
        InputStream input = new ByteArrayInputStream("qualquerPayload".getBytes(StandardCharsets.UTF_8));

        OmniNode result = useCase.parseAndTransform(input, FORMAT, List.of(new FieldMapping("nome", "fullName")));

        assertEquals("Tavera", ((OmniValue) result.asObject().get("fullName")).getValue());
    }

    @Test
    void resolveSerializerEscreveNoOutputStream() {
        ShiftDataUseCase useCase = newUseCase(MappingProfileRepository.none());
        InputStream input = new ByteArrayInputStream("qualquerPayload".getBytes(StandardCharsets.UTF_8));
        OmniNode node = useCase.parseAndTransform(input, FORMAT, List.of());

        DataSerializer serializer = useCase.resolveSerializer(FORMAT);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.serialize(node, output);

        assertEquals("nome=Tavera", output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void parseAndTransformWithProfilePerfilInexistenteLancaIllegalArgumentException() {
        ShiftDataUseCase useCase = newUseCase(MappingProfileRepository.none());
        InputStream input = new ByteArrayInputStream("qualquerPayload".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.parseAndTransformWithProfile(input, FORMAT, "naoExiste"));
    }
}

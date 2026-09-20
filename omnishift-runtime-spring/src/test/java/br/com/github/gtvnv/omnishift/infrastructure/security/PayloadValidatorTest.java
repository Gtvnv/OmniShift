package br.com.github.gtvnv.omnishift.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayloadValidatorTest {

    private final PayloadValidator validator = new PayloadValidator();

    @Test
    void validateAceitaPayloadDentroDoLimite() {
        validator.validate("payload pequeno");
    }

    @Test
    void validateRejeitaPayloadAcimaDoLimite() {
        String payloadGrande = "a".repeat(1_000_001);

        assertThrows(IllegalArgumentException.class, () -> validator.validate(payloadGrande));
    }

    @Test
    void limitPermiteLerStreamDentroDoLimiteIntegralmente() throws IOException {
        byte[] conteudo = "payload pequeno".getBytes(StandardCharsets.UTF_8);
        InputStream limited = validator.limit(new ByteArrayInputStream(conteudo));

        byte[] lido = limited.readAllBytes();

        assertEquals(conteudo.length, lido.length);
    }

    @Test
    void limitLancaAoLerStreamAcimaDoLimite() {
        byte[] conteudoGrande = new byte[1_000_001];
        InputStream limited = validator.limit(new ByteArrayInputStream(conteudoGrande));

        assertThrows(IllegalArgumentException.class, limited::readAllBytes);
    }

    @Test
    void chunkedPayloadAccumulatorAceitaPedacosDentroDoLimite() {
        PayloadValidator.ChunkedPayloadAccumulator accumulator = validator.newStreamingAccumulator();

        accumulator.append("parte1".getBytes(StandardCharsets.UTF_8));
        accumulator.append("parte2".getBytes(StandardCharsets.UTF_8));

        assertEquals("parte1parte2", new String(accumulator.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void chunkedPayloadAccumulatorLancaQuandoPedacosExcedemLimite() {
        PayloadValidator.ChunkedPayloadAccumulator accumulator = validator.newStreamingAccumulator();
        accumulator.append(new byte[25_000_000]);

        assertThrows(IllegalArgumentException.class, () -> accumulator.append(new byte[25_000_001]));
    }
}

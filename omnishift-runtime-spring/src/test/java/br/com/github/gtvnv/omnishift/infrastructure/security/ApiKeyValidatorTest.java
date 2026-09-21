package br.com.github.gtvnv.omnishift.infrastructure.security;

import br.com.github.gtvnv.omnishift.infrastructure.config.ApiKeyProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyValidatorTest {

    private final ApiKeyValidator validator = newValidator(Map.of(
            "cliente-a", "chave-a",
            "cliente-b", "chave-b"));

    @Test
    void chaveValidaResolveOClienteCerto() {
        assertEquals(Optional.of("cliente-a"), validator.resolveClientName("chave-a"));
        assertEquals(Optional.of("cliente-b"), validator.resolveClientName("chave-b"));
    }

    @Test
    void chaveInvalidaNaoResolveNenhumCliente() {
        assertTrue(validator.resolveClientName("chave-errada").isEmpty());
    }

    @Test
    void chaveNulaNaoResolveNenhumCliente() {
        assertTrue(validator.resolveClientName(null).isEmpty());
    }

    @Test
    void nenhumaChaveConfiguradaNuncaResolveCliente() {
        ApiKeyValidator semChaves = newValidator(Map.of());

        assertTrue(semChaves.resolveClientName("qualquer-coisa").isEmpty());
    }

    private static ApiKeyValidator newValidator(Map<String, String> apiKeys) {
        ApiKeyProperties properties = new ApiKeyProperties();
        properties.setApiKeys(apiKeys);
        return new ApiKeyValidator(properties);
    }
}

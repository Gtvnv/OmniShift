package br.com.github.gtvnv.omnishift.infrastructure.security;

import br.com.github.gtvnv.omnishift.infrastructure.config.ApiKeyProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

/**
 * Lógica pura de validação de API key, sem depender de Servlet API nem de gRPC — o que
 * permite testar com JUnit puro (o projeto nunca usou Mockito, e as interfaces de
 * HttpServletRequest/Metadata são grandes demais pra fake escrito à mão).
 */
@Component
public class ApiKeyValidator {

    private final Map<String, String> apiKeys;

    public ApiKeyValidator(ApiKeyProperties properties) {
        this.apiKeys = properties.getApiKeys();
    }

    public Optional<String> resolveClientName(String suppliedKey) {
        if (suppliedKey == null) {
            return Optional.empty();
        }
        for (Map.Entry<String, String> entry : apiKeys.entrySet()) {
            // MessageDigest.isEqual: comparação em tempo constante, para não vazar via
            // timing quantos caracteres da chave bateram.
            if (MessageDigest.isEqual(
                    suppliedKey.getBytes(StandardCharsets.UTF_8),
                    entry.getValue().getBytes(StandardCharsets.UTF_8))) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }
}

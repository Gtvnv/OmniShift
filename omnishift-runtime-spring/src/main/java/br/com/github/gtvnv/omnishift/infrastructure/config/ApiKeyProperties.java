package br.com.github.gtvnv.omnishift.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Chaves de API por cliente, configuradas em application.yml sob o prefixo
 * "omnishift.security". Chave = nome do cliente (usado em log/auditoria), valor = chave
 * secreta esperada no header X-Api-Key (REST) / metadata x-api-key (gRPC).
 */
@ConfigurationProperties(prefix = "omnishift.security")
public class ApiKeyProperties {

    private Map<String, String> apiKeys = new HashMap<>();

    public Map<String, String> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(Map<String, String> apiKeys) {
        this.apiKeys = apiKeys;
    }
}

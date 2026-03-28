package br.com.github.gtvnv.omnishift.domain.ports;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import java.io.InputStream;

public interface DataParser {

    /**
     * Converte um payload em texto puro para o Modelo Canônico.
     * Ideal para requisições REST síncronas com payloads pequenos/médios.
     */
    OmniNode parse(String payload);

    /**
     * Lê um fluxo de dados e converte para o Modelo Canônico.
     * Essencial para a feature de STREAMING (Arquivos grandes via Kafka/gRPC).
     */
    OmniNode parse(InputStream inputStream);

    /**
     * Retorna o formato que este parser suporta (ex: "JSON", "XML", "CSV").
     * Isso ajudará a nossa Factory a escolher o parser correto dinamicamente.
     */
    String getSupportedFormat();
}
package br.com.github.gtvnv.omnishift.domain.ports;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import java.io.OutputStream;

public interface DataSerializer {

    /**
     * Converte o Modelo Canônico para uma String formatada.
     */
    String serialize(OmniNode node);

    /**
     * Escreve o Modelo Canônico diretamente em um fluxo de saída.
     * Essencial para STREAMING de alta performance (ex: gravar direto no disco ou no response gRPC).
     */
    void serialize(OmniNode node, OutputStream outputStream);

    /**
     * Retorna o formato que este serializer produz (ex: "GRAPHQL", "XML", "YAML").
     */
    String getSupportedFormat();


}
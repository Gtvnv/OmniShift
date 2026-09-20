package br.com.github.gtvnv.omnishift.domain.service;

import br.com.github.gtvnv.omnishift.domain.mapping.FieldMapping;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;

import java.util.List;

public class TransformationEngine {

    /**
     * Aplica uma lista de FieldMapping ao nó de entrada. Sem mappings, retorna o nó
     * original inalterado (passthrough). Com mappings, o resultado contém exclusivamente
     * os campos explicitamente mapeados (allow-list) — campos de origem não referenciados
     * em nenhum FieldMapping não aparecem no destino.
     */
    public OmniNode transform(OmniNode node, List<FieldMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return node;
        }

        OmniObject result = new OmniObject();
        for (FieldMapping mapping : mappings) {
            OmniNode value = OmniPath.read(node, mapping.sourcePath());
            OmniPath.write(result, mapping.targetPath(), value);
        }
        return result;
    }
}

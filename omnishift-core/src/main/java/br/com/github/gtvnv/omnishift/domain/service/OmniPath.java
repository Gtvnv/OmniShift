package br.com.github.gtvnv.omnishift.domain.service;

import br.com.github.gtvnv.omnishift.domain.exception.DataShiftException;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniNull;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve e constrói caminhos estilo "a.b[0].c" sobre o modelo canônico (OmniNode).
 * Usado pelo TransformationEngine para aplicar FieldMapping.
 */
final class OmniPath {

    private static final Pattern SEGMENT = Pattern.compile("([^\\[\\]]+)((?:\\[\\d+])*)");
    private static final Pattern INDEX = Pattern.compile("\\[(\\d+)]");

    private OmniPath() {
    }

    /**
     * Navegação "safe": qualquer segmento ausente ou de tipo incompatível resolve
     * para OmniNull, em vez de lançar — mesma filosofia de OmniObject.get/OmniArray.get.
     */
    static OmniNode read(OmniNode root, String path) {
        OmniNode current = root;
        for (String rawSegment : path.split("\\.")) {
            current = readSegment(current, rawSegment);
        }
        return current;
    }

    private static OmniNode readSegment(OmniNode current, String rawSegment) {
        Matcher segmentMatcher = SEGMENT.matcher(rawSegment);
        if (!segmentMatcher.matches()) {
            throw new DataShiftException("Segmento de caminho inválido: '" + rawSegment + "'");
        }

        String key = segmentMatcher.group(1);
        current = current.isObject() ? current.asObject().get(key) : OmniNull.getInstance();

        Matcher indexMatcher = INDEX.matcher(segmentMatcher.group(2));
        while (indexMatcher.find()) {
            int index = Integer.parseInt(indexMatcher.group(1));
            current = current.isArray() ? current.asArray().get(index) : OmniNull.getInstance();
        }
        return current;
    }

    /**
     * Escreve 'value' em 'root' na posição indicada por 'path', criando OmniObjects
     * intermediários conforme necessário. Não suporta índice de array (write em
     * posição de array exigiria um OmniArray mutável por índice, que não existe hoje).
     * Lança DataShiftException em vez de sobrescrever silenciosamente um segmento
     * intermediário que já contenha um valor incompatível.
     */
    static void write(OmniObject root, String path, OmniNode value) {
        if (path.indexOf('[') >= 0) {
            throw new DataShiftException("targetPath não suporta índice de array: '" + path + "'");
        }

        String[] segments = path.split("\\.");
        OmniObject current = root;
        for (int i = 0; i < segments.length - 1; i++) {
            String key = segments[i];
            OmniNode next = current.get(key);
            if (next.isNull()) {
                OmniObject created = new OmniObject();
                current.put(key, created);
                current = created;
            } else if (next.isObject()) {
                current = next.asObject();
            } else {
                throw new DataShiftException(
                        "Conflito ao mapear para '" + path + "': '" + key + "' já contém um valor não-objeto.");
            }
        }
        current.put(segments[segments.length - 1], value);
    }
}

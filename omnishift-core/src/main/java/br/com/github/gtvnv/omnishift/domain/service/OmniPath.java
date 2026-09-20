package br.com.github.gtvnv.omnishift.domain.service;

import br.com.github.gtvnv.omnishift.domain.exception.DataShiftException;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniNull;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;

/**
 * Resolve e constrói caminhos estilo "a.b[0].c" sobre o modelo canônico (OmniNode).
 * Usado pelo TransformationEngine para aplicar FieldMapping.
 */
final class OmniPath {

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

    /**
     * Parsing manual (sem regex) de propósito: um padrão como "(?:\[\d+])*" combina
     * grupo repetido dentro de quantificador repetido, o que o motor de regex do Java
     * resolve recursivamente e pode estourar a pilha em entradas patológicas longas.
     */
    private static OmniNode readSegment(OmniNode current, String rawSegment) {
        int bracketStart = rawSegment.indexOf('[');
        String key = bracketStart < 0 ? rawSegment : rawSegment.substring(0, bracketStart);
        if (key.isEmpty() || key.indexOf(']') >= 0) {
            throw new DataShiftException("Segmento de caminho inválido: '" + rawSegment + "'");
        }
        current = current.isObject() ? current.asObject().get(key) : OmniNull.getInstance();

        int pos = bracketStart;
        while (pos >= 0) {
            int bracketEnd = rawSegment.indexOf(']', pos);
            String indexText = bracketEnd < 0 ? "" : rawSegment.substring(pos + 1, bracketEnd);
            if (bracketEnd < 0 || indexText.isEmpty() || !isDigits(indexText)) {
                throw new DataShiftException("Segmento de caminho inválido: '" + rawSegment + "'");
            }
            current = current.isArray() ? current.asArray().get(Integer.parseInt(indexText)) : OmniNull.getInstance();

            int next = bracketEnd + 1;
            if (next == rawSegment.length()) {
                pos = -1;
            } else if (rawSegment.charAt(next) == '[') {
                pos = next;
            } else {
                throw new DataShiftException("Segmento de caminho inválido: '" + rawSegment + "'");
            }
        }
        return current;
    }

    private static boolean isDigits(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
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

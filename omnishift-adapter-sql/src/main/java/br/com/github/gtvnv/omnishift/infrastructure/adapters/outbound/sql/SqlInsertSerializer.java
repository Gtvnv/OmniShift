package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.sql;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniNull;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;
import br.com.github.gtvnv.omnishift.domain.model.OmniValue;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera instruções INSERT a partir de { "table": "...", "rows": [ {...}, ... ] } — essa
 * forma (não uma árvore genérica) é o contrato de entrada deste formato, do mesmo jeito
 * que CSV exige array-de-objetos-flat na raiz. Um INSERT por linha (nunca VALUES múltiplo
 * numa instrução só), porque Oracle não suporta multi-row VALUES.
 *
 * <p>Compartilhada pelas 4 subclasses de dialeto para não repetir esta lógica 4 vezes.
 */
class SqlInsertSerializer implements DataSerializer {

    private final SqlDialect dialect;
    private final String format;

    SqlInsertSerializer(SqlDialect dialect, String format) {
        this.dialect = dialect;
        this.format = format;
    }

    @Override
    public String serialize(OmniNode node) {
        StringWriter writer = new StringWriter();
        serializeInternal(node, writer);
        return writer.toString();
    }

    @Override
    public void serialize(OmniNode node, OutputStream outputStream) {
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        serializeInternal(node, writer);
        try {
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao serializar OmniNode para SQL (" + format + ") OutputStream", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return format;
    }

    private void serializeInternal(OmniNode node, Writer writer) {
        if (!node.isObject()) {
            throw new IllegalArgumentException(
                    "Serialização SQL exige um objeto raiz no formato { \"table\": ..., \"rows\": [...] }.");
        }
        OmniObject root = node.asObject();
        String table = requireTableName(root.get("table"));
        List<OmniNode> rows = requireRows(root.get("rows"));

        try {
            for (OmniNode rowNode : rows) {
                if (!rowNode.isObject()) {
                    throw new IllegalArgumentException("Cada elemento de 'rows' deve ser um objeto (uma linha).");
                }
                writer.write(buildInsert(table, rowNode.asObject()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao serializar OmniNode para SQL (" + format + ")", e);
        }
    }

    private String buildInsert(String table, OmniObject row) {
        List<String> columns = new ArrayList<>(row.getProperties().keySet());

        StringBuilder columnList = new StringBuilder();
        StringBuilder valueList = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                columnList.append(", ");
                valueList.append(", ");
            }
            columnList.append(dialect.quoteIdentifier(columns.get(i)));
            valueList.append(formatValue(row.get(columns.get(i))));
        }

        return "INSERT INTO " + dialect.quoteIdentifier(table) + " (" + columnList + ") VALUES (" + valueList + ");\n";
    }

    private String formatValue(OmniNode value) {
        return switch (value) {
            case OmniNull ignored -> "NULL";
            case OmniValue v -> formatScalar(v.getValue());
            default -> throw new IllegalArgumentException(
                    "SQL não suporta valores aninhados (objetos/arrays) numa célula.");
        };
    }

    private String formatScalar(Object value) {
        if (value instanceof Boolean b) {
            return dialect.formatBoolean(b);
        }
        if (value instanceof Number n) {
            // Number real (nunca String bruta do cliente quando a origem tem tipo, ex:
            // JSON/XML/YAML) — toString() de um Number Java não pode conter aspas/';'.
            return n.toString();
        }
        // String (inclusive valor "numérico" vindo de CSV, que não faz inferência de tipo):
        // aspas simples dobradas, padrão ANSI que funciona nos 4 dialetos.
        return "'" + String.valueOf(value).replace("'", "''") + "'";
    }

    private String requireTableName(OmniNode tableNode) {
        if (tableNode.isValue() && ((OmniValue) tableNode).getValue() instanceof String tableName
                && !tableName.isBlank()) {
            return tableName;
        }
        throw new IllegalArgumentException("Campo 'table' (nome da tabela) é obrigatório e deve ser uma string.");
    }

    private List<OmniNode> requireRows(OmniNode rowsNode) {
        if (!rowsNode.isArray()) {
            throw new IllegalArgumentException("Campo 'rows' é obrigatório e deve ser um array de objetos.");
        }
        return rowsNode.asArray().getElements();
    }
}

package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.csv;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniNull;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;
import br.com.github.gtvnv.omnishift.domain.model.OmniValue;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class CsvSerializer implements DataSerializer {

    // Caracteres que o Excel/Google Sheets pode interpretar como início de fórmula ao
    // abrir o CSV exportado ("CSV Injection" / "Formula Injection", OWASP Cheat Sheet).
    private static final String FORMULA_TRIGGER_CHARS = "=+-@\t\r";

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
            throw new RuntimeException("Falha ao serializar OmniNode para CSV OutputStream", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "CSV";
    }

    private void serializeInternal(OmniNode node, Writer writer) {
        if (!node.isArray()) {
            throw new IllegalArgumentException("CSV exige um array de objetos no nível raiz para serializar.");
        }
        List<OmniNode> rows = node.asArray().getElements();

        LinkedHashSet<String> headers = new LinkedHashSet<>();
        for (OmniNode row : rows) {
            requireFlatObject(row);
            headers.addAll(row.asObject().getProperties().keySet());
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(headers.toArray(new String[0]))
                .build();

        try (CSVPrinter printer = format.print(writer)) {
            for (OmniNode row : rows) {
                OmniObject obj = row.asObject();
                List<String> values = new ArrayList<>();
                for (String header : headers) {
                    values.add(cellValue(obj.get(header)));
                }
                printer.printRecord(values);
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao serializar OmniNode para CSV", e);
        }
    }

    private void requireFlatObject(OmniNode row) {
        if (!row.isObject()) {
            throw new IllegalArgumentException("Cada elemento do array CSV deve ser um objeto (uma linha).");
        }
        for (OmniNode value : row.asObject().getProperties().values()) {
            if (value.isObject() || value.isArray()) {
                throw new IllegalArgumentException(
                        "CSV não suporta valores aninhados (objetos/arrays) dentro de uma linha.");
            }
        }
    }

    private String cellValue(OmniNode value) {
        String raw = switch (value) {
            case OmniNull ignored -> "";
            case OmniValue v -> String.valueOf(v.getValue());
            default -> throw new IllegalStateException("Tipo de nó não suportado numa célula CSV: " + value.getClass());
        };
        return sanitizeFormulaInjection(raw);
    }

    private String sanitizeFormulaInjection(String value) {
        if (!value.isEmpty() && FORMULA_TRIGGER_CHARS.indexOf(value.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }
}

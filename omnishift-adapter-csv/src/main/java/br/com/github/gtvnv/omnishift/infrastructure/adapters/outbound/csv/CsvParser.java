package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.csv;

import br.com.github.gtvnv.omnishift.domain.model.OmniArray;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;
import br.com.github.gtvnv.omnishift.domain.model.OmniValue;
import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvParser implements DataParser {

    // Primeira linha é sempre o cabeçalho; valores lidos como String pura, sem inferência
    // de tipo (CSV não carrega informação de tipo — inferir gera bugs sutis como "007"
    // virando número 7).
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();

    @Override
    public OmniNode parse(String payload) {
        try (Reader reader = new StringReader(payload)) {
            return parseInternal(reader);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar o payload CSV bruto", e);
        }
    }

    @Override
    public OmniNode parse(InputStream inputStream) {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return parseInternal(reader);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar o InputStream CSV", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "CSV";
    }

    private OmniNode parseInternal(Reader reader) throws IOException {
        OmniArray rows = new OmniArray();
        try (org.apache.commons.csv.CSVParser csvParser = FORMAT.parse(reader)) {
            List<String> headers = csvParser.getHeaderNames();
            for (CSVRecord record : csvParser) {
                OmniObject row = new OmniObject();
                for (String header : headers) {
                    row.put(header, new OmniValue(record.isSet(header) ? record.get(header) : ""));
                }
                rows.add(row);
            }
        }
        return rows;
    }
}

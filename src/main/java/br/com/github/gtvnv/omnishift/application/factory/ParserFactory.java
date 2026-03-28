package br.com.github.gtvnv.omnishift.application.factory;

import br.com.github.gtvnv.omnishift.domain.ports.DataParser;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fábrica responsável por fornecer a implementação correta de DataParser 
 * consoante o formato de entrada solicitado.
 */
public class ParserFactory {

    // Mantemos um mapa em memória para pesquisas rápidas O(1)
    private final Map<String, DataParser> parsers;

    /**
     * O Spring encarrega-se de injetar automaticamente todas as classes 
     * que implementem a interface DataParser nesta lista.
     * * @param availableParsers Lista de todos os parsers registados no sistema.
     */
    public ParserFactory(List<DataParser> availableParsers) {
        this.parsers = availableParsers.stream()
                .collect(Collectors.toMap(
                        parser -> parser.getSupportedFormat().toUpperCase(),
                        parser -> parser
                ));
    }

    /**
     * Obtém o parser adequado para o formato especificado.
     * * @param format O formato desejado (ex: "JSON", "XML").
     * @return A implementação de DataParser correspondente.
     * @throws IllegalArgumentException se o formato não for suportado.
     */
    public DataParser getParser(String format) {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("O formato de entrada não pode ser nulo ou vazio.");
        }

        DataParser parser = parsers.get(format.toUpperCase());

        if (parser == null) {
            throw new IllegalArgumentException("Formato não suportado pelo OmniShift: " + format);
        }

        return parser;
    }
}
package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.csv;

import br.com.github.gtvnv.omnishift.domain.model.OmniArray;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;
import br.com.github.gtvnv.omnishift.domain.model.OmniValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvParserTest {

    private final CsvParser parser = new CsvParser();

    @Test
    void parseiaDuasLinhasEmArrayDeObjetos() {
        String csv = "nome,idade\nTavera,20\nGustavo,21\n";

        OmniNode result = parser.parse(csv);

        OmniArray array = result.asArray();
        assertEquals(2, array.getElements().size());

        OmniObject linha0 = array.get(0).asObject();
        assertEquals("Tavera", ((OmniValue) linha0.get("nome")).getValue());
        assertEquals("20", ((OmniValue) linha0.get("idade")).getValue());
    }

    @Test
    void campoComVirgulaEntreAspasEPreservadoInteiro() {
        String csv = "cidade\n\"Recife, PE\"\n";

        OmniNode result = parser.parse(csv);

        assertEquals("Recife, PE", ((OmniValue) result.asArray().get(0).asObject().get("cidade")).getValue());
    }

    @Test
    void csvSoComCabecalhoViraArrayVazio() {
        String csv = "nome,idade\n";

        OmniNode result = parser.parse(csv);

        assertTrue(result.asArray().getElements().isEmpty());
    }

    @Test
    void valorLidoNuncaSofreInferenciaDeTipo() {
        String csv = "codigo\n007\n";

        OmniNode result = parser.parse(csv);

        assertEquals("007", ((OmniValue) result.asArray().get(0).asObject().get("codigo")).getValue());
    }
}

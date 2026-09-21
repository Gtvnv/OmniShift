package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.csv;

import br.com.github.gtvnv.omnishift.domain.model.OmniArray;
import br.com.github.gtvnv.omnishift.domain.model.OmniNull;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;
import br.com.github.gtvnv.omnishift.domain.model.OmniValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvSerializerTest {

    private final CsvSerializer serializer = new CsvSerializer();

    @Test
    void serializaArrayDeObjetosFlatComCabecalhoEValores() {
        OmniArray root = new OmniArray();
        OmniObject linha = new OmniObject();
        linha.put("nome", new OmniValue("Tavera"));
        linha.put("idade", new OmniValue(20));
        root.add(linha);

        String csv = serializer.serialize(root);

        assertEquals("nome,idade\r\nTavera,20\r\n", csv);
    }

    @Test
    void cabecalhoEUniaoDasChavesEChaveAusenteViraCelulaVazia() {
        OmniArray root = new OmniArray();
        OmniObject linha0 = new OmniObject();
        linha0.put("a", new OmniValue("1"));
        OmniObject linha1 = new OmniObject();
        linha1.put("a", new OmniValue("2"));
        linha1.put("b", new OmniValue("3"));
        root.add(linha0);
        root.add(linha1);

        String csv = serializer.serialize(root);

        assertEquals("a,b\r\n1,\r\n2,3\r\n", csv);
    }

    @Test
    void omniNullViraCelulaVazia() {
        OmniArray root = new OmniArray();
        OmniObject linha = new OmniObject();
        linha.put("apelido", OmniNull.getInstance());
        root.add(linha);

        String csv = serializer.serialize(root);

        // Commons CSV quota ("") um valor vazio quando ele é o único campo do registro,
        // pra não confundir uma linha "sem colunas" com uma linha de fato com valor vazio.
        assertEquals("apelido\r\n\"\"\r\n", csv);
    }

    @Test
    void raizNaoArrayLancaIllegalArgumentException() {
        OmniObject root = new OmniObject();

        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(root));
    }

    @Test
    void valorAninhadoDentroDeUmaLinhaLancaIllegalArgumentException() {
        OmniArray root = new OmniArray();
        OmniObject linha = new OmniObject();
        linha.put("itens", new OmniArray());
        root.add(linha);

        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(root));
    }

    @Test
    void celulaComFormulaMaliciosaSaiPrefixadaComAspaSimples() {
        OmniArray root = new OmniArray();
        OmniObject linha = new OmniObject();
        linha.put("comando", new OmniValue("=cmd|'/c calc'!A1"));
        root.add(linha);

        String csv = serializer.serialize(root);

        assertTrue(csv.contains("'=cmd|'/c calc'!A1") || csv.contains("\"'=cmd|'/c calc'!A1\""),
                "célula deveria estar prefixada com aspa simples para neutralizar a fórmula: " + csv);
    }
}

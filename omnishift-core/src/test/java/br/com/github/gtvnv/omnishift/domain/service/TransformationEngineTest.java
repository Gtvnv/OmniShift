package br.com.github.gtvnv.omnishift.domain.service;

import br.com.github.gtvnv.omnishift.domain.exception.DataShiftException;
import br.com.github.gtvnv.omnishift.domain.mapping.FieldMapping;
import br.com.github.gtvnv.omnishift.domain.model.OmniArray;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;
import br.com.github.gtvnv.omnishift.domain.model.OmniValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationEngineTest {

    private final TransformationEngine engine = new TransformationEngine();

    @Test
    void semMappingsRetornaOMesmoNoDeEntrada() {
        OmniObject input = new OmniObject();
        input.put("nome", new OmniValue("Tavera"));

        assertSame(input, engine.transform(input, null));
        assertSame(input, engine.transform(input, List.of()));
    }

    @Test
    void renomeiaCampoTopLevel() {
        OmniObject input = new OmniObject();
        input.put("nome", new OmniValue("Tavera"));

        OmniNode result = engine.transform(input, List.of(new FieldMapping("nome", "fullName")));

        assertEquals("Tavera", ((OmniValue) result.asObject().get("fullName")).getValue());
        assertTrue(result.asObject().get("nome").isNull());
    }

    @Test
    void criaEstruturaAninhadaNoDestino() {
        OmniObject input = new OmniObject();
        input.put("cidade", new OmniValue("Recife"));

        OmniNode result = engine.transform(input, List.of(new FieldMapping("cidade", "endereco.cidade")));

        OmniNode endereco = result.asObject().get("endereco");
        assertEquals("Recife", ((OmniValue) endereco.asObject().get("cidade")).getValue());
    }

    @Test
    void leElementoDeArrayNaOrigem() {
        OmniObject input = new OmniObject();
        OmniArray itens = new OmniArray();
        OmniObject item0 = new OmniObject();
        item0.put("nome", new OmniValue("Java 21"));
        itens.add(item0);
        input.put("itens", itens);

        OmniNode result = engine.transform(input, List.of(new FieldMapping("itens[0].nome", "primeiraHabilidade")));

        assertEquals("Java 21", ((OmniValue) result.asObject().get("primeiraHabilidade")).getValue());
    }

    @Test
    void sourcePathInexistenteViraOmniNullNoDestino() {
        OmniObject input = new OmniObject();

        OmniNode result = engine.transform(input, List.of(new FieldMapping("naoExiste", "destino")));

        assertTrue(result.asObject().get("destino").isNull());
    }

    @Test
    void leIndicesEncadeadosNoMesmoSegmento() {
        OmniObject input = new OmniObject();
        OmniArray linha0 = new OmniArray();
        linha0.add(new OmniValue("a"));
        linha0.add(new OmniValue("b"));
        OmniArray matriz = new OmniArray();
        matriz.add(linha0);
        input.put("matriz", matriz);

        OmniNode result = engine.transform(input, List.of(new FieldMapping("matriz[0][1]", "destino")));

        assertEquals("b", ((OmniValue) result.asObject().get("destino")).getValue());
    }

    @Test
    void segmentoDeCaminhoInvalidoLancaDataShiftException() {
        OmniObject input = new OmniObject();
        input.put("a", new OmniValue("valor"));

        List<FieldMapping> mappings = List.of(new FieldMapping("a[x]", "destino"));

        assertThrows(DataShiftException.class, () -> engine.transform(input, mappings));
    }

    @Test
    void conflitoDeEscritaLancaDataShiftException() {
        OmniObject input = new OmniObject();
        input.put("a", new OmniValue("valorFolha"));
        input.put("b", new OmniValue("outroValor"));

        List<FieldMapping> mappings = List.of(
                new FieldMapping("a", "usuario"),
                new FieldMapping("b", "usuario.nome")
        );

        assertThrows(DataShiftException.class, () -> engine.transform(input, mappings));
    }
}

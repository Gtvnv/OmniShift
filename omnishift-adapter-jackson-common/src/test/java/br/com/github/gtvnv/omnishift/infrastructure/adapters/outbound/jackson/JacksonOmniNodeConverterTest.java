package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.jackson;

import br.com.github.gtvnv.omnishift.domain.model.OmniArray;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;
import br.com.github.gtvnv.omnishift.domain.model.OmniValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonOmniNodeConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toOmniNodeConverteObjetoAninhadoComTodosOsTiposPrimitivos() throws Exception {
        JsonNode json = objectMapper.readTree("""
                {
                  "nome": "Tavera",
                  "idade": 20,
                  "ativo": true,
                  "apelido": null,
                  "habilidades": ["Java", "Arquitetura"]
                }
                """);

        OmniNode result = JacksonOmniNodeConverter.toOmniNode(json);

        OmniObject obj = result.asObject();
        assertEquals("Tavera", ((OmniValue) obj.get("nome")).getValue());
        assertEquals(20, ((Number) ((OmniValue) obj.get("idade")).getValue()).intValue());
        assertEquals(true, ((OmniValue) obj.get("ativo")).getValue());
        assertTrue(obj.get("apelido").isNull());
        assertEquals("Java", ((OmniValue) obj.get("habilidades").asArray().get(0)).getValue());
    }

    @Test
    void toJavaObjectConverteEstruturaCompletaUsandoNullRepresentationInformado() {
        OmniObject root = new OmniObject();
        root.put("nome", new OmniValue("Tavera"));
        root.put("apelido", br.com.github.gtvnv.omnishift.domain.model.OmniNull.getInstance());
        OmniArray habilidades = new OmniArray();
        habilidades.add(new OmniValue("Java"));
        root.put("habilidades", habilidades);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) JacksonOmniNodeConverter.toJavaObject(root, "");

        assertEquals("Tavera", result.get("nome"));
        assertEquals("", result.get("apelido"));
        assertEquals(List.of("Java"), result.get("habilidades"));
    }

    @Test
    void toJavaObjectUsaNullJavaQuandoNullRepresentationENull() {
        OmniObject root = new OmniObject();
        root.put("apelido", br.com.github.gtvnv.omnishift.domain.model.OmniNull.getInstance());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) JacksonOmniNodeConverter.toJavaObject(root, null);

        assertNull(result.get("apelido"));
        assertTrue(result.containsKey("apelido"));
    }
}

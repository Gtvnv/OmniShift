package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.yaml;

import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.model.OmniValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonYamlParserTest {

    private final JacksonYamlParser parser = new JacksonYamlParser();

    @Test
    void parseiaEstruturaBasica() {
        OmniNode result = parser.parse("nome: Tavera\nidade: 20\n");

        assertEquals("Tavera", ((OmniValue) result.asObject().get("nome")).getValue());
        assertEquals(20, ((Number) ((OmniValue) result.asObject().get("idade")).getValue()).intValue());
    }

    /**
     * Comportamento verificado empiricamente, não assumido: o parser de YAML do Jackson
     * (baseado em eventos, sem a fase de "compose" completa do SnakeYAML) não resolve
     * &ancora / *alias / merge keys (<<) — eles chegam como texto literal com o nome da
     * âncora. Por isso o ataque clássico de "YAML bomb" (expansão exponencial via aliases)
     * não tem superfície aqui: não há nada para o parser expandir. Se uma futura versão do
     * jackson-dataformat-yaml passar a resolver aliases, este teste falha — sinal de que
     * LoaderOptions.setMaxAliasesForCollections precisaria ser configurado de fato.
     */
    @Test
    void aliasesDeYamlNaoSaoExpandidos() {
        String yamlComAlias = """
                base: &ancora
                  - lol
                  - lol
                usado:
                  - *ancora
                  - *ancora
                """;

        OmniNode result = parser.parse(yamlComAlias);

        OmniNode usado = result.asObject().get("usado");
        assertEquals(2, usado.asArray().getElements().size());
        assertEquals("ancora", ((OmniValue) usado.asArray().get(0)).getValue());
        assertEquals("ancora", ((OmniValue) usado.asArray().get(1)).getValue());
    }

    @Test
    void mergeKeyDeYamlNaoEResolvida() {
        String yamlComMerge = """
                base: &b
                  x: 1
                filho:
                  <<: *b
                  y: 2
                """;

        OmniNode result = parser.parse(yamlComMerge);

        OmniNode filho = result.asObject().get("filho");
        assertEquals("b", ((OmniValue) filho.asObject().get("<<")).getValue());
        assertEquals(2, ((Number) ((OmniValue) filho.asObject().get("y")).getValue()).intValue());
    }
}

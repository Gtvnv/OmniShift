package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.sql;

import br.com.github.gtvnv.omnishift.domain.model.OmniArray;
import br.com.github.gtvnv.omnishift.domain.model.OmniNull;
import br.com.github.gtvnv.omnishift.domain.model.OmniObject;
import br.com.github.gtvnv.omnishift.domain.model.OmniValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlInsertSerializerTest {

    private final MySqlInsertSerializer mysql = new MySqlInsertSerializer();

    @Test
    void geraUmInsertPorLinha() {
        OmniObject root = new OmniObject();
        root.put("table", new OmniValue("usuarios"));
        OmniArray rows = new OmniArray();
        OmniObject linha0 = new OmniObject();
        linha0.put("nome", new OmniValue("Tavera"));
        linha0.put("idade", new OmniValue(20));
        rows.add(linha0);
        OmniObject linha1 = new OmniObject();
        linha1.put("nome", new OmniValue("Gustavo"));
        linha1.put("idade", new OmniValue(21));
        rows.add(linha1);
        root.put("rows", rows);

        String sql = mysql.serialize(root);

        assertEquals(
                "INSERT INTO `usuarios` (`nome`, `idade`) VALUES ('Tavera', 20);\n"
                        + "INSERT INTO `usuarios` (`nome`, `idade`) VALUES ('Gustavo', 21);\n",
                sql);
    }

    @Test
    void omniNullViraNull() {
        OmniObject root = new OmniObject();
        root.put("table", new OmniValue("usuarios"));
        OmniArray rows = new OmniArray();
        OmniObject linha = new OmniObject();
        linha.put("apelido", OmniNull.getInstance());
        rows.add(linha);
        root.put("rows", rows);

        String sql = mysql.serialize(root);

        assertEquals("INSERT INTO `usuarios` (`apelido`) VALUES (NULL);\n", sql);
    }

    @Test
    void booleanUsaConvencaoDoDialeto() {
        OmniObject root = new OmniObject();
        root.put("table", new OmniValue("flags"));
        OmniArray rows = new OmniArray();
        OmniObject linha = new OmniObject();
        linha.put("ativo", new OmniValue(true));
        rows.add(linha);
        root.put("rows", rows);

        assertEquals("INSERT INTO `flags` (`ativo`) VALUES (TRUE);\n", mysql.serialize(root));
        assertEquals("INSERT INTO \"flags\" (\"ativo\") VALUES (1);\n", new OracleInsertSerializer().serialize(root));
        assertEquals("INSERT INTO [flags] ([ativo]) VALUES (1);\n", new SqlServerInsertSerializer().serialize(root));
    }

    @Test
    void tabelaAusenteLancaIllegalArgumentException() {
        OmniObject root = new OmniObject();
        root.put("rows", new OmniArray());

        assertThrows(IllegalArgumentException.class, () -> mysql.serialize(root));
    }

    @Test
    void rowsAusenteLancaIllegalArgumentException() {
        OmniObject root = new OmniObject();
        root.put("table", new OmniValue("usuarios"));

        assertThrows(IllegalArgumentException.class, () -> mysql.serialize(root));
    }

    @Test
    void raizNaoObjetoLancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> mysql.serialize(new OmniArray()));
    }

    @Test
    void valorAninhadoNumaCelulaLancaIllegalArgumentException() {
        OmniObject root = new OmniObject();
        root.put("table", new OmniValue("usuarios"));
        OmniArray rows = new OmniArray();
        OmniObject linha = new OmniObject();
        linha.put("itens", new OmniArray());
        rows.add(linha);
        root.put("rows", rows);

        assertThrows(IllegalArgumentException.class, () -> mysql.serialize(root));
    }

    @Test
    void valorStringMaliciosoEEscapadoComAspaSimplesDobrada() {
        OmniObject root = new OmniObject();
        root.put("table", new OmniValue("usuarios"));
        OmniArray rows = new OmniArray();
        OmniObject linha = new OmniObject();
        linha.put("nome", new OmniValue("x'); DROP TABLE usuarios; --"));
        rows.add(linha);
        root.put("rows", rows);

        String sql = mysql.serialize(root);

        assertEquals(
                "INSERT INTO `usuarios` (`nome`) VALUES ('x''); DROP TABLE usuarios; --');\n",
                sql);
    }

    @Test
    void nomeDeColunaMaliciosoEEscapadoNoQuotingDoIdentificador() {
        OmniObject root = new OmniObject();
        root.put("table", new OmniValue("usuarios"));
        OmniArray rows = new OmniArray();
        OmniObject linha = new OmniObject();
        linha.put("a`); DROP TABLE usuarios; --", new OmniValue("valor"));
        rows.add(linha);
        root.put("rows", rows);

        String sql = mysql.serialize(root);

        assertEquals(
                "INSERT INTO `usuarios` (`a``); DROP TABLE usuarios; --`) VALUES ('valor');\n",
                sql);
    }
}

package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlDialectTest {

    @Test
    void mysqlUsaCraseEBooleanTrueFalse() {
        assertEquals("`col`", SqlDialect.MYSQL.quoteIdentifier("col"));
        assertEquals("TRUE", SqlDialect.MYSQL.formatBoolean(true));
        assertEquals("FALSE", SqlDialect.MYSQL.formatBoolean(false));
    }

    @Test
    void postgresqlUsaAspaDuplaEBooleanTrueFalse() {
        assertEquals("\"col\"", SqlDialect.POSTGRESQL.quoteIdentifier("col"));
        assertEquals("TRUE", SqlDialect.POSTGRESQL.formatBoolean(true));
    }

    @Test
    void oracleUsaAspaDuplaEBoolean10() {
        assertEquals("\"col\"", SqlDialect.ORACLE.quoteIdentifier("col"));
        assertEquals("1", SqlDialect.ORACLE.formatBoolean(true));
        assertEquals("0", SqlDialect.ORACLE.formatBoolean(false));
    }

    @Test
    void sqlServerUsaColcheteEBoolean10() {
        assertEquals("[col]", SqlDialect.SQLSERVER.quoteIdentifier("col"));
        assertEquals("1", SqlDialect.SQLSERVER.formatBoolean(true));
        assertEquals("0", SqlDialect.SQLSERVER.formatBoolean(false));
    }

    @Test
    void identificadorComCaractereDeQuotingEEscapadoDobrandoOCaractere() {
        assertEquals("`a``b`", SqlDialect.MYSQL.quoteIdentifier("a`b"));
        assertEquals("\"a\"\"b\"", SqlDialect.POSTGRESQL.quoteIdentifier("a\"b"));
        assertEquals("[a]]b]", SqlDialect.SQLSERVER.quoteIdentifier("a]b"));
    }
}

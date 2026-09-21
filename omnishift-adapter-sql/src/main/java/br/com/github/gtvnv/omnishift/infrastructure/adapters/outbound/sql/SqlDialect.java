package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.sql;

/**
 * As únicas diferenças entre dialetos que esta geração de INSERT precisa resolver:
 * quoting de identificador (tabela/coluna) e literal de boolean. Números e NULL são
 * idênticos nos 4; multi-row VALUES é evitado de propósito (Oracle não suporta), então
 * não há diferença de dialeto na forma do próprio INSERT.
 */
enum SqlDialect {

    MYSQL {
        @Override
        String quoteIdentifier(String identifier) {
            return "`" + identifier.replace("`", "``") + "`";
        }

        @Override
        String formatBoolean(boolean value) {
            return value ? "TRUE" : "FALSE";
        }
    },

    POSTGRESQL {
        @Override
        String quoteIdentifier(String identifier) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }

        @Override
        String formatBoolean(boolean value) {
            return value ? "TRUE" : "FALSE";
        }
    },

    ORACLE {
        @Override
        String quoteIdentifier(String identifier) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }

        @Override
        String formatBoolean(boolean value) {
            // Oracle não tem tipo boolean nativo (pré-23c); convenção numérica 1/0.
            return value ? "1" : "0";
        }
    },

    SQLSERVER {
        @Override
        String quoteIdentifier(String identifier) {
            // Colchetes são mais portáveis aqui que aspas duplas, que dependem de
            // QUOTED_IDENTIFIER estar ligado.
            return "[" + identifier.replace("]", "]]") + "]";
        }

        @Override
        String formatBoolean(boolean value) {
            // SQL Server usa BIT (1/0); TRUE/FALSE não são literais válidos nas versões tradicionais.
            return value ? "1" : "0";
        }
    };

    abstract String quoteIdentifier(String identifier);

    abstract String formatBoolean(boolean value);
}

package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.sql;

public final class PostgreSqlInsertSerializer extends SqlInsertSerializer {

    public PostgreSqlInsertSerializer() {
        super(SqlDialect.POSTGRESQL, "SQL_POSTGRESQL");
    }
}

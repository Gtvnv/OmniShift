package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.sql;

public final class SqlServerInsertSerializer extends SqlInsertSerializer {

    public SqlServerInsertSerializer() {
        super(SqlDialect.SQLSERVER, "SQL_SQLSERVER");
    }
}

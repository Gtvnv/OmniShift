package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.sql;

public final class MySqlInsertSerializer extends SqlInsertSerializer {

    public MySqlInsertSerializer() {
        super(SqlDialect.MYSQL, "SQL_MYSQL");
    }
}

package br.com.github.gtvnv.omnishift.infrastructure.adapters.outbound.sql;

public final class OracleInsertSerializer extends SqlInsertSerializer {

    public OracleInsertSerializer() {
        super(SqlDialect.ORACLE, "SQL_ORACLE");
    }
}

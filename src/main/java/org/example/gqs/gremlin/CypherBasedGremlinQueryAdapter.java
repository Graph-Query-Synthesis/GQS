package org.example.gqs.gremlin;

import org.example.gqs.GlobalState;
import org.example.gqs.common.query.ExpectedErrors;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.common.query.Query;
import org.example.gqs.cypher.CypherConnection;

import java.util.List;

public class CypherBasedGremlinQueryAdapter extends Query<CypherConnection> {

    private final String gremlinQuery;

    public CypherBasedGremlinQueryAdapter(String cypherQuery) {
        this.gremlinQuery = CypherGremlinTranslater.translate(cypherQuery);
    }

    private String canonicalizeString(String s) {
        if (s.endsWith(";")) {
            return s;
        } else if (!s.contains("--")) {
            return s + ";";
        } else {
            return s;
        }
    }

    @Override
    public String getLogString() {
        return getQueryString();
    }

    @Override
    public String getQueryString() {
        return gremlinQuery;
    }

    @Override
    public String getUnterminatedQueryString() {
        return canonicalizeString(gremlinQuery);
    }

    @Override
    public boolean couldAffectSchema() {
        return false;
    }

    @Override
    public <G extends GlobalState<?, ?, CypherConnection>> boolean execute(G globalState, String... fills) throws Exception {
        System.out.println(gremlinQuery);
        globalState.getConnection().executeStatement(gremlinQuery);
        return true;
    }

    @Override
    public <G extends GlobalState<?, ?, CypherConnection>> List<GQSResultSet> executeAndGet(G globalState, String... fills) throws Exception {
        return globalState.getConnection().executeStatementAndGet(gremlinQuery);
    }

    @Override
    public ExpectedErrors getExpectedErrors() {
        return null;
    }
}

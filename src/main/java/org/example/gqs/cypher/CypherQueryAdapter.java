package org.example.gqs.cypher;

import org.example.gqs.GlobalState;
import org.example.gqs.MainOptions;
import org.example.gqs.common.query.ExpectedErrors;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.common.query.Query;
import org.example.gqs.memGraph.MemGraphConnection;

import java.util.List;

public class CypherQueryAdapter extends Query<CypherConnection> {

    private final String query;

    public CypherQueryAdapter(String query) {
        this.query = query;
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
        return query;
    }

    @Override
    public String getUnterminatedQueryString() {
        return canonicalizeString(query);
    }

    @Override
    public boolean couldAffectSchema() {
        return false;
    }

    @Override
    public <G extends GlobalState<?, ?, CypherConnection>> boolean execute(G globalState, String... fills) throws Exception {
        System.out.println(query);
        if((query.contains("INDEX") || query.contains("CONSTRAINT")) && MainOptions.mode == "memgraph")
            ((MemGraphConnection)(globalState.getConnection())).nonTxnExecuteStatement(query);
        else
            globalState.getConnection().executeStatement(query);
        return true;
    }



    @Override
    public <G extends GlobalState<?, ?, CypherConnection>> List<GQSResultSet> executeAndGet(G globalState, String... fills) throws Exception {
        return globalState.getConnection().executeStatementAndGet(query);
    }

    @Override
    public <G extends GlobalState<?, ?, CypherConnection>> List<Long> executeAndGetTime(G globalState, String... fills) throws Exception {
        return globalState.getConnection().executeStatementAndGetTime(query);
    }

    @Override
    public ExpectedErrors getExpectedErrors() {
        return null;
    }
}

package org.example.gqs.cypher;

public interface CypherQueryProvider<S> {
    CypherQueryAdapter getQuery(S globalState) throws Exception;
}

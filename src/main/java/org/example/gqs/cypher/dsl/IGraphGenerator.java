package org.example.gqs.cypher.dsl;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherQueryAdapter;

import java.util.List;

public interface IGraphGenerator <G extends CypherGlobalState<?,?>> {
    List<CypherQueryAdapter> createGraph(G globalState) throws Exception;
}

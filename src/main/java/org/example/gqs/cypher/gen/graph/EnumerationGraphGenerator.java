package org.example.gqs.cypher.gen.graph;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.dsl.IGraphGenerator;
import org.example.gqs.cypher.gen.EnumerationGraphManager;
import org.example.gqs.cypher.gen.EnumerationSeq;
import org.example.gqs.cypher.schema.CypherSchema;

import java.util.List;

public class EnumerationGraphGenerator<G extends CypherGlobalState<?,S>, S extends CypherSchema<G,?>> implements IGraphGenerator<G> {

    private final G globalState;
    private EnumerationSeq enumerationSeq;


    private EnumerationGraphManager enumerationGraphManager;


    public EnumerationGraphGenerator(G globalState, EnumerationSeq enumerationSeq){
        this.globalState = globalState;
        enumerationGraphManager = new EnumerationGraphManager(globalState.getSchema(), globalState.getOptions(), enumerationSeq);
    }


    @Override
    public List<CypherQueryAdapter> createGraph(G globalState) throws Exception {
        List<CypherQueryAdapter> queries = enumerationGraphManager.generateCreateGraphQueries();
        return queries;
    }
}


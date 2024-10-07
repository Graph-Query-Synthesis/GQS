package org.example.gqs.cypher.algorithm;

import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.OracleFactory;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherProviderAdapter;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.gen.query.RandomCoverageQueryGenerator;
import org.example.gqs.cypher.gen.graph.RandomGraphGenerator;
import org.example.gqs.cypher.schema.CypherSchema;

import java.util.List;

public class SimpleTestingAlgorithm  <S extends CypherSchema<G,?>, G extends CypherGlobalState<O, S>,
        O extends DBMSSpecificOptions<? extends OracleFactory<G>>, C extends CypherConnection> extends SimpleTestingAlgorithmBase<S,G,O,C>{

    public SimpleTestingAlgorithm(CypherProviderAdapter<G, S, O> provider) {
        super(provider);
    }

    @Override
    public IQueryGenerator<S, G> createQueryGenerator() {
        return new RandomCoverageQueryGenerator<>();
    }

    @Override
    public void generateDatabase(G globalState) throws Exception {

        List<CypherQueryAdapter> queries = new RandomGraphGenerator<G,S>(globalState).createGraph(globalState);
        for(CypherQueryAdapter query : queries){
            globalState.executeStatement(query);
        }
    }
}

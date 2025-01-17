package org.example.gqs.cypher.algorithm;

import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.OracleFactory;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherProviderAdapter;
import org.example.gqs.cypher.schema.CypherSchema;

public abstract class CypherTestingAlgorithm <S extends CypherSchema<G,?>, G extends CypherGlobalState<O, S>,
        O extends DBMSSpecificOptions<? extends OracleFactory<G>>, C extends CypherConnection>{

    protected CypherProviderAdapter<G,S,O> provider;
    public CypherTestingAlgorithm(CypherProviderAdapter<G,S,O> provider){
        this.provider = provider;
    }

    public abstract void generateAndTestDatabase(G globalState) throws Exception;
}

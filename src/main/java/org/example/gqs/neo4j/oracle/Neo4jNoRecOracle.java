package org.example.gqs.neo4j.oracle;

import org.example.gqs.cypher.oracle.NoRecOracle;
import org.example.gqs.neo4j.Neo4jGlobalState;
import org.example.gqs.neo4j.schema.Neo4jSchema;

public class Neo4jNoRecOracle extends NoRecOracle<Neo4jGlobalState, Neo4jSchema> {
    public Neo4jNoRecOracle(Neo4jGlobalState globalState) {
        super(globalState);
    }
}

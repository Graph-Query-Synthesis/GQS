package org.example.gqs.neo4j.gen;

import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.neo4j.Neo4jGlobalState;

public class Neo4jNodeGenerator {

    private final Neo4jGlobalState globalState;
    public Neo4jNodeGenerator(Neo4jGlobalState globalState){
        this.globalState = globalState;
    }

    public static CypherQueryAdapter createNode(Neo4jGlobalState globalState){
        return new Neo4jNodeGenerator(globalState).generateCreate();
    }

    public CypherQueryAdapter generateCreate(){
        return new CypherQueryAdapter("CREATE (p:Person{id: 1})");
    }
}

package org.example.gqs.neo4j;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.neo4j.gen.Neo4jSchemaGenerator;
import org.example.gqs.neo4j.schema.Neo4jSchema;

public class Neo4jGlobalState extends CypherGlobalState<Neo4jOptions, Neo4jSchema> {

    private Neo4jSchema neo4jSchema = null;
    public Neo4jGlobalState(){
        super();
        System.out.println("new global state");
    }

    @Override
    protected Neo4jSchema readSchema() {
        if(neo4jSchema == null){
            neo4jSchema = new Neo4jSchemaGenerator(this).generateSchema();
        }
        return neo4jSchema;
    }
}

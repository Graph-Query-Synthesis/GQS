package org.example.gqs.agensGraph;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.agensGraph.gen.AgensGraphSchemaGenerator;

public class AgensGraphGlobalState extends CypherGlobalState<AgensGraphOptions, AgensGraphSchema> {

    private AgensGraphSchema agensGraphSchema = null;

    public AgensGraphGlobalState(){
        super();
        System.out.println("new global state");
    }

    @Override
    protected AgensGraphSchema readSchema()  {
        if(agensGraphSchema == null){
            agensGraphSchema = new AgensGraphSchemaGenerator(this).generateSchema();
        }
        return agensGraphSchema;
    }
}

package org.example.gqs.memGraph;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.memGraph.gen.MemGraphSchemaGenerator;

public class MemGraphGlobalState extends CypherGlobalState<MemGraphOptions, MemGraphSchema> {

    private MemGraphSchema memGraphSchema = null;

    public MemGraphGlobalState(){
        super();
        System.out.println("new global state");
    }

    @Override
    protected MemGraphSchema readSchema()  {
        if(memGraphSchema == null){
            memGraphSchema = new MemGraphSchemaGenerator(this).generateSchema();
        }
        return memGraphSchema;
    }
}

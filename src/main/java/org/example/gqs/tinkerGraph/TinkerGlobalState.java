package org.example.gqs.tinkerGraph;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.tinkerGraph.gen.TinkerSchemaGenerator;
import org.example.gqs.tinkerGraph.schema.TinkerSchema;

public class TinkerGlobalState extends CypherGlobalState<TinkerOptions, org.example.gqs.tinkerGraph.schema.TinkerSchema> {

    private TinkerSchema TinkerSchema = null;

    public TinkerGlobalState(){
        super();
        System.out.println("new global state");
    }

    @Override
    protected TinkerSchema readSchema()  {
        if(TinkerSchema == null){
            TinkerSchema = new TinkerSchemaGenerator(this).generateSchema();
        }
        return TinkerSchema;
    }
}

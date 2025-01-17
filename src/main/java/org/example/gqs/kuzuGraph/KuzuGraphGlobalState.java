package org.example.gqs.kuzuGraph;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.kuzuGraph.gen.KuzuGraphSchemaGenerator;

public class KuzuGraphGlobalState extends CypherGlobalState<KuzuGraphOptions, KuzuGraphSchema> {

    private KuzuGraphSchema kuzuGraphSchema = null;

    public KuzuGraphGlobalState(){
        super();
        System.out.println("new global state");
    }

    @Override
    protected KuzuGraphSchema readSchema()  {
        if(kuzuGraphSchema == null){
            kuzuGraphSchema = new KuzuGraphSchemaGenerator(this).generateSchema();
        }
        return kuzuGraphSchema;
    }
}
